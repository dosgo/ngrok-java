package com.geek.ngrok;


import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.UUID;


import org.json.JSONException;
import org.json.JSONObject;

public class MsgOn {
	NgrokClient ngrokcli;
	
	public MsgOn(NgrokClient ngrokcli) {
		this.ngrokcli=ngrokcli;
		// TODO Auto-generated constructor stub
	}

	public void jsonunpack(String str,SelectionKey key) {
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
					AuthResp(json, key);
				} else {
					Log.print("AuthResp .....error....");
				}
			}

			if (type.equals("ReqProxy")) {

				ReqProxy(json);

			}

			// ping ack
			if (type.equals("Ping")) {

				Ping(json, key);
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
				StartProxy(json, key);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void AuthResp(JSONObject json, SelectionKey key) {

		// 请求映射
		try {
			JSONObject Payload = json.getJSONObject("Payload");
			ngrokcli.ClientId = Payload.getString("ClientId");
			
			
			HashMap<String, String> tunelInfo;
			
	        for(int i = 0;i < ngrokcli.tunnels.size(); i ++){
	        	tunelInfo=ngrokcli.tunnels.get(i);
	        	String ReqId =  UUID.randomUUID().toString().toLowerCase().replace("-", "").substring(0, 8);
	        	this.ngrokcli.msgSend.SendReqTunnel(key,ReqId, tunelInfo.get("Protocol"),tunelInfo.get("Hostname"),tunelInfo.get("Subdomain"),tunelInfo.get("RemotePort"),tunelInfo.get("HttpAuth"));
	        	HashMap<String, String> tunelInfo1 = new HashMap<String, String>();
	        	tunelInfo1.put("localhost", tunelInfo.get("localhost"));
	        	tunelInfo1.put("localport", tunelInfo.get("localport"));
	        	ngrokcli.tunnelinfos.put(ReqId, tunelInfo1);
	        	
	        }
	        //ping一次
	        this.ngrokcli.msgSend.SendPing(key);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void ReqProxy(JSONObject json) {
		
  	  SockInfo sockinfo=new SockInfo();
		
  	  sockinfo.type=3;
  	  sockinfo.tokey=null;
  	  sockinfo.forward=0;
  	  
	  SelectionKey key=ngrokcli.connect(ngrokcli.serveraddr,ngrokcli.serverport,false,sockinfo);		

	}

	public void Ping(JSONObject json,SelectionKey key) {
		this.ngrokcli.msgSend.SendPong(key);
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
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void StartProxy(JSONObject json, SelectionKey key) {
	
			try{
				JSONObject Payload = json.getJSONObject("Payload");
				String Url=Payload.getString("Url");
				
			  	  SockInfo sockinfo=new SockInfo();
					
			  	  sockinfo.type=2;
			  	  sockinfo.tokey=key;
			  	  sockinfo.forward=0;
			  	  
				SelectionKey localKey=ngrokcli.connect(ngrokcli.tunnelinfos.get(Url).get("localhost"), Integer.parseInt(ngrokcli.tunnelinfos.get(Url).get("localport")),false,sockinfo);
	
				 //处理远程连接替换
				  SockInfo sockinfo1=(SockInfo) key.attachment();
				  sockinfo1.forward=1;
				  sockinfo1.tokey=localKey;
				  key.attach(sockinfo1);

			
			}catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	}

	public void unpack(SelectionKey key,ByteBuffer decrypted) {
		
		
		SockInfo sockinfo=(SockInfo) key.attachment();
		


		if(sockinfo.buf==null){
			sockinfo.buf = new byte[4096];
			sockinfo.buflen=0;
		}
		
    	byte[] buffer = new byte[decrypted.remaining()];
    	decrypted.get(buffer);
    	
		int len = buffer.length;
		BytesUtil.myaddBytes(sockinfo.buf, sockinfo.buflen, buffer, len);
		sockinfo.buflen = sockinfo.buflen + len;

		if (sockinfo.buflen > 8) {

			//
			int packlen = (int) BytesUtil
					.bytes2long(BytesUtil.leTobe(
							BytesUtil.cutOutByte(sockinfo.buf, 0, 8), 8), 0);
			
			// 加上头8个字节
			packlen = packlen + 8;
			if (sockinfo.buflen == packlen) {
				jsonunpack(
						new String(BytesUtil.cutOutByte(sockinfo.buf, 8,
								packlen - 8)), key);
				sockinfo.buflen = 0;
			}

			else if (sockinfo.buflen > packlen) {
				 
				jsonunpack(
						new String(BytesUtil.cutOutByte(sockinfo.buf, 8,
								packlen - 8)), key);
				sockinfo.buflen = sockinfo.buflen - packlen;
				BytesUtil.myaddBytes(sockinfo.buf, 0, BytesUtil.cutOutByte(
						sockinfo.buf, packlen, sockinfo.buflen), sockinfo.buflen);
			}

		}
	}
}
