# ngrok-java 
Use

NgrokClient ngclient=new NgrokClient();

NgrokClient ngclient1=new NgrokClient();

//Http

ngclient.start("127.0.0.1",80,"http","",true);

//TCP

ngclient1.start("127.0.0.1",22,"tcp","you authtoken",true);

TCP is a must way authtoken.

Go get your authtoken ngrok.
