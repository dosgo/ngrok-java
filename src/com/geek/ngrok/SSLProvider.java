package com.geek.ngrok;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

public abstract class SSLProvider
{
 //  final SSLEngine engine;
   final Executor ioWorker, taskWorkers;
   final ByteBuffer clientWrap, clientUnwrap;
   final ByteBuffer serverWrap, serverUnwrap;
   public HashMap<SocketChannel, SSLEngine> engines = new HashMap<SocketChannel, SSLEngine>();  

   public SSLProvider(int capacity, Executor ioWorker, Executor taskWorkers)
   {
      this.clientWrap = ByteBuffer.allocate(capacity);
      this.serverWrap = ByteBuffer.allocate(capacity);
      this.clientUnwrap = ByteBuffer.allocate(capacity);
      this.serverUnwrap = ByteBuffer.allocate(capacity);
      this.clientUnwrap.limit(0);
      this.ioWorker = ioWorker;
      this.taskWorkers = taskWorkers;
   }

   public abstract void onInput(SocketChannel key,ByteBuffer decrypted);
   public abstract void onOutput(SocketChannel key,ByteBuffer encrypted);
   public abstract void onFailure(SocketChannel key,Exception ex);
   public abstract void onSuccess(SocketChannel key);
   public abstract void onClosed(SocketChannel key);

   public void sendAsync(final SocketChannel key,final ByteBuffer data)
   {
      this.ioWorker.execute(new Runnable()
      {
         @Override
         public void run()
         {
            clientWrap.put(data);
            SSLProvider.this.exec(key);
         }
      });
   }

   public void notify(final SocketChannel key,final ByteBuffer data)
   {
      this.ioWorker.execute(new Runnable()
      {
         @Override
         public void run()
         {
            clientUnwrap.put(data);
            SSLProvider.this.exec(key);
         }
      });
   }
   
   public void exec(SocketChannel key)
   {
      // executes non-blocking tasks on the IO-Worker
      while (this.isHandShaking(key))
      {
         continue;
      }
   }
   
   public  SSLEngine getEngine(SocketChannel key) throws  SSLException {
	   SSLEngine  engine=  engines.get(key);
	   if(engine==null){
		   new SSLException("not find engine");
		   return null;
	   }
	   return engine;   
   }

   private synchronized boolean isHandShaking(final SocketChannel key)
   {
	SSLEngine engine;
	try {
		engine = this.getEngine(key);
		switch (engine.getHandshakeStatus())
	      {
	         case NOT_HANDSHAKING:
	            boolean occupied = false;
	            {
	               if (clientWrap.position() > 0)
	            	   occupied |= this.wrap(key);
	               if (clientUnwrap.position() > 0)
	            	   occupied |= this.unwrap(key);
	            }
	            return occupied;

	         case NEED_WRAP:
	            if (!this.wrap(key))
	               return false;
	            break;

	         case NEED_UNWRAP:
	            if (!this.unwrap(key))
	               return false;
	            break;

	         case NEED_TASK:
	            final Runnable sslTask = engine.getDelegatedTask();
	            Runnable wrappedTask = new Runnable()
	            {
	               @Override
	               public void run()
	               {
	            	  
	            	   if(sslTask!=null){ 
	            		   sslTask.run();
	            		}
	                  ioWorker.execute(new Runnable(){
	                	  @Override
	                      public void run()
	                      {                		  	
	                		  SSLProvider.this.exec(key);
	                      }
	                  });
	               }
	            };
	            taskWorkers.execute(wrappedTask);
	            return false;

	         case FINISHED:
	            throw new IllegalStateException("FINISHED");
	      }

	      return true;
		} catch (SSLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return false;
   }

   private boolean wrap(SocketChannel key)
   {
	  SSLEngine engine;
	try {
		engine = this.getEngine(key);
	} catch (SSLException e) {
		this.onFailure(key,e);
        return false;
	}
      SSLEngineResult wrapResult;

      try
      {
         clientWrap.flip();
         wrapResult = engine.wrap(clientWrap, serverWrap);
         clientWrap.compact();
      }
      catch (SSLException exc)
      {
         this.onFailure(key,exc);
         return false;
      }

      switch (wrapResult.getStatus())
      {
         case OK:
            if (serverWrap.position() > 0)
            {
               serverWrap.flip();
               this.onOutput(key,serverWrap);
               serverWrap.compact();
            }
            break;

         case BUFFER_UNDERFLOW:
            // try again later
            break;

         case BUFFER_OVERFLOW:
            throw new IllegalStateException("failed to wrap");

         case CLOSED:
            this.onClosed(key);
            return false;
      }

      return true;
   }

   private boolean unwrap(SocketChannel key)
   {
      SSLEngineResult unwrapResult;
      SSLEngine engine;
	try {
		engine = this.getEngine(key);
	} catch (SSLException e) {
		   this.onFailure(key,e);
	         return false;
	}
     
      try
      {
         clientUnwrap.flip();
         unwrapResult = engine.unwrap(clientUnwrap, serverUnwrap);
         clientUnwrap.compact();
      }
      catch (SSLException ex)
      {
         this.onFailure(key,ex);
         return false;
      }

      switch (unwrapResult.getStatus())
      {
         case OK:
            if (serverUnwrap.position() > 0)
            {
               serverUnwrap.flip();
               this.onInput(key,serverUnwrap);
               serverUnwrap.compact();
            }
            break;

         case CLOSED:
            this.onClosed(key);
            return false;

         case BUFFER_OVERFLOW:
            throw new IllegalStateException("failed to unwrap");

         case BUFFER_UNDERFLOW:
            return false;
      }

      if (unwrapResult.getHandshakeStatus() == HandshakeStatus.FINISHED)
      {
            this.onSuccess(key);
            return false;
      }

      return true;
   }
}
