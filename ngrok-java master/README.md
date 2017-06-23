# ngrok-java 
Use


NgrokClient ngclient=new NgrokClient();

NgrokClient ngclient=new NgrokClient("ngrok.com",4443);

//addtunnel

//http
ngclient.addTun("127.0.0.1",80,"http","www.xxx.com","",0,"");

//tcp
ngclient.addTun("127.0.0.1",80,"tcp","","",50000,"");

//start
ngclient.start();



TCP is a must way authtoken.

Go get your authtoken ngrok.
