import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.io.*;

public class ServerThread extends Thread {
    private Socket socket = null;
	private String userDir, rootDir;

    public ServerThread(Socket socket, String name, String rootDir, String userDir) {
    	super("ServerThread");
    	this.setName(name);
    	this.socket = socket;
    	this.userDir = userDir;
    	this.rootDir = rootDir;
    }
    
    /**
     * Main thread can call this to close the worker's socket.
     */
    public void closeSocket() {
    	try {
			socket.close();
		} catch (IOException e) {
			System.out.println("Main thread failed to close worker socket");
			e.printStackTrace();
		}
    }
    
    /**
     * Worker thread's body
     */
    public void run() {
    	//Create streams
    	DataOutputStream outputStrm = null;
    	BufferedReader inputStrm = null;
    	String requestLine;
		try {
			//Safely hook them up to the socket
			outputStrm = new DataOutputStream(socket.getOutputStream());
		    inputStrm = new BufferedReader(
					    new InputStreamReader(
					    socket.getInputStream()));
			while (true) {
				//Main worker loop...
				//Continue to handle requests until exception is thrown
				requestLine = inputStrm.readLine();
				if (requestLine != null) { 
		    		String up = requestLine;
		    		up.toUpperCase();
		    		if (up.startsWith("GET") || up.startsWith("HEAD")) {
		    			//System.out.println(requestLine);
		    			respond(requestLine, outputStrm);
		    		}
				}
			}
		}
		catch(SocketException se) {
			//This all-important catch block executes when the socket has been closed before I/O.
			//It kills the thread
			try {
				Thread.currentThread().join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
		catch(IOException e) {
			System.out.println("I/O error on socket: " + e.getMessage());
		}
		finally{
			//Make sure stuff is closed once and for all.
		    try {
		    	inputStrm.close();
				outputStrm.close();
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    }
    
    /**
     * Responds appropriately to GET and HEAD requests
     */
	private void respond(String request, DataOutputStream outputStrm) {
		StringTokenizer tokens = new StringTokenizer(request);

        int get = 0;
        String requestedPath = new String();
        Path filepath = null;
        String username = null;
        try {
        	if (!tokens.hasMoreTokens()) {throw new Exception();}
	        String up = tokens.nextToken();
	        up.toUpperCase();
	        //Determine request type
	        if (up.equals("GET")) {
	          get = 1;
	        }
	        else if (!up.equals("HEAD")){
	        //Send 501 for invalid request type
	            try {
	            	String message = "<html><body>501 Unsupported</body></html>";
	                outputStrm.writeBytes(makeHeader(501, message.length() * 2));
	                outputStrm.writeChars(message);
	            }
	            catch (Exception e3) {
	              System.out.println(e3.getMessage());
	            }
	            return;
	        }
	        //Determine requested file path
	        if (!tokens.hasMoreTokens()) {throw new Exception();}
	        requestedPath = tokens.nextToken();
	        
	        //Detect user folder request
	        if (requestedPath.length() > 2 && requestedPath.charAt(1) == '~') {
	        	int slash = requestedPath.indexOf('/', 2);
	        	username = requestedPath.substring(2, slash);
	        	filepath = Paths.get(userDir, username, "public_html", 
	        			requestedPath.substring(requestedPath.lastIndexOf('/')));
	        }
	        //Else use rootDir
	        else {
	        	filepath = Paths.get(rootDir, requestedPath);
	        }
        }
        catch (Exception b) {
        	//400 due to gibberish request.
        	try {
        		String message = "<html><body>400 Bad Request</body></html>";
                outputStrm.writeBytes(makeHeader(400, message.length() * 2));
                outputStrm.writeChars(message);
        	}
        	catch (Exception e6) {System.out.println(e6.getMessage());}
        	return;
        }
        
        //Safely get the whole file for output if we can, otherwise send 404.
        byte[] outputBytes = null;
        try {
        	//Handle folder index defaults
        	if (!new File(filepath.toString()).isFile()){
        		if (new File(filepath.resolve("index.html").toString()).isFile()){
        			filepath = filepath.resolve("index.html");
        		}
        		else if (new File(filepath.resolve("index.htm").toString()).isFile()){
        			filepath = filepath.resolve("index.htm");
        		}
        		else {
        			//404 out if path is not a file, and there is no index.html or index.htm
        			throw new Exception();
        		}
        	}
        	outputBytes = Files.readAllBytes(filepath);
        }
        catch (Exception e) {
        	try {
        		String message = "<html><body>404 Not Found</body></html>";
        		outputStrm.writeBytes(makeHeader(404, message.length() * 2));
        		outputStrm.writeChars(message);
        	}
        	//Output error messages
        	catch (Exception e4) {System.out.println(e4.getMessage());}
        	System.out.println(e.getMessage());
        	return;
        }

        try {
        	//Guess the file's mime based on its extension.
        	int contentCode = -1;
        	requestedPath.toLowerCase();
        	if (requestedPath.endsWith(".zip")) {
        		contentCode = 3;}
        	else if (requestedPath.endsWith(".jpg") || requestedPath.endsWith(".jpeg")) {
        		contentCode = 1;}
        	else if (requestedPath.endsWith(".gif")) {
        		contentCode = 2;}
        	else if (requestedPath.endsWith(".css")) {
        		contentCode = 3;}
        
        	//Write the 200 header of success!
        	outputStrm.writeBytes(makeHeader(200, contentCode, outputBytes.length));
        	
        	//Include the file's contents as well, if it is a GET request.
        	if (outputBytes != null && get == 1) {
        		outputStrm.write(outputBytes);
        	}
        }

        catch (Exception e) {System.out.println("Error during file transmission -- " + e.getMessage());}

    }
	
	/**
     * Generates HTTP headers to be sent by respond()
     */
    private String makeHeader(int resultCode, int contentCode, Integer length) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
    	
    	String headerString = new String("HTTP/1.1 ");
        switch (resultCode) {
        	case 200:
        		headerString = headerString + "200 OK"; break;
        	case 400:
        		headerString = headerString + "400 Bad Request"; break;
        	case 404:
        		headerString = headerString + "404 Not Found"; break;
        	case 501:
        		headerString = headerString + "501 Not Implemented"; break;
        }

        headerString = headerString + "\r\n";
        
        headerString = headerString + "Date: " + dateFormat.format(calendar.getTime()) + "\r\n";
        
        switch (contentCode) {
          case 0: break;
          case 1:
            headerString = headerString + "Content-Type: image/jpeg\r\n"; break;
          case 2:
            headerString = headerString + "Content-Type: image/gif\r\n"; break;
          case 3:
            headerString = headerString + "Content-Type: text/css\r\n"; break;
          default:
            headerString = headerString + "Content-Type: text/html\r\n"; break;
        }
        
        if (length != null) {
        	headerString = headerString + "Content-Length: " + length + "\r\n";
        }
        
        //End of header
        headerString = headerString + "\r\n";
        return headerString;
    }
    
    private String makeHeader(int return_code, int length) {
    	return makeHeader(return_code, 0, length);
    }
}
