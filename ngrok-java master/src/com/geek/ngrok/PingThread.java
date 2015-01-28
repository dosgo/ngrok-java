package com.geek.ngrok;

import java.io.IOException;

import javax.net.ssl.SSLSocket;

public class PingThread extends Thread {
	SSLSocket sock;

	public PingThread(SSLSocket s) {
		super();
		this.sock = s;
	}

	public void run() {
		while (true) {
			try {
				MsgSend.SendPing(sock.getOutputStream());
				Log.print("Ping ....");
			} catch (IOException e) {
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