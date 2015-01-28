package com.geek.ngrok;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class NgrokClient {
	public void start(String localhost, int loclaport, String protocol,String authtoken,
			boolean debug) {

		Log.isdebug = debug;
		String serveraddr="ngrokd.ngrok.com";
		int serverport=443;

		SocketFactory sf = SSLSocketFactory.getDefault();
		try {
			SSLSocket s = (SSLSocket) sf.createSocket(serveraddr, serverport);
			s.setEnabledProtocols(new String[] { "SSLv3" });
			// 发送登录认证
			MsgSend.SendAuth("",authtoken,s.getOutputStream());
			// 监听
			MsgOn msg = new MsgOn(localhost, loclaport, protocol);
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
