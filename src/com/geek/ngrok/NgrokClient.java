package com.geek.ngrok;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;






class SockInfo{
	int type;//1远程连接,2本地连接,3代理连接
	SocketChannel tokey;//对方的连接
	int forward;//转发标记,代理连接专用，
	byte[] buf;
	int buflen;
}

public class NgrokClient {
	
	String serveraddr="tunnel.qydev.com";
	int serverport=4443;
	public String ClientId = "";
	public String localhost = "127.0.0.1";
	public int localport = 80;
	public String protocol = "http";
	public long lasttime=0; 
	public long pingtime=0;
	public String authtoken="";
	public List<HashMap<String, String>> tunnels = new ArrayList<HashMap<String, String>>();  
	public static NioSSLProvider ssl;
	Selector selector;
	public Map<SocketChannel, SockInfo> Socks =  Collections.synchronizedMap(new HashMap<SocketChannel, SockInfo>());  
	//public HashMap<SocketChannel, SockInfo> Socks =  new HashMap<SocketChannel, SockInfo>();   
	public HashMap<String,HashMap<String, String>> tunnelinfos = new HashMap<String,HashMap<String, String>>();  	
	public SocketChannel mainkey;
	
	MsgOn msg = new MsgOn(NgrokClient.this);
	MsgSend msgSend = new MsgSend(NgrokClient.this);
	
	 public NgrokClient(String serveraddr,int serverport,String authtoken, Boolean debug){
		 this.serveraddr=serveraddr;
		 this.serverport=serverport;
		 Log.isdebug = debug;
	 }
	 public NgrokClient(){
		
	 }

