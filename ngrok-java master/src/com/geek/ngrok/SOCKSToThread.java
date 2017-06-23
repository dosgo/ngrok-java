package com.geek.ngrok;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class SOCKSToThread extends Thread {
	private DataInputStream in; // 读数据
	private DataOutputStream out; // 写数据

	public SOCKSToThread(InputStream _in, OutputStream _out) {
		in = new DataInputStream(_in);
		out = new DataOutputStream(_out);
		start();
	}

	public void run() {
		// 线程运行函数,循环读取返回数据,并发送给相关客户端
		int readbytes = 0;
		byte buf[] = new byte[1000];
		while (true) { // 循环
			try {
				if (readbytes == -1)
					break; // 无数据则退出循环
				readbytes = in.read(buf, 0, 1000);
				if (readbytes > 0) {
					out.write(buf, 0, readbytes);
					out.flush();
				}
			} catch (Exception e) {
				break;
			} // 异常则退出循环
		}
		//如果远程连接关闭。。也关闭本地的连接。。避免无限超时现象
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}