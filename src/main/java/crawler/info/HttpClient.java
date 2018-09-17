import java.io.BufferedReader;   
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.net.ssl.HttpsURLConnection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class HttpClient {
	private Socket s;
	private HttpsURLConnection con;
	private BufferedReader in;
	private PrintWriter out;
	private URL URL;
	private String contentType=null;
	private long contentLength=0;
	private int status=0;
	private String contentString=null;
	private String RedirectedLocation = null;
	
	public HttpClient(){}
	
	//open connection according to protocol http or https
	public void openConnection(String host) throws Exception{
		//if the protocol equals to http, open a socket as connection
		if (URL.getProtocol().equals("http")){
			//InetAddress address = null;
			//address = InetAddress.getByName(host);
			s = new Socket(host, 80);
			//set I/O based on socket
			InputStreamReader reader = new InputStreamReader(s.getInputStream());
			in = new BufferedReader(reader);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
			out = new PrintWriter(writer);
		//else open a HttpsURLConnection as the connection
		}else if (URL.getProtocol().equals("https")){
			con = (HttpsURLConnection) URL.openConnection();
		}
	}
	
	//send "HEAD" request with the request url and the time of last crawl
	public void sendHeadRequest(String url,Date lastCrawledTime) throws Exception{
		this.URL = new URL(url);
		openConnection(URL.getHost());
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		//send the request with the outputStream of socket if the protocol is http
		if (URL.getProtocol().equals("http")){
			out.write("HEAD "+URL.getPath()+" HTTP/1.1\r\n");
			out.write("Host:"+URL.getHost()+"\r\n");
			//send If-Modified-since header if there is a lastCrawledTime
			if (lastCrawledTime != null) {
				out.write("User-Agent: cis455crawler\r\n");
				String time = sdf.format(lastCrawledTime);
				out.write("If-Modified-Since: "+time+" GMT\r\n\r\n");
			}else{
				out.write("User-Agent: cis455crawler\r\n\r\n");
			}
			out.flush();
		//send the request just with httpsURLConncetion
		}else if (URL.getProtocol().equals("https")){
			con.setRequestMethod("HEAD");
			con.setRequestProperty("Host", URL.getHost());
			con.setRequestProperty("User-Agent", "cis455crawler");
			if (lastCrawledTime != null) {
				con.setIfModifiedSince(lastCrawledTime.getTime());
			}
		}
	}
	
	//send "GET" request with the requesting URL
	public void sendGetRequest(String url) throws Exception {
		this.URL = new URL(url);
		openConnection(URL.getHost());
		if (URL.getProtocol().equals("http")){
			out.write("GET "+URL.getPath()+" HTTP/1.1\r\n");
			out.write("Host: "+URL.getHost()+"\r\n");
			out.write("User-Agent: cis455crawler\r\n\r\n");
			out.flush();
			
		}else if (URL.getProtocol().equals("https")){
			con.setRequestMethod("GET");
			con.setRequestProperty("Host", URL.getHost());
			con.setRequestProperty("User-Agent", "cis455crawler");
			in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		}
	}
	
	//parse the response after sending "HEAD" request
	public void parseHeadResponse(boolean closeAfterwards){
		try {
			//determine whether the socket is used or the httpsURLConnection is used as the connection of the client
			if (this.s != null) {
				//read and store the response status
				String line = in.readLine();
				if (line != null){		
					try{
						status = Integer.parseInt(line.split(" ")[1]);
					}catch (NumberFormatException e){}
				}
				while (!line.isEmpty()) {
					//read and store the content-length
					if (line.toLowerCase().contains("content-length")) {
						contentLength = Long.parseLong(line.substring(line.indexOf(":") + 1).trim());
					//read and store content-type
					} else if (line.toLowerCase().contains("content-type")) {
						if (line.contains(";"))
							contentType = line.substring(line.indexOf(":") + 1 , line.indexOf(";")).trim();
						else
							contentType = line.substring(line.indexOf(":") + 1).trim();
					//read and store the redirecting location if exists
					} else if (line.toLowerCase().contains("location"))
						this.RedirectedLocation = line.substring(line.indexOf(":") +1).trim();
					line = in.readLine();
				}
			} else if (con != null){
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				status = con.getResponseCode();
				contentLength = con.getContentLengthLong();
				contentType = con.getContentType();
				if (con.getHeaderField("Location")!=null)
					RedirectedLocation = con.getHeaderField("Location");
			}
			// close the connection if the input boolean variable closeAfterwards is true
			if (closeAfterwards)
				close();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public String getRedirectLocation(){
		return this.RedirectedLocation;
	}
	
	//parse the response after sending "GET" request
	public void parseGetResponse(){
		try {
			//parse the initialLine and headers first and keep the connection unclosed
			parseHeadResponse(false);
			//only retrieve HTML and XML files
			if (status == 200){
				if (this.contentType.equals("text/html")) { 
					StringBuilder body = new StringBuilder();
					for (int i = 0; i<this.contentLength; i++){
						body.append((char) in.read());
					}
					this.contentString = body.toString();
				} else if (this.contentType.endsWith("+xml") || contentType.equals("text/xml") || contentType.equals("application/xml")) {
					StringBuilder body = new StringBuilder();
					for (int i = 0; i<this.contentLength; i++){
						body.append((char) in.read());
					}
					this.contentString = body.toString().substring(0,body.toString().lastIndexOf('>')+1);
					if (!contentString.contains("?xml version="))
						contentString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + contentString;
//					System.out.println(contentString);
				}
			}
		//close the connection after parsing
		close();	
		} catch (Exception e) {} 
	}
	
	//parse response after sending the request to get robot.txt of the website
	public RobotsTxtInfo parseRobotTxt(String rootURL) throws Exception{
		RobotsTxtInfo robot = new RobotsTxtInfo();
		//send the request to get the robot.txt
		String url = rootURL+"/robots.txt";
		sendGetRequest(url);
		//parse the response headers first
		parseHeadResponse(false);
		String agent = null;
		String line = "";
		//parse the robot.txt, and get the user-agent, disAllowing links and allowing links, crawl delay, if exist
		while ((line = in.readLine()) !=null) {
			if (line.toLowerCase().contains("user-agent")) {
				agent = line.substring(line.indexOf(':') + 1).trim();
				robot.addUserAgent(agent);
			} else if (line.toLowerCase().contains("disallow")) {
				String path = line.substring(line.indexOf(':') + 1).trim();
				robot.addDisallowedLink(agent, path);
			} else if (line.toLowerCase().contains("allow")) {
				String path = line.substring(line.indexOf(':') + 1).trim();
				robot.addAllowedLink(agent, path);
			} else if (line.toLowerCase().contains("crawl-delay")) {
				int delay = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
				robot.addCrawlDelay(agent, delay);
			}
		}
		close();
		return robot;
		
	}
	
	public void close() throws IOException {
		//close the IO/socket/httpsURLConnection
		if (this.out != null) 
			this.out.close();
		if (this.in != null) 
			this.in.close();
		if (this.s != null) 
			this.s.close();
		if (this.con != null) 
			this.con.disconnect();
		s = null;
		con = null;
	}
	
	public Document getHTMLDocument(){
		//get the document object of the achieved html document using jsoup, which is easy to extract URL links
		try {
			if (this.contentType.equals("text/html")) {
				String baseURL = getBaseURL(URL);
				Document d = Jsoup.parse(contentString, baseURL);
				return d;
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}
	
	// get the base URL of a HTML document with the URL object the document was achieved with
	public static String getBaseURL(URL URL) throws MalformedURLException{
		String baseURL = null;
		if (URL.getPort()!=-1)
			baseURL = URL.getProtocol()+"://"+URL.getHost()+":"+URL.getPort()+URL.getPath();
		else
			baseURL = URL.getProtocol()+"://"+URL.getHost()+URL.getPath();
		return baseURL;
	}
	
	public long getContentLength(){
		return this.contentLength;
	}
	
	public String getContentType(){
		return this.contentType;
	}
	
	public int getStatus(){
		return this.status;
	}
	
	public String getContentString(){
		return this.contentString;
	}
	
	public void setURL(String url) throws MalformedURLException{
		this.URL = new URL(url);
	}
}
