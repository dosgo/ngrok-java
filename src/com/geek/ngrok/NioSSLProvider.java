package com.geek.ngrok;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLEngine;
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
   public void onOutput(SocketChannel key,ByteBuffer encrypted)
   {
      try
      {
         ((WritableByteChannel)key).write(encrypted);
      }
      catch (IOException exc)
      {
         throw new IllegalStateException(exc);
      }
   }
   public boolean processInput(SocketChannel key)
   {
	  buffer.clear();
      int bytes;
      try
      {
         bytes = ((ReadableByteChannel) key).read(buffer);
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
      this.notify(key,copy);
      return true;
   }
   

   public boolean initEngine(SocketChannel key,boolean clientMode){
	   
	   TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

	       public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	           return new java.security.cert.X509Certificate[] {};
	       }

	       public void checkClientTrusted(X509Certificate[] chain, String authType) {

	       }

	       public void checkServerTrusted(X509Certificate[] chain, String authType) {

	       }
	   } };

	   
	   SocketChannel   socketChannel = (SocketChannel) key;  //
       if (socketChannel.isConnectionPending()) {
   			try {
  				 socketChannel.finishConnect();
		   		 SSLContext sc = SSLContext.getInstance("TLS");
		   	     sc.init(null, trustAllCerts, new java.security.SecureRandom());
		   	     SSLEngine engine = sc.createSSLEngine();
   				 if(clientMode){
   				      engine.setUseClientMode(true);
   				 }
   				 engine.beginHandshake();
   				 engines.put(key, engine);
   				 this.exec(key);//≥ı ºªØ
   				 return true;
   			} catch (Exception e) {
   				// TODO Auto-generated catch block
   				e.printStackTrace();
   			}
       }
	   return false;  
   }




	public void freeEngine(SocketChannel channel ){
		 engines.remove(channel);
	}
}