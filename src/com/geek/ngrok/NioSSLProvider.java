package com.geek.ngrok;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.cert.X509Certificate;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;

public abstract class NioSSLProvider extends SSLProvider
{
   private final ByteBuffer buffer = ByteBuffer.allocate(32 * 1024);
  // private final SelectionKey key;

   public NioSSLProvider(int bufferSize, Executor ioWorker, Executor taskWorkers)
   {
      super(bufferSize, ioWorker, taskWorkers);
   }
   
   @Override
   public void onOutput(SelectionKey key,ByteBuffer encrypted)
   {
      try
      {
         ((SocketChannel) key.channel()).write(encrypted);
      }
      catch (IOException exc)
      {
        // throw new IllegalStateException(exc);
      }
   }
   public boolean processInput(SslInfo sinfo)
   {
	  buffer.clear();
      int bytes;
      try
      {
         bytes = ((ReadableByteChannel) sinfo.key.channel()).read(buffer);
      }
      catch (IOException ex)
      {
         bytes = -1;
      }
      if (bytes == -1) {
         return false;
      }
      buffer.flip();
      ByteBuffer copy = ByteBuffer.allocate(bytes);
      copy.put(buffer);
      copy.flip();
      this.notify(sinfo,copy);
      return true;
   }
   

   public boolean bindEngine(SslInfo sinfo){
	   SocketChannel   socketChannel = (SocketChannel) sinfo.key.channel();  //
       if (socketChannel.isConnectionPending()) {
   			try {
  				 socketChannel.finishConnect();
   				 this.exec(sinfo);//≥ı ºªØ
   				 return true;
   			} catch (Exception e) {
   				// TODO Auto-generated catch block
   				e.printStackTrace();
   			}
       }
	   return false;  
   }





}