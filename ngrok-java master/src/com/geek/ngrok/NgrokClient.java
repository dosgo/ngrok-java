package com.geek.ngrok;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NgrokClient {
	
	String serveraddr="tunnel.qydev.com";
	int serverport=4443;
	SSLSocket s;
	SocketFactory sf=null;
	public String ClientId = "";
	public String localhost = "127.0.0.1";
	public int localport = 80;
	public String protocol = "http";
	public boolean  trfalg=true;
	public long lasttime=0;
	public String authtoken="";
	public List<HashMap<String, String>> tunnels = new ArrayList<HashMap<String, String>>();  
	

	
	public HashMap<String,HashMap<String, String>> tunnelinfos = new HashMap<String,HashMap<String, String>>();  	
	
	
	 public NgrokClient(String serveraddr,int serverport,String authtoken, Boolean debug){
		 this.serveraddr=serveraddr;
		 this.serverport=serverport;
		 Log.isdebug = debug;
	 }
	
	

	public NgrokClient(){
	}
	
	public void start(){
		s=connectSSL();
		// 发送登录认证
		try {
			MsgSend.SendAuth("",authtoken,s.getOutputStream());
			//启动线程监听
			new CmdThread(this,s).start();
		} catch (IOException e) {
			
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
	

	/*
	 * 
	 */
	public  SSLSocket  connectSSL(){
		SSLSocket s=null;
		if(sf==null){
			try {
				sf=trustAllSocketFactory();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		try {
			s = (SSLSocket) sf.createSocket(this.serveraddr, this.serverport);
		    s.startHandshake();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return s;
	}
	
	
	/*忽略证书*/
	public static SSLSocketFactory trustAllSocketFactory() throws Exception{
	    TrustManager[] trustAllCerts = new TrustManager[]{
	            new X509TrustManager() {
	                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                    return null;
	                }

	                public void checkClientTrusted(X509Certificate[] certs, String authType) {
	                }

	                public void checkServerTrusted(X509Certificate[] certs, String authType) {
	                }

	            }
	    };
	    SSLContext sslCxt = SSLContext.getInstance("TLSv1.2");
	    sslCxt.init(null, trustAllCerts, null);
	    return sslCxt.getSocketFactory();
	}

}
