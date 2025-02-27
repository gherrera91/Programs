package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 * 
 * @author Jon Cook, Ph.D.
 *
 **/

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.lang.Runnable; 
import java.io.*; 

public class WebWorker implements Runnable
{

	private Socket socket;

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		String address = ""; // initializes the address 
		String contentType = "text/html"; 

		System.err.println("Handling connection...");
		try
		{
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			address = readHTTPRequest(is);

			if(address.contains(".png")){
				contentType = "image/png"; 
			}
			else if(address.contains(".jpg")){
				contentType = "image/png"; 
			}
			else if(address.contains(".jpeg")){
				contentType = "image/jpeg";
			}
			else if(address.contains(".gif")){
				contentType = "image/gif"; 
			}

			writeHTTPHeader(os, contentType,address);
			writeContent(os,contentType, address);

			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.");
		return;
	}

	/**
	 * Read the HTTP request header.
	 **/
	private String readHTTPRequest(InputStream is)
	{
		String line;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));

		String address = ""; 
		while (true)
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
				if(line.contains("GET")){ 
					address = line.substring(4); 
					for(int i = 0; i<address.length(); i++){
						if(address.charAt(i)==' '){
							address = address.substring(0,i);
						} 
					}
				}
				System.err.println("Request line: (" + line + ")");
				if (line.length() == 0)
					break;
			}
			catch (Exception e)
			{
				System.err.println("Request error: " + e);
				break;
			}
		}
		return address;
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType, String address) throws Exception
	{
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));

		String copy = '.' + address; 
		File f = new File(copy); 
		
		try{
			FileReader file = new FileReader(f); 
			BufferedReader r = new BufferedReader(file); 
		}
		catch(Exception e){
			System.out.println("File not found:" + address); 
			os.write("HTTP/1.1 404 Error: Not Found\n".getBytes()); 
		}
		
		

		os.write("HTTP/1.1 200 OK\n".getBytes());
		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Jon's very own server\n".getBytes());
		// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		// os.write("Content-Length: 438\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os, String contentType, String address) throws Exception
	{
		Date d = new Date(); 
		DateFormat dformat = DateFormat.getDateInstance(); 
		dformat.setTimeZone(TimeZone.getTimeZone("GMT"));

		String fcont = ""; 
		String copy = "." + address.substring(0, address.length()); 
		String date = dformat.format(d); 
		File f = new File(copy); 
		if(contentType.contains("text/html")){
			try{
				FileReader fRead = new FileReader(f); 
				BufferedReader fBuff = new BufferedReader(fRead); 

				while((fcont = fBuff.readLine()) != null){
					os.write(fcont.getBytes()); 
					os.write("\n".getBytes()); 
			

					if (fcont.contains("<cs371date>")){
						os.write(date.getBytes());
					}

					if(fcont.contains("<cs371server")){
						os.write("Geralds Server \n".getBytes()); 
					}
				}
			}
			catch (Exception e){
				System.err.println("File Not Found:" + address); 
				os.write("<h1>404 Error: Not Found <h1> \n".getBytes()); 
			}
		}
		else if(contentType.contains("image")){
			int marker; 
			FileInputStream ff = new FileInputStream(f); 
			int size = (int) f.length();
			byte x[] = new byte[size]; 
			while((marker = ff.read(x)) > 0){
				os.write(x,0,marker); 

			} 

		}

		//os.write("<html><head></head><body>\n".getBytes());
		//os.write("<h3>My web server works!</h3>\n".getBytes());
		//os.write("</body></html>\n".getBytes());
	}

} // end class
