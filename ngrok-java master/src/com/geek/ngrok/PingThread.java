package com.geek.ngrok;

import java.io.IOException;

import javax.net.ssl.SSLSocket;

public class PingThread extends Thread {
	SSLSocket sock;
	NgrokClient ngrokcli;
	public PingThread(NgrokClient ngrokcli,SSLSocket s) {
		super();
		this.ngrokcli=ngrokcli;
		this.sock = s;
	}

	public void run() {
		while (ngrokcli.trfalg) {
			try {
				MsgSend.SendPing(sock.getOutputStream());
				Log.print("Ping ....");
			} catch (IOException e) {
				ngrokcli.trfalg=false;
			}
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}