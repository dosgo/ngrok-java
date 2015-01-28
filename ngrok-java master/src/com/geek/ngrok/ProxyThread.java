package com.geek.ngrok;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class ProxyThread extends Thread {
	String ClientId = "";
	String serveraddr="ngrokd.ngrok.com";
	int serverport=443;

	public ProxyThread(String ClientIdp) {
		super();
		this.ClientId = ClientIdp;
	}
	

	public void run() {
		SocketFactory sf = SSLSocketFactory.getDefault();
		SSLSocket s;
		try {
			s = (SSLSocket) sf.createSocket(serveraddr, serverport);
			s.setEnabledProtocols(new String[] { "SSLv3" });
			MsgOn msg = new MsgOn();
			// Æ´°ü·¢ËÍ
			MsgSend.SendRegProxy(ClientId, s.getOutputStream());
			msg.unpack(s);

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
