package com.geek.ngrok;


public class ngrok {
	static String ClientId = "";

	public static void main(String[] args) {
		//new
		NgrokClient ngclient=new NgrokClient();
		//addtunnel
		ngclient.addTun("127.0.0.1",81,"http","","",0,"");
		ngclient.addTun("127.0.0.1",80,"http","","",0,"");
		//start
		ngclient.start();
		//check error
		while(true){
			if(ngclient.lasttime+30<(System.currentTimeMillis() / 1000)&&ngclient.lasttime>0){
				Log.print("check err");
				
				ngclient.trfalg=false;
				ngclient.tunnelinfos.clear();//
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//reconnct
				ngclient.trfalg=true;
				ngclient.start();
				
			}else{
				Log.print("check ok");
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
