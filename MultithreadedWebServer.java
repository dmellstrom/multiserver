import java.net.*;
import java.io.*;

public class MultithreadedWebServer {
	private static ServerThread[] pool;
	
	public static void main(String[] args) throws IOException, InterruptedException {
    	String rootDir = "";
    	String userDir = "";
    	int maxConnections = 100; //Default values; overwritten by argument.
    	//Argument handler
    	if (args.length != 3){
    		System.out.println("Usage: java MultithreadedWebServer maxConnections rootDir userDir");
    		System.exit(0);
    	}
    	else {
    		maxConnections = Integer.decode(args[0]);
    		rootDir = args[1];
    		userDir = args[2];
    	}
    	
        ServerSocket serverSocket = null;
        //Safely establish the server's listening status
        try {
            serverSocket = new ServerSocket(8888);
        } 
        catch (IOException e) {
            System.err.println("Could not establish listening port 8888.");
            System.exit(-1);
        }
        
        pool = new ServerThread[maxConnections];
        int i = 0;
        //Main server loop
        while (true) {
        	//Block until there is a new cxn
        	Socket s = serverSocket.accept();
        	s.setKeepAlive(true);

        	//Kill an old thread, if there are too many
        	if (pool[i] != null && pool[i].isAlive()) {
        		pool[i].closeSocket();
        	}
        	//Start worker thread to handle connection
        	pool[i] = new ServerThread(s, "ServerThread" + i, rootDir, userDir);
        	pool[i].start();
        	
        	//Handle thread count
        	i++;
        	if (i >= pool.length) {
        		i = 0;
        	}
        }
    }
}
