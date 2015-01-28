package com.geek.ngrok;


public class ngrok {
	static String ClientId = "";

	public static void main(String[] args) {
		System.out.println("main start");
		NgrokClient ngclient=new NgrokClient();
		ngclient.start("127.0.0.1",80,"tcp",true);

	}
}
