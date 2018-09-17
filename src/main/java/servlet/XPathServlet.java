import java.io.IOException;    
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.servlet.http.*;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Transaction;

import storage.Channel;
import storage.DBWrapper;
import storage.Doc;
import storage.User;

@SuppressWarnings("serial")
public class XPathServlet extends HttpServlet {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException 
	{	
		//determine the get response by URI 
		String URI = request.getRequestURI();
		request.getSession(true);
		DBWrapper db = new DBWrapper(getServletContext().getInitParameter("BDBstore"));
		//if the URI of the request equals to /lookup, then retrieve corresponding file with respect to query string
		if(URI.equals("/lookup")){
			String query = request.getQueryString().substring(4).trim();
			//open BerkeleyDB and get the corresponding file
			Transaction transaction = db.getTransaction();
			Doc doc = db.getDoc(query);	
			transaction.commit();
			//set and send the response
			response.setContentType(doc.getcontentType());
			PrintWriter out = response.getWriter();
			out.println(doc.getcontentString());

		}else{
			//determine whether the client is logged in
			boolean loggedIn = false;
			Cookie[] cookies = request.getCookies();
			Cookie cookie = null;
			if (cookies != null){
				for (Cookie c : cookies) {
					if (c.getName().equals("cis455")){ 
						cookie = c;
						loggedIn = true;
					}
				}
			}
			//if the client is logged in, send the main page to the client, as well as a logout choice
			if (loggedIn){
				String username = cookie.getValue();
				//if URI is logout, set the cookie to expire at once, and redirect the client to the main page
				if (URI.equals("/create")){
					String channelName = request.getParameter("name");
					String xpath = request.getParameter("xpath");
					try {
						boolean exist = !db.insertChannel(channelName, xpath, username);
						//report 409 error if the channel already exists
						if(exist){
							response.setContentType("text/html");
							response.setStatus(HttpServletResponse.SC_CONFLICT);
							PrintWriter out = response.getWriter();
							out.println("<html><head>Error 409</head>");
							out.println("<title>Xpath Servlet</title>");
							out.println("<body><h1>Channel already exist!</h1>");
							out.println("</html>");
						//redirect to main page
						}else{
							response.setContentType("text/html");
							response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
							response.setHeader("Location", "/xpath");
							response.sendRedirect("/xpath");
						}
					} catch (DatabaseException e) {		
						
					} catch (Exception e) {
						
					}
				}else if(URI.equals("/delete")){
					String channelName = request.getParameter("name");
					//report 404 error if the channel is not found
					if (!db.containsChannel(channelName)){
						response.setContentType("text/html");
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						PrintWriter out = response.getWriter();
						out.println("<html><head>Error 404</head>");
						out.println("<title>Xpath Servlet</title>");
						out.println("<body><h1>Channel Not Found!</h1>");
						out.println("</html>");
					}else{
						//redirect to main page if delete successfully;
						if(db.deleteChannel(channelName, username)){
							response.setContentType("text/html");
							response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
							response.setHeader("Location", "/xpath");
							response.sendRedirect("/xpath");
						}else{
						//report 403 error if the deleter is not the creator
							response.setContentType("text/html");
							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
							PrintWriter out = response.getWriter();
							out.println("<html><head>Error 403</head>");
							out.println("<title>Xpath Servlet</title>");
							out.println("<body><h1>You are not the owner ot the channel!</h1>");
							out.println("</html>");
						}
					}
				//for subscribing
				}else if(URI.equals("/subscribe")){
					String channelName = request.getParameter("name");
					if (!db.containsChannel(channelName)){
						response.setContentType("text/html");
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						PrintWriter out = response.getWriter();
						out.println("<html><head>Error 404</head>");
						out.println("<title>Xpath Servlet</title>");
						out.println("<body><h1>Channel Not Found!</h1>");
						out.println("</html>");
					}else{
						Channel c = db.getChannel(channelName);
						if (c.isSubscriber(username)){
							response.setContentType("text/html");
							response.setStatus(HttpServletResponse.SC_CONFLICT);
							PrintWriter out = response.getWriter();
							out.println("<html><head>Error 409</head>");
							out.println("<title>Xpath Servlet</title>");
							out.println("<body><h1>Channel already subscribed!</h1>");
							out.println("</html>");
						}else{
							c.addSubscriber(username);
							db.insertChannel(c);
							response.setContentType("text/html");
							response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
							response.setHeader("Location", "/xpath");
							response.sendRedirect("/xpath");
						}
					}
				//for unsubscribing
				}else if(URI.equals("/unsubscribe")){
					String channelName = request.getParameter("name");
					Channel c = db.getChannel(channelName);
					if (c.isSubscriber(username)){
						c.deleteSubscriber(username);
						db.insertChannel(c);
						response.setContentType("text/html");
						response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
						response.setHeader("Location", "/xpath");
						response.sendRedirect("/xpath");
					}else{
						response.setContentType("text/html");
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						PrintWriter out = response.getWriter();
						out.println("<html><head>Error 404</head>");
						out.println("<title>Xpath Servlet</title>");
						out.println("<body><h1>Channel Not Subscribed!</h1>");
						out.println("</html>");
					}
				//for showing
				}else if(URI.equals("/show")){
					String channelName = request.getParameter("name");
					if (!db.containsChannel(channelName)){
						response.setContentType("text/html");
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						PrintWriter out = response.getWriter();
						out.println("<html><head>Error 404</head>");
						out.println("<title>Xpath Servlet</title>");
						out.println("<body><h1>Channel Not Found!</h1>");
						out.println("</html>");
					}else{
						Channel c = db.getChannel(channelName);
						if (!c.isSubscriber(username)&&!c.getowner().equals(username)){
							response.setContentType("text/html");
							response.setStatus(HttpServletResponse.SC_NOT_FOUND);
							PrintWriter out = response.getWriter();
							out.println("<html><head>Error 404</head>");
							out.println("<title>Xpath Servlet</title>");
							out.println("<body><h1>Channel Not Subscribed!</h1>");
							out.println("</html>");
						}else{
							ArrayList<String> urls = c.getUrls();
							ArrayList<Doc> docs = new ArrayList<Doc>();
							for (String url:urls){
								Doc doc = db.getDoc(url);
								docs.add(doc);
							}
							SimpleDateFormat date1 = new SimpleDateFormat("yyyy-MM-dd");
							SimpleDateFormat date2 = new SimpleDateFormat("hh:mm:ss");
							PrintWriter out = response.getWriter();
							out.println("<html><body>");
							out.println("<div class=\'channelheader\'>");
							out.println("Channel name: " + c.getChannelName() + ", Created by:" + c.getowner()+"</div>");
							for (Doc doc:docs){
								String date = date1.format(doc.getLastCrawled())+"T"+date2.format(doc.getLastCrawled());
								out.println("<div class=\'documentheader\'>");
								out.println("Crawled on:" + date + "</br>");
								out.println("Location:"+doc.getUrl()+"<br/></div>");
								out.println("<div class='document'>");
								if (doc.getcontentType().endsWith("+xml") || doc.getcontentType().equals("text/xml") || doc.getcontentType().equals("application/xml")){
									out.println("<xmp>"+doc.getcontentString()+"</xmp>");
								}
								else
									out.println(doc.getcontentString());
								out.println("</div>");
							}
							out.println("</body></html>");
						}
					}
				}
					
				else if(URI.equals("/logout")){
					response.setContentType("text/html");
					cookie.setMaxAge(0);
					response.addCookie(cookie);
					response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
					response.setHeader("Location", "/xpath");
					response.sendRedirect("/xpath");
				}
				//if the client is not to logout, present the client with loggedIn info
				else{
					response.setContentType("text/html");
					PrintWriter out = response.getWriter();
					out.println("<html><head>");
					out.println("<title>Xpath Servlet</title>");
					out.println("<body><h1>username: "+username+"</h1>");
					
					ArrayList<Channel> allChannels = db.getAllChannels();
					//listing all the channels exist in the system so as well as some operations
					for (Channel c:allChannels){
						String output = "<h4>"+"Name: "+c.getChannelName() + "; xpaths:" + c.getXPaths();
						output += "; owner: "+c.getowner()+" ";
						if (c.getowner().equals(username)){
							output += "<a href=\"/show?name="+c.getChannelName()+"\">show</a> "+" <a href=\"/delete?name="+c.getChannelName()+"\">delete</a>";
						}else {
							if(c.isSubscriber(username)){
								output += "<a href=\"/show?name="+c.getChannelName()+"\">show</a> "+" <a href=\"/unsubscribe?name="+c.getChannelName()+"\">unsubscribe</a>";
							}else{
								output += "<a href=\"/subscribe?name="+c.getChannelName()+"\">subscribe</a>";
							}	
						}
						output += "</h4>";
						out.println(output);
					}
					//user could create channel on the bottom
				    out.println("<form action=\"/create\" method=\"get\">");
				    out.println("channelName: <input type=\"text\" name=\"name\"><br/>");
				    out.println("xpath: <input type=\"text\" name=\"xpath\"><br/>");
				    out.println("<input type=\"submit\" value=\"create\"></form>");				
					out.println("<p>logout here!</p></body>");
					out.println("<a href=\"/logout\">logout</a>");
					out.println("</html>");
				}
			}
			//if the client is not logged in
			else{
				//if the URI is /login or /xpath, send the login page as well as the register option to the client
				//then take a post with given parameters to /login
				if(URI.equals("/create")||URI.equals("/delete")||URI.equals("/subscribe")||URI.equals("/unsubscribe")||URI.equals("/show")){
					response.setContentType("text/html");
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					PrintWriter out = response.getWriter();
					out.println("<html><head>");
					out.println("<title>Xpath Servlet</title>");
					out.println("<body><h1>Error 401</h1>");
					out.println("<p>Unauthorized!</p></body>");
					out.println("</html>");
				}else if(URI.equals("/login")||URI.equals("/xpath")) {
					response.setContentType("text/html");
				    request.getSession(true);			    
					try {
						PrintWriter out = response.getWriter();
					    out.println("<html><head><title>Login</title></head><body>");	
					    out.println("<h1>Login</h1>");
					    out.println("<form method=\"post\">");
					    out.println("Username: <input type=\"text\" name=\"username\"><br/>");
					    out.println("Password: <input type=\"text\" name=\"password\"><br/>");
					    out.println("<input type=\"submit\" value=\"Login\"></form>");
					    out.println("<p>Don't have an account? Register one here!</p>");
					    out.println("<a href=\"/register\">Register</a>");
					    out.println("</body></html>");
					} catch (IOException e) {
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					}
				//if the URI is /register, send the register page to the client, and send a post with given parameter to /register	
				}else if(URI.equals("/register")) {
					response.setContentType("text/html");
				    request.getSession(true);
					try {
						PrintWriter out = response.getWriter();
					    out.println("<html><head><title>Register</title></head><body>");	
					    out.println("<h1>Register</h1>");
					    out.println("<form method=\"post\">");
					    out.println("Username: <input type=\"text\" name=\"username\"><br/>");
					    out.println("Password: <input type=\"text\" name=\"password\"><br/>");
					    out.println("<input type=\"submit\" value=\"Register\"></form>");
					    out.println("</body></html>");
					} catch (IOException e) {
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					}
					catch (Exception e) {
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					}
				}
			}
		}
		db.close();
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
    {
		//determine the post response by URI
		String URI = request.getRequestURI();
		//if the URI is /login or /xpath
	    if(URI.equals("/login")||URI.equals("/xpath")) {
			request.getSession(true);
		    try {
		    	//open the database and get/process the parameters,username and password
		    	DBWrapper db = new DBWrapper(getServletContext().getInitParameter("BDBstore"));
			    String username = request.getParameter("username").trim();
			    //hash the input password with SHA256 hashing to get compared with the password in database
			    MessageDigest digest = db.getDigest();
			    byte [] password = digest.digest(request.getParameter("password").trim().getBytes(StandardCharsets.UTF_8));
			    if (username == null || password == null) {
			    		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			    		return;
			    }
				
			    //get the hashing password from the database corresponding to the given username
				Transaction transaction = db.getTransaction();
				User user = db.getUser(username);	
				transaction.commit();
				
				//compare the input password and the password stored in the database byte by byte
				boolean Success = true;
				if (user==null){
					PrintWriter out = response.getWriter();
					out.println("<html><head><title>Login</title></head><body>");	
				    out.println("<h3>Login Unsuccessful!</h3>");
			    	out.println("<p>Your username does not exist. Please try again or register for a new account</p>");
			    	out.println("<a href=\"/login\">Login</a><br />");
			    	out.println("<a href=\"/register\">Register</a>");
					out.println("</body></html>");
				}else{
					for (int i=0; i<password.length;i++){
						if(password[i]!=user.getPassword()[i])
							Success = false;
					}
					//if the two password are not identical
					if(!Success){
						PrintWriter out = response.getWriter();
						out.println("<html><head><title>Login</title></head><body>");	
					    out.println("<h3>Login Unsuccessful!</h3>");
				    	out.println("<p>Your password is wrong. Please try again or register for a new account</p>");
				    	out.println("<a href=\"/login\">Login</a><br />");
				    	out.println("<a href=\"/register\">Register</a>");
						out.println("</body></html>");
					//if the two password are identical, set a new cookie for the client and redirect the client to the main page
					} else {
						Cookie login = new Cookie("cis455", username);
						login.setMaxAge(300); 
						response.addCookie(login);
						response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
						response.setHeader("Location", "/xpath");
						response.sendRedirect("/xpath");
					}
				}
				
			} catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		  
		//if the URI is /register
		}else if(URI.equals("/register")) {
			request.getSession(true);
		    try {
		    	//open the database and get the parameters from the request
		    	DBWrapper db = new DBWrapper(getServletContext().getInitParameter("BDBstore"));	
			    String username = request.getParameter("username").trim();
			    String password = request.getParameter("password").trim();
			    if (username == null || password == null) {
			    		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			    		return;
			    }
				//insert the new username/password pair into the database
				Transaction t = db.getTransaction();
				boolean Success = db.insertUser(username, password);
				t.commit();

				//if the result of insertion is unsuccessful, it is because the registering username is already in the database 
				if(!Success) {
					PrintWriter out = response.getWriter();
				    out.println("<html><head><title>Register</title></head><body>");	
				    out.println("<h3>Register Unsuccessful!</h3>");
				    	out.println("<p>That account name already exists, please try again.</p>");
				    	out.println("<a href=\"/register\">Register</a>");
				    out.println("</body></html>");
				//if insert successfully, redirect the client to the main page/ lgoin page
				} else if(Success){				
					response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
					response.setHeader("Location", "/login");
					response.sendRedirect("/login");
				}
			} catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
	}
}









