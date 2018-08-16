package com.geek.ngrok;


import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;


import org.json.JSONException;
import org.json.JSONObject;

public class MsgSend {
	
	NgrokClient ngrokcli;
	public MsgSend(NgrokClient ngrokcli) {
		this.ngrokcli=ngrokcli;
	}
	
	public   void  SendAuth(String ClientId,String user,SelectionKey key) {

		try {
			JSONObject msgjson=new JSONObject();
			msgjson.put("Type","Auth");
			JSONObject Payloadjson=new JSONObject();
			Payloadjson.put("Version", "2");
			Payloadjson.put("MmVersion", "1.7");
			Payloadjson.put("User", user);
			Payloadjson.put("Password", "");
			Payloadjson.put("OS", "darwin");
			Payloadjson.put("Arch", "amd64");
			Payloadjson.put("ClientId", ClientId);
			msgjson.put("Payload", Payloadjson);
			pack(msgjson.toString(),key);
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public   void  SendReqTunnel(SelectionKey key,String ReqId,String Protocol,String Hostname,String Subdomain,String RemotePort,String HttpAuth) {
		//
		try {
			JSONObject msgjson=new JSONObject();
			msgjson.put("Type","ReqTunnel");
			
			JSONObject Payloadjson=new JSONObject();
			Payloadjson.put("ReqId", ReqId);
			Payloadjson.put("Protocol", Protocol);
			if(Protocol.equals("tcp"))
			{
				Payloadjson.put("RemotePort",RemotePort);
			}
			
			else
			{
				Payloadjson.put("Subdomain", Subdomain);
				Payloadjson.put("HttpAuth", HttpAuth);
				Payloadjson.put("Hostname", Hostname);
			}
			msgjson.put("Payload", Payloadjson);
			pack(msgjson.toString(),key);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	public   void  SendPong(SelectionKey key) {		
		pack("{\"Type\":\"Pong\",\"Payload\":{}}",key);
	}
	
	public   void  SendPing(SelectionKey key) {		
		pack("{\"Type\":\"Ping\",\"Payload\":{}}",key);
	}
	

	public   void  SendRegProxy(String ClientId,SelectionKey key) {		
		

		pack("{\"Type\":\"RegProxy\",\"Payload\":{\"ClientId\":\""
				+ ClientId + "\"}}",key);
	}
	
	
	public  void pack(String str,SelectionKey key) {
		byte[] lenbuf = BytesUtil.longToBytes(str.length(), 0);
		byte[] msgpack = BytesUtil.addBytesnew(str.length() + 8, lenbuf, str.getBytes());				
		ngrokcli.ssl.sendAsync(key, ByteBuffer.wrap(msgpack));
	}
}
