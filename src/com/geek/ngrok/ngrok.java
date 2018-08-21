package com.geek.ngrok;



public class ngrok{
	

	
	 public static void main(String args[])  throws Exception{ 
		 
		//new
			NgrokClient ngclient=new NgrokClient();
			//addtunnel
			ngclient.addTun("172.20.131.130",80,"http","","test1",0,"");
			//start
			ngclient.start();
     
	}
	 
	 
	
}




