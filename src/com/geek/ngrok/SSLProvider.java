package com.geek.ngrok;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;



class SslInfo{
	SSLEngine engine;
	SelectionKey tokey;
	public SelectionKey key;
}

public abstract class SSLProvider
{
   final Executor ioWorker, taskWorkers;
   final ByteBuffer clientWrap, clientUnwrap;
   final ByteBuffer serverWrap, serverUnwrap;
   
   

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
   
   public void clearBuf(){
	   this.clientWrap.clear();
	   this.serverWrap.clear();
	   this.clientUnwrap.clear();
	   this.serverUnwrap.clear();
	   this.clientUnwrap.limit(0);
   }

   public abstract void onInput(SelectionKey key,ByteBuffer decrypted);
   public abstract void onOutput(SelectionKey key,ByteBuffer encrypted);
   public abstract void onFailure(SelectionKey key,Exception ex);
   public abstract void onSuccess(SelectionKey key);
   public abstract void onClosed(SelectionKey key);

   public void sendAsync(final SslInfo sinfo,final ByteBuffer data)
   {
      this.ioWorker.execute(new Runnable()
      {
         @Override
         public void run()
         {
            clientWrap.put(data);
            SSLProvider.this.exec(sinfo);
         }
      });
   }

   public void notify(final SslInfo sinfo ,final ByteBuffer data)
   {
      this.ioWorker.execute(new Runnable()
      {
         @Override
         public void run()
         {
        	 
            clientUnwrap.put(data);
            SSLProvider.this.exec(sinfo);
         }
      });
   }
   
   public void exec(SslInfo sinfo)
   {
      // executes non-blocking tasks on the IO-Worker
      while (this.isHandShaking(sinfo))
      {
         continue;
      }
   }
   


   private synchronized boolean isHandShaking(final SslInfo sinfo)
   {
	
	   if(sinfo.engine==null){
		   return false;
	   }
	   
		switch (sinfo.engine.getHandshakeStatus())
	      {
	         case NOT_HANDSHAKING:
	            boolean occupied = false;
	            {
	               if (clientWrap.position() > 0)
	            	   occupied |= this.wrap(sinfo);
	               if (clientUnwrap.position() > 0)
	            	   occupied |= this.unwrap(sinfo);
	            }
	            return occupied;

	         case NEED_WRAP:
	            if (!this.wrap(sinfo))
	               return false;
	            break;

	         case NEED_UNWRAP:
	            if (!this.unwrap(sinfo))
	               return false;
	            break;

	         case NEED_TASK:
	            final Runnable sslTask = sinfo.engine.getDelegatedTask();
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
	                		  SSLProvider.this.exec(sinfo);
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
   }

   private boolean wrap(SslInfo sinfo)
   {
	
	   if(sinfo.engine==null){
		   return false;
	   }
      SSLEngineResult wrapResult;

      try
      {
         clientWrap.flip();
         wrapResult = sinfo.engine.wrap(clientWrap, serverWrap);
         clientWrap.compact();
      }
      catch (SSLException exc)
      {
    	 //需要清空否则会异常
    	 clientWrap.clear();
         this.onFailure(sinfo.key,exc);
         return false;
      }

      switch (wrapResult.getStatus())
      {
         case OK:
            if (serverWrap.position() > 0)
            {
               serverWrap.flip();
               this.onOutput(sinfo.key,serverWrap);
               serverWrap.compact();
            }
            break;

         case BUFFER_UNDERFLOW:
            // try again later
            break;

         case BUFFER_OVERFLOW:
            throw new IllegalStateException("failed to wrap");

         case CLOSED:
            this.onClosed(sinfo.key);
            return false;
      }

      return true;
   }

   private boolean unwrap(SslInfo sinfo)
   {
	  if(sinfo.engine==null){
		   return false;
	   }
      SSLEngineResult unwrapResult;
      try
      {
         clientUnwrap.flip();
         unwrapResult = sinfo.engine.unwrap(clientUnwrap, serverUnwrap);
         clientUnwrap.compact();
      }
      catch (SSLException ex)
      {
    	 //需要清空否则会异常
    	 clientUnwrap.clear();
         this.onFailure(sinfo.key,ex);
         return false;
      }

      switch (unwrapResult.getStatus())
      {
         case OK:
            if (serverUnwrap.position() > 0)
            {
               serverUnwrap.flip();
               this.onInput(sinfo.key,serverUnwrap);
               serverUnwrap.compact();
            }
            break;

         case CLOSED:
            this.onClosed(sinfo.key);
            return false;

         case BUFFER_OVERFLOW:
            throw new IllegalStateException("failed to unwrap");

         case BUFFER_UNDERFLOW:
            return false;
      }

      if (unwrapResult.getHandshakeStatus() == HandshakeStatus.FINISHED)
      {
            this.onSuccess(sinfo.key);
            return false;
      }

      return true;
   }
}
