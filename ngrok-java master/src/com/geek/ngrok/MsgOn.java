package com.geek.ngrok;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

import org.json.JSONException;
import org.json.JSONObject;

public class MsgOn {
	boolean isrecv = true;

	static String ClientId = "";
	public String localhost = "127.0.0.1";
	public int localport = 80;
	public String protocol = "http";

	public MsgOn(String localhost, int localport, String protocol) {
		this.localhost = localhost;
		this.localport = localport;
		this.protocol = protocol;
	}

	public MsgOn() {
		// TODO Auto-generated constructor stub
	}

	public void jsonunpack(String str, SSLSocket s) {
		JSONObject json;
		try {
			Log.print("recvstr:" + str);
			json = new JSONObject(str);

			String type = json.getString("Type");
			// Auth back
			if (type.equals("AuthResp")) {
				JSONObject Payload = json.getJSONObject("Payload");
				String Error = Payload.getString("Error");
				if (Error.endsWith("")) {
					Log.print("AuthResp .....OK....");
					AuthResp(json, s);
				} else {
					Log.print("AuthResp .....error....");
				}
			}

			if (type.equals("ReqProxy")) {

				ReqProxy(json);

			}

			// ping ack
			if (type.equals("Ping")) {

				Ping(json, s);
			}

			// NewTunnel
			if (type.equals("NewTunnel")) {
				JSONObject Payload = json.getJSONObject("Payload");
				String Error = Payload.getString("Error");
				if (Error.endsWith("")) {
					Log.print("NewTunnel .....OK....");
					NewTunnel(json);
				} else {
					Log.print("NewTunnel .....error....");
				}
			}

			// StartProxy
			if (type.equals("StartProxy")) {
				StartProxy(json, s);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void AuthResp(JSONObject json, SSLSocket s) {

		// 请求映射
		try {
			JSONObject Payload = json.getJSONObject("Payload");
			ClientId = Payload.getString("ClientId");
			MsgSend.SendReqTunnel(s.getOutputStream(), protocol);
			// start ping thread
			Thread pingtr = new PingThread(s);
			pingtr.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void ReqProxy(JSONObject json) {

		Thread xx = new ProxyThread(ClientId);
		xx.start();
	}

	public void Ping(JSONObject json, SSLSocket s) {

		try {
			MsgSend.SendPong(s.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void NewTunnel(JSONObject json) {

		try {
			JSONObject Payload = json.getJSONObject("Payload");
			System.out.println("Url:" + Payload.getString("Url")
					+ "  Protocol:" + Payload.getString("Protocol"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void StartProxy(JSONObject json, SSLSocket s) {
		try {
			// 不再接收命令,
			this.isrecv = false;

			Socket locals = new Socket(localhost, localport);

			SOCKSToThread thread1 = new SOCKSToThread(s.getInputStream(),
					locals.getOutputStream());

			// 读取本地数据给远程
			SOCKSToThread thread2 = new SOCKSToThread(locals.getInputStream(),
					s.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void unpack(SSLSocket s) {
		byte[] packbuff = new byte[2048];
		int packbufflen = 0;
		byte[] buffer = new byte[1024];
		try {
			InputStream zx = s.getInputStream();
			while (isrecv) {
				int len = zx.read(buffer);
				if (len == -1) {
					break;
				}
				if (len == 0) {
					continue;
				}
				BytesUtil.myaddBytes(packbuff, packbufflen, buffer, len);
				packbufflen = packbufflen + len;

				if (packbufflen > 8) {

					// 发送时间
					int packlen = (int) BytesUtil
							.bytes2long(BytesUtil.leTobe(
									BytesUtil.cutOutByte(packbuff, 0, 8), 8), 0);
					// 加上头8个字节
					packlen = packlen + 8;
					if (packbufflen == packlen) {
						jsonunpack(
								new String(BytesUtil.cutOutByte(packbuff, 8,
										packlen - 8)), s);
						packbufflen = 0;
					}

					else if (packbufflen > packlen) {
						jsonunpack(
								new String(BytesUtil.cutOutByte(packbuff, 8,
										packlen - 8)), s);
						packbufflen = packbufflen - packlen;
						BytesUtil.myaddBytes(packbuff, 0, BytesUtil.cutOutByte(
								packbuff, packlen, packbufflen), packbufflen);
					}

				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
