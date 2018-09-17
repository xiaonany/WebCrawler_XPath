import java.net.InetAddress; 
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import stormlite.Config;
import stormlite.LocalCluster;
import stormlite.Topology;
import stormlite.TopologyBuilder;
import crawler.info.RobotsTxtInfo;
import storage.DBWrapper;

public class XPathCrawler {
	static Logger log = Logger.getLogger(XPathCrawler.class);

	private static final String QUEUE_SPOUT = "QUEUE_SPOUT";
    private static final String CRAWLER_BOLT = "CRAWLER_BOLT";
    private static final String XPATHENGINE_BOLT = "XPATHENGINE_BOLT";
    private static final String DOCPARSER_BOLT = "DOCPARSER_BOLT";
    private static final String URLFILTER_BOLT = "URLFILTER_BOLT";
    private static boolean complete = false;
    
    //for crawlerBolt
	static HashSet<String> crawledURL = new HashSet<String>(); 
	static HashMap<String, RobotsTxtInfo> robotsMap = new HashMap<String, RobotsTxtInfo>();
	static int numAchieved = 0;
	static String previousHost = "";
    
	
	//for spout and filter bolt
    static Queue<String> frontierQueue = new LinkedList<String>();
    
	static LinkedList<String> ExtractedURLs = new LinkedList<String>();
    
	static int maxNumOfFiles = 1000;
	static String firstURL = null;
	static String BDBEnvirDir = null;
	static int maxSizeOfDoc = 0;
	static InetAddress monitorHost = null;
	static DBWrapper db;
    
    public static void main(String[] args) throws Exception {
    	System.setProperty("java.net.preferIPv4Stack","true");
		//set the default monitor
		try {
			monitorHost = InetAddress.getByName("cis455.cis.upenn.edu");
		} catch (UnknownHostException e1) {}

		//read in arguments
		if(args.length >= 3){
			firstURL = new String(args[0]);
			frontierQueue.add(firstURL);
			BDBEnvirDir = new String(args[1]);
			db = new DBWrapper(BDBEnvirDir);
			maxSizeOfDoc = Integer.parseInt(args[2])*1024;

			//if there are 4 arguments, read the fourth as the max number of files
			if(args.length == 4){
				maxNumOfFiles = Integer.parseInt(args[3]);
			//if there are 5 arguments, read the fifth as the monitor host name
			}else if (args.length == 5){
				maxNumOfFiles = Integer.parseInt(args[3]);
				monitorHost = InetAddress.getByName(args[4]);
			}
			

			//set up stormLite
			Config config = new Config();
	        QueueSpout spout = new QueueSpout();
	        CrawlerBolt crawler = new CrawlerBolt();
	        xpathEngineBolt xpathEngine = new xpathEngineBolt();
	        DocParserBolt parser = new DocParserBolt();
	        URLFilterBolt filter = new URLFilterBolt();
	        
	        
	        TopologyBuilder builder = new TopologyBuilder();

	        builder.setSpout(QUEUE_SPOUT, spout, 1);
	        
	        builder.setBolt(CRAWLER_BOLT, crawler, 1).shuffleGrouping(QUEUE_SPOUT);
	        
	        builder.setBolt(XPATHENGINE_BOLT, xpathEngine, 1).shuffleGrouping(CRAWLER_BOLT);
	        
	        builder.setBolt(DOCPARSER_BOLT, parser, 1).shuffleGrouping(XPATHENGINE_BOLT);
	        
	        builder.setBolt(URLFILTER_BOLT, filter, 1).shuffleGrouping(DOCPARSER_BOLT);

	        LocalCluster cluster = new LocalCluster();
	        Topology topo = builder.createTopology();

	        ObjectMapper mapper = new ObjectMapper();

			try {
				String str = mapper.writeValueAsString(topo);
				
				System.out.println("The StormLite topology is:\n" + str);
			} catch (JsonProcessingException e) {
			}

	        cluster.submitTopology("XPathCrawler", config, 
	        		builder.createTopology());

	        Thread.sleep(3000);
	        while (!complete){
	        	if (numAchieved>maxNumOfFiles)
	        		complete = true;
	        }
	        cluster.killTopology("XPathCrawler");
	        cluster.shutdown();
	        System.exit(0);	
	        db.close();
		}
		//if there are no more than 3 arguments, print SEAS login and exit
		else{
			System.out.println("XIAONAN YANG(xiaonany)");
			System.exit(-1);
		}
    } 
}
