package com.geek.ngrok;

import java.io.IOException;

import javax.net.ssl.SSLSocket;

public class CmdThread extends Thread {
	SSLSocket sock;
	NgrokClient ngrokcli;
	public CmdThread(NgrokClient ngrokcli,SSLSocket s) {
		super();
		this.sock = s;
		this.ngrokcli=ngrokcli;
	}	

	public void run() {
		while (ngrokcli.trfalg) {
			// ¼àÌý
			MsgOn msg = new MsgOn(this.ngrokcli);
			msg.unpack(sock);
		}

	}
}