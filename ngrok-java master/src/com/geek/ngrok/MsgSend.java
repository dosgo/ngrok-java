package com.geek.ngrok;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

public class MsgSend {
	

	public static  void  SendAuth(String ClientId,String user,OutputStream o) {

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
			pack(msgjson.toString(),o);
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public static  void  SendReqTunnel(OutputStream o,String Protocol) {
		// µÃµ½¶ÔÏó²úÉúµÄID
		String ReqId =  UUID.randomUUID().toString().toLowerCase().replace("-", "").substring(0, 8);
		try {
			JSONObject msgjson=new JSONObject();
			msgjson.put("Type","ReqTunnel");
			
			JSONObject Payloadjson=new JSONObject();
			Payloadjson.put("ReqId", ReqId);
			Payloadjson.put("Protocol", Protocol);
			if(Protocol.equals("tcp"))
			{
				Payloadjson.put("RemotePort",0);
			}
			
			else
			{
				Payloadjson.put("Subdomain", "");
				Payloadjson.put("HttpAuth", "");
				Payloadjson.put("Hostname", "");
			}
			msgjson.put("Payload", Payloadjson);
			pack(msgjson.toString(),o);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	public static  void  SendPong(OutputStream o) {		
		pack("{\"Type\":\"Pong\",\"Payload\":{}}",o);
	}
	
	public static  void  SendPing(OutputStream o) {		
		pack("{\"Type\":\"Ping\",\"Payload\":{}}",o);
	}
	

	public static  void  SendRegProxy(String ClientId,OutputStream o) {		
		

		pack("{\"Type\":\"RegProxy\",\"Payload\":{\"ClientId\":\""
				+ ClientId + "\"}}",o);
	}
	
	
	public static void pack(String str, OutputStream o) {
		byte[] lenbuf = BytesUtil.longToBytes(str.length(), 0);
		byte[] xx = str.getBytes();
		byte[] msgpack = BytesUtil.addBytesnew(str.length() + 8, lenbuf, xx);

		try {
			o.write(msgpack, 0, str.length() + 8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
