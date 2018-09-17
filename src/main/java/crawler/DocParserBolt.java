import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;  
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import stormlite.OutputFieldsDeclarer;
import stormlite.TopologyContext;
import stormlite.bolt.IRichBolt;
import stormlite.bolt.OutputCollector;
import stormlite.routers.IStreamRouter;
import stormlite.tuple.Fields;
import stormlite.tuple.Tuple;
import storage.Doc;


public class DocParserBolt implements IRichBolt{
	private String executorId = UUID.randomUUID().toString();
	private Fields schema = new Fields();
	private OutputCollector collector;
	private ArrayList<String> lst;
	HttpURLConnection conn;
	String MasterAddr;
	int MasterPort;
	
	public DocParserBolt(){}
	
	public DocParserBolt(String masterAddr, int masterPort){
		this.MasterAddr = masterAddr;
		this.MasterPort = masterPort;
	}
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
	public void execute(Tuple input) {
		//get doc object from the xpathEngineBolt
		Doc doc = (Doc) input.getObjectByField("Doc");
		//get jsoup.document object from the doc object for extracting urls
		Document d = null;
		try {
			d = doc.getDoc();
		} catch (MalformedURLException e) {
			e.printStackTrace(); 
		}
		
		if (doc.getcontentType()!=null && doc.getcontentType().equals("text/html")){
			Elements links = d.select("a");
			String URLString = "http://" + MasterAddr + ":" + MasterPort + "/add_url";
			//make conncetion to the master get send the URL out
			try{
				URL url = new URL(URLString);				
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("POST");
				PrintWriter out = new PrintWriter(new OutputStreamWriter(conn.getOutputStream()));
				String outContent = "";
				for (Element e:links){
					String link = e.attr("abs:href");
					outContent+= link+"\n";
					lst.add(link);
				}
				out.write(outContent);
				out.flush();
				conn.getResponseCode();
				conn.disconnect();
			}catch(IOException e){
				e.printStackTrace();
			}
		}	
	}

	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context,
			OutputCollector collector) {
		this.collector = collector;	
		lst = new ArrayList<>();
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
