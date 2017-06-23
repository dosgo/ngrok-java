package com.geek.ngrok;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.UUID;

import javax.net.ssl.SSLSocket;

import org.json.JSONException;
import org.json.JSONObject;

public class MsgOn {
	boolean isrecv = true;
	NgrokClient ngrokcli;



	public MsgOn(NgrokClient ngrokcli) {
		this.ngrokcli=ngrokcli;
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
			if (type.equals("Pong")) {

				Pong();
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
			ngrokcli.ClientId = Payload.getString("ClientId");
			
			// 
	        for(int i = 0;i < ngrokcli.tunnels.size(); i ++){
	        	HashMap<String, String> tunelInfo=ngrokcli.tunnels.get(i);
	        	String ReqId =  UUID.randomUUID().toString().toLowerCase().replace("-", "").substring(0, 8);
	        	MsgSend.SendReqTunnel(s.getOutputStream(),ReqId, tunelInfo.get("Protocol"),tunelInfo.get("Hostname"),tunelInfo.get("Subdomain"),tunelInfo.get("RemotePort"),tunelInfo.get("HttpAuth"));
	        	HashMap<String, String> tunelInfo1 = new HashMap<String, String>();
	        	tunelInfo1.put("localhost", tunelInfo.get("localhost"));
	        	tunelInfo1.put("localport", tunelInfo.get("localport"));
	        	ngrokcli.tunnelinfos.put(ReqId, tunelInfo1);
	        	
	        }
			
			
			// start ping thread
			Thread pingtr = new PingThread(ngrokcli,s);
			pingtr.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void ReqProxy(JSONObject json) {

		Thread xx = new ProxyThread(ngrokcli,ngrokcli.ClientId);
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
	
	public void Pong() {
		ngrokcli.lasttime=System.currentTimeMillis() / 1000;
	}

	public  void NewTunnel(JSONObject json) {

		try {
			JSONObject Payload = json.getJSONObject("Payload");
			String ReqId=Payload.getString("ReqId");
			//添加到通道队列
			
			ngrokcli.tunnelinfos.put(Payload.getString("Url"), ngrokcli.tunnelinfos.get(ReqId));
			ngrokcli.tunnelinfos.remove(ReqId);//remove 
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
			try{
				JSONObject Payload = json.getJSONObject("Payload");
				String Url=Payload.getString("Url");
				HashMap<String, String> tunelInfo=ngrokcli.tunnelinfos.get(Url);
				

				Socket locals = new Socket(tunelInfo.get("localhost"),Integer.parseInt(tunelInfo.get("localport")));

				SOCKSToThread thread1 = new SOCKSToThread(s.getInputStream(),
						locals.getOutputStream());

				// 读取本地数据给远程
				SOCKSToThread thread2 = new SOCKSToThread(locals.getInputStream(),
						s.getOutputStream());
			}catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
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
			//异常关闭连接
			isrecv=false;
			//e.printStackTrace();
			return ;
		}
	}
}
