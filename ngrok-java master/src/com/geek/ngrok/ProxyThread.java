package com.geek.ngrok;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class ProxyThread extends Thread {
	String ClientId = "";
	NgrokClient ngrokcli;
	public ProxyThread(NgrokClient ngrokcli,String ClientIdp) {
		super();
		this.ngrokcli=ngrokcli;
		this.ClientId = ClientIdp;
	}
	

	public void run() {
			SSLSocket s=ngrokcli.connectSSL();
			//
			try {
				MsgSend.SendRegProxy(ClientId, s.getOutputStream());
				MsgOn msg = new MsgOn(ngrokcli);
				msg.unpack(s);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}
}
