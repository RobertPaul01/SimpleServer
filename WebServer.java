import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public final class WebServer {
	
	/**
	 * Main method to start serving data
	 * 
	 * @param argv
	 * @throws Exception
	 */
	public static void main(String argv[]) throws Exception {
		int port = -1;
		try {
			if(argv.length < 1) {
				System.out.println("Please provide the port number in the arguments");
				return;
			} else if (Integer.parseInt(argv[0]) > 1024 && Integer.parseInt(argv[0]) < 65535) {
				port = Integer.parseInt(argv[0]);
			}
		}
		catch(NumberFormatException e) {
			System.out.println("Argument 1 is not a valid port number");
		}
		
		@SuppressWarnings({ "resource" })
		ServerSocket listenSocket = new ServerSocket(port);

		int count = 0;
		
		while (true) {
			// Listen for and accept a TCP connection request.
			Socket connectionSocket = listenSocket.accept();
			
			// Construct an object to process the HTTP request message. 
			HttpRequest request = new HttpRequest(connectionSocket, count);
			// process request
			request.start();
			// increment the count so we can have unique thread names
			count ++;
		}
	}

}

/**
 * Final class that represents an HTTTP request, it will work asynchronously.
 * 
 * @author Robby
 *
 */
final class HttpRequest implements Runnable {

	final static String CRLF = "\r\n";
	
	Socket socket;
	int number;
	
	Thread t;
	
	/**
	 * Construct the request
	 * 
	 * @param socket
	 * @param number
	 * @throws Exception
	 */
	public HttpRequest(Socket socket, int number) throws Exception {
		this.socket = socket;
		this.number = number;
	}
	
	/**
	 * Process incoming HTTP requests
	 * 
	 * @throws Exception
	 */
	public void processRequest() throws Exception {
		// Get a reference to the socket's input and output streams. 
		InputStream is = socket.getInputStream();
		DataOutputStream os = new DataOutputStream(socket.getOutputStream());
		// Set up input stream filters. 
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		
		// Get the request line of the HTTP request message. 
		String requestLine = br.readLine();
		// Display the request line.
		System.out.println(); 
		System.out.println(requestLine);
		
		if (requestLine == null) {
			System.out.println("Error: null request, deny request");
			return;
		}
		
		// Get and display the header lines.
		String headerLine = null;
		while ((headerLine = br.readLine()).length() != 0) {
			System.out.println(headerLine); 
		}
		
		// Extract the filename from the request line.
		StringTokenizer tokens = new StringTokenizer(requestLine); 
		tokens.nextToken(); // skip over the method, which should be "GET" 
		String fileName = tokens.nextToken();
		// Prepend a "." so that the file request is within the current directory. 
		fileName = "." + fileName;
		// Remove a trailing '/'
		if (fileName.charAt(fileName.length()-1) == '/') {
			fileName = fileName.substring(0,fileName.length()-1);
		}
		
		// Open the requested file. 
		FileInputStream fis = null;
		boolean boolFileExists = true; 
		try {
			fis = new FileInputStream(fileName); 
		} catch (FileNotFoundException e) {
			try {
				fileName += "/";
				fis = new FileInputStream(fileName);
			} catch (FileNotFoundException f) {
				fileName = fileName.substring(0,fileName.length()-1);
				try {
					fileName = "./index.html";
					fis = new FileInputStream(fileName);
				} catch (FileNotFoundException g) {
					boolFileExists = false;
				}
			}
		}
		
		if (fileName.contains("..") || fileName.contains("$") || fileName.contains("^")) {
			System.out.println(fileName + " is not a valid file name.");
			boolFileExists = false;
		}
		
		// Construct the response message.
		String statusLine = null;
		String contentTypeLine = null;
		String entityBody = null;
		if (boolFileExists) {
			statusLine = "HTTP/1.1 200 OK" + CRLF;
			contentTypeLine = "Content-type: " +
					contentType(fileName) + CRLF;
		} else {
			statusLine = "HTTP/1.1 404 Not Found" + CRLF;
			contentTypeLine = "Content-type: text/html" + CRLF;
			entityBody = "<HTML>" +
					"<HEAD><TITLE>Not Found</TITLE></HEAD>" + 
					"<BODY>Not Found</BODY></HTML>";
		}
		
		// Send the status line.
		os.writeBytes(statusLine);
		// Send the content type line.
		os.writeBytes(contentTypeLine);
		// Send a blank line to indicate the end of the header lines. 
		os.writeBytes(CRLF);
		
		// Send the entity body. 
		if (boolFileExists) {
			sendBytes(fis, os);
			fis.close(); 
		} else {
			os.writeBytes(entityBody);
		}
		
		// Close streams and socket. 
		os.close();
		br.close();
		socket.close();	
	}
	
	/**
	 * Send bytes from the FileInputStream to the OutputStream
	 * 
	 * @param fis
	 * @param os
	 * @throws Exception
	 */
	private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {
		// Construct a 1K buffer to hold bytes on their way to the socket. 
		byte[] buffer = new byte[1024];
		int bytes = 0;
		// Copy requested file into the socket's output stream. 
		while((bytes = fis.read(buffer)) != -1 ) {
			os.write(buffer, 0, bytes);
		} 
	}
	
	/**
	 * Return the MIME content type for the file name
	 * 
	 * @param fileName
	 * @return
	 */
	private String contentType(String fileName) {
		if(fileName.endsWith(".htm") || fileName.endsWith(".html")) { 
			return "text/html";
		} else if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
			return "image/jpg"; 
		} else if(fileName.endsWith(".png")) {
			return "image/png";
		} else if(fileName.endsWith(".gif")) {
			return "image/gif";
		} else if(fileName.endsWith(".css")) {
			return "text/css";
		}
		return "application/octet-stream";
	}

	/**
	 * Runs the thread
	 * 
	 */
	@Override
	public void run() {
		try {
			processRequest();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** 
     * Prepare the thread for execution 
	 */
	public void start () {
      if (t == null) {
         t = new Thread (this, number+"");
         t.start ();
      }
   }
	
}