	public void start(){
		
	   
		
		
	      try {
	    	  selector = Selector.open();
	    	  //连接到服务器
	    	  mainkey=connect(serveraddr,serverport);
	    	  setSock(mainkey,1,0,null);//添加到监听
		      // create the worker threads
		      final Executor ioWorker = Executors.newSingleThreadExecutor();
		      final Executor taskWorkers = Executors.newFixedThreadPool(2);
		      
		     
		      final int ioBufferSize = 32 * 1024;
		      ssl = new NioSSLProvider(ioBufferSize, ioWorker, taskWorkers)
		      {
		         @Override
		         public void onFailure(SocketChannel key,Exception ex)
		         {
		            System.out.println("handshake failure");
		            ex.printStackTrace();
		         }

		         @Override
		         public void onSuccess(SocketChannel key)
		         {
		        	SockInfo sockinfo= Socks.get(key);
		        	//控制连接，直接登录认证
		        	if(sockinfo.type==1){
		        		//ssl认证成功.ngrokr认证
		        		msgSend.SendAuth("",authtoken,key);
		        	}
		        	//代理连接
		        	if(sockinfo.type==3){
		        		//非控制连接，注册端口
		        		msgSend.SendRegProxy(ClientId,key);
		        	}
		         }

		         @Override
		         public void onInput(SocketChannel key,ByteBuffer decrypted)
		         {
		        	SockInfo sockinfo= Socks.get(key);
		        	//代理连接，转发模式
		        	if(sockinfo.type==3&&sockinfo.forward==1){
		        		RemoteToLocal(decrypted,sockinfo.tokey);
		        	}else{
		        		// 监听
		        		msg.unpack(key,decrypted);
		        	}
		         }

		         @Override
		         public void onClosed(SocketChannel key)
		         {
		        	ssl.freeEngine(key);
		        	freeSock(key);//回收内存
		            System.out.println("ssl session closed");
		         }
				
		      };

		      // NIO selector
		      while (true)
		      {

		    	  //定时心跳
		    	  if(lasttime>0){
		    		  if(pingtime==0||pingtime+25<System.currentTimeMillis() / 1000){
		    			  msgSend.SendPing(mainkey);
		    			  pingtime=(System.currentTimeMillis() / 1000);
		    		  }
		    	  }
		    	  try {
		    		  selector.select(10);
		    		  Iterator<SelectionKey>keySet = selector.selectedKeys().iterator();
		    		  while (keySet.hasNext())
		    	      {
		    	        	SelectionKey sKey= keySet.next();
		    	        	keySet.remove();
	    	        		SockInfo sockinfo= Socks.get(sKey.channel());
		    	        	if(sKey.isConnectable()){
		    	        		if(sockinfo.type==1||sockinfo.type==3){

				        			if(ssl.initEngine((SocketChannel)sKey.channel(), true)){
				        				sKey.channel().register(selector, SelectionKey.OP_READ);  
				        			}
		    	        		}
		    	        		if(sockinfo.type==2){
		    	        			sKey.channel().register(selector, SelectionKey.OP_READ);  
		    	        		}
		    	        	}
		    	        	if(sKey.isReadable()){
		    	        		if(sockinfo.type==1||sockinfo.type==3){
		    	        			ssl.processInput((SocketChannel)sKey.channel());
		    	        		}
		    	        		if(sockinfo.type==2){
		    	        			LocalToRemote(sKey,sockinfo.tokey);
		    	        		}
		    	        	}
		    	      }
					} catch (Exception e) {
						e.printStackTrace();
						break;
					}
		      }

		      
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	  
	
	}
	
	
	 public void addTun(String localhost, int localport,String Protocol,String Hostname,String Subdomain,int RemotePort,String HttpAuth){
			
			HashMap<String, String> tunelInfo = new HashMap<String, String>();
			tunelInfo.put("localhost", localhost);
			tunelInfo.put("localport", localport+"");
			tunelInfo.put("Protocol", Protocol);
			tunelInfo.put("Hostname", Hostname);
			tunelInfo.put("Subdomain", Subdomain);
			tunelInfo.put("HttpAuth", HttpAuth);
			tunelInfo.put("RemotePort", RemotePort+"");
			tunnels.add(tunelInfo);
			
	 }
	
	 /*本地的转发远程*/
	 public void LocalToRemote(SelectionKey key ,SocketChannel remoteKey){
			SocketChannel clientChannel = (SocketChannel) key.channel();
			ByteBuffer buf = ByteBuffer.allocate(4096);
				try {
					int len = clientChannel.read(buf);
					buf.flip();
					if(len>0){
					
						ssl.sendAsync(remoteKey,buf);
					}
					//关闭连接
					if(len==-1){
						clientChannel.close();
						remoteKey.close();//关闭远程
						freeSock(clientChannel);//回收内存
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			 
	 }
	 /*远程数据转发到本地*/
	 public void RemoteToLocal(ByteBuffer decrypted ,SocketChannel remoteKey){
			byte[] buffer = new byte[decrypted.remaining()];
	    	decrypted.get(buffer);
	    	ByteBuffer buf = ByteBuffer.allocate(4096);
	    	buf.clear();
	    	buf.put(buffer);
	    	buf.flip();
	    	try {
				remoteKey.finishConnect();
				while(buf.hasRemaining()&&remoteKey.isConnected()) {
					remoteKey.write(buf);
		    	}	  
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	    	  	
	 }
	 
	 public SocketChannel  connect(String serveraddr,int serverport){
	    SocketChannel channel;
		try {
			channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.connect(new InetSocketAddress(serveraddr, serverport));
			return channel;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	   
	 }

	 
	 public void setSock(SocketChannel channel,int type,int forward,SocketChannel tokey){
		 SockInfo sockinfo= Socks.get(channel);
		 if(sockinfo==null){
			sockinfo=new SockInfo();
		 }
		 sockinfo.type=type;
		 sockinfo.tokey=tokey;
		 sockinfo.forward=forward;
		 Socks.put(channel,sockinfo);
		 
		 try {
			 //如果select没有设置超时，这里会阻塞
			channel.register(selector,  SelectionKey.OP_CONNECT);
		} catch (ClosedChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 //回收内存
	 public void freeSock(SocketChannel channel){
		 Socks.remove(channel);
	 }
	 
	 
}
