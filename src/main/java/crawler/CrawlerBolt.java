import java.io.IOException;  
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Map; 
import java.util.UUID;
import stormlite.OutputFieldsDeclarer;
import stormlite.TopologyContext;
import stormlite.bolt.IRichBolt;
import stormlite.bolt.OutputCollector;
import stormlite.routers.IStreamRouter;
import stormlite.tuple.Fields;
import stormlite.tuple.Tuple;
import stormlite.tuple.Values;
import crawler.info.HttpClient;
import crawler.info.RobotsTxtInfo;
import crawler.info.URLInfo;
import storage.Doc;


public class CrawlerBolt implements IRichBolt{
	private String executorId = UUID.randomUUID().toString();
	private Fields schema = new Fields("Doc", "unModified");
	private OutputCollector collector;
	
	public CrawlerBolt(){}
	
	@Override
	public String getExecutorId() {
		return this.executorId;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(schema);
		
	}

	@Override
	public void cleanup() {}

	@Override
	public void execute(Tuple input) throws Exception{
		String URL = input.getStringByField("URL");
		if (XPathCrawler.maxNumOfFiles == -1 || XPathCrawler.numAchieved < XPathCrawler.maxNumOfFiles) {
			
			if (!URL.startsWith("http"))
				URL = "http://"+URL;
			URLInfo urlInfo = new URLInfo(URL);
			String fullURL = urlInfo.getFullURL();
			
			if (XPathCrawler.crawledURL.contains(fullURL)){
				return;
			}		
			else 
				XPathCrawler.crawledURL.add(fullURL);

			//send a UDP packet to port 10455 on the monitoring host
			byte[] data = ("xiaonany;"+fullURL).getBytes();
			DatagramPacket packet = new DatagramPacket(data, data.length,XPathCrawler.monitorHost,10455);
			try {
				DatagramSocket s = new DatagramSocket();
				s.send(packet);
				s.close();
			} catch (IOException e1) {}
			
			String rootURL = urlInfo.getRootURL();
			HttpClient Client = new HttpClient();
			//get the robot.txt from the websites, if the robot.txt for the web is cached before, get it from the cache; else add it to the cache
			RobotsTxtInfo info = null;
			if (XPathCrawler.robotsMap.containsKey(rootURL)) 
				info = XPathCrawler.robotsMap.get(rootURL);
			else { 
				info = Client.parseRobotTxt(rootURL);	
				XPathCrawler.robotsMap.put(rootURL, info);
			}						
			//parse the robot.txt
			int crawlDelay = 0; 
			if (info != null) { 
				ArrayList<String> DisallowedLinks = null;
				ArrayList<String> AllowedLinks = null;
				Integer crawlDelayTime = null;
				//get disallowedLiknks and allowedLinkes
				if (info.containsUserAgent("cis455crawler")) {
					DisallowedLinks = info.getDisallowedLinks("cis455crawler");
					AllowedLinks = info.getAllowedLinks("cis455crawler");
					crawlDelayTime = info.getCrawlDelay("cis455crawler");
				}
				else if (info.containsUserAgent("*")) { 
					DisallowedLinks = info.getDisallowedLinks("*");
					AllowedLinks = info.getAllowedLinks("*");
					crawlDelayTime = info.getCrawlDelay("*");
				}
				
				String DisallowedLink = "";
				String AllowedLink = "";
				//determine if the URL we are crawling now is disallowed
				if (DisallowedLinks != null) {
					for (String s : DisallowedLinks){
						if (urlInfo.getFilePath().startsWith(s) && (s.length() > DisallowedLink.length()))
							DisallowedLink = s;
					}
				}
				if (AllowedLinks != null) {
					for (String s : AllowedLinks){
						if (urlInfo.getFilePath().startsWith(s) && (s.length() > AllowedLink.length()))
							AllowedLink = s;
					}
				}
				//if is disallowed, print access denied message and pass this URL
				if (AllowedLink.length() < DisallowedLink.length()) {
					System.out.println(fullURL+": Disallowed: Robots.txt denied access");
					return;
				}
				//if the crawlDelay in the robot.txt is not null, implement a delay	
				if (crawlDelayTime != null)
					crawlDelay = crawlDelayTime.intValue();				
				try {
					if (crawlDelay > 0 && XPathCrawler.previousHost.equals(urlInfo.getHostName()))
					Thread.sleep(crawlDelay*1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				XPathCrawler.previousHost = urlInfo.getHostName();
			}
			//If the URL is allowed for this URL, start downloading
			//get doc from the database with the URL first
			Doc doc = XPathCrawler.db.getDoc(fullURL);
			//if the doc does not exit in the data base, send and parse request to get the document
			if(URL!=null&&doc==null) {
				try {
					Client.sendHeadRequest(fullURL, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
				Client.parseHeadResponse(true);
			//else, send parse the request with a If-Modified-Since header
			}else if(URL!=null&&doc!=null){
				try {
					Client.sendHeadRequest(fullURL,doc.getLastCrawled());
				} catch (Exception e) {
					e.printStackTrace();
				}
				Client.parseHeadResponse(true);
			}else {
				return;
			}
			//whatever the response is, since we have send a head request, the number of files crawled make a increment
			XPathCrawler.numAchieved += 1;
			//if the response status is 304, print Not modified message
			if(Client.getStatus()==304) {
				System.out.println(fullURL + ": Not Modified!");
				collector.emit(new Values<Object>(doc,true));
			}
			else { 
				//if the response status is 301, which means redirecting
				while (Client.getStatus()==301){
					//resend request to the redirecting location and parse the response in the same way
					//until there is no redirection
					String location = Client.getRedirectLocation();
					try {
						Client.sendHeadRequest(location, null);
					} catch (Exception e) {
						e.printStackTrace();
					}
					Client.parseHeadResponse(true);
					if (Client.getStatus() == 200)
						fullURL = new URLInfo(location).getFullURL();
				}
				//send get request and parse the response
				try {
					Client.sendGetRequest(fullURL);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				Client.parseGetResponse();
				if (Client.getContentString()==null){
					System.out.println(fullURL+": DownloadFailed");
					return;
				}else if(Client.getContentLength() > XPathCrawler.maxSizeOfDoc){
					System.out.println(fullURL+": File too big");
					return;
				}
				doc = new Doc(fullURL,Client.getContentString(),Client.getContentType(),Client.getContentLength());
				collector.emit(new Values<Object>(doc,false));
			}
		}
		System.out.println(XPathCrawler.numAchieved);
	}

	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context,
			OutputCollector collector) {
		this.collector = collector;
		
	}

	@Override
	public void setRouter(IStreamRouter router) {
		this.collector.setRouter(router);
		
	}

	@Override
	public Fields getSchema() {
		return this.schema;
	}

	
}
