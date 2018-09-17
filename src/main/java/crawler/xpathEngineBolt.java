import java.io.ByteArrayInputStream;  
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.tidy.Tidy;
import org.xml.sax.SAXException;
import stormlite.OutputFieldsDeclarer;
import stormlite.TopologyContext;
import stormlite.bolt.IRichBolt;
import stormlite.bolt.OutputCollector;
import stormlite.routers.IStreamRouter;
import stormlite.tuple.Fields;
import stormlite.tuple.Tuple;
import stormlite.tuple.Values;
import storage.Channel;
import storage.Doc;
import xpathengine.XPathEngineImpl;
import org.w3c.dom.Document;

public class xpathEngineBolt implements IRichBolt{
	private String executorId = UUID.randomUUID().toString();
	private Fields schema = new Fields("Doc", "unModified");
	private OutputCollector collector;
	
	public xpathEngineBolt() {}
	
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
	public void execute(Tuple input) throws ParserConfigurationException, SAXException, IOException {
		//get document and another argument from crawlerBolt
		Doc doc = (Doc) input.getObjectByField("Doc");
		Boolean unModified = (Boolean) input.getObjectByField("unModified");
		//if not modified, then the channel is also not needed for change
		if (!unModified){
			//get org.w3c.dom.document argument accorrding to document type
			Document d = null;
			String type = doc.getcontentType();
			if (type.equals("text/html")){
				Tidy tidy = new Tidy();
				tidy.setMakeBare(true);
				tidy.setShowWarnings(false);
				tidy.setXHTML(true);
				d = tidy.parseDOM(new ByteArrayInputStream(doc.getcontentString().getBytes()) , null);
			}else if (type.endsWith("+xml") || type.equals("text/xml") || type.equals("application/xml")){
				DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
				DocumentBuilder buider = bf.newDocumentBuilder();
				System.out.println(doc.getUrl());
				d = buider.parse(new ByteArrayInputStream(doc.getcontentString().getBytes()));
			}
			//get all xpaths in all channels
			ArrayList<Channel> channels= XPathCrawler.db.getAllChannels();
			String[] xpaths = new String[channels.size()];
			for (int i = 0; i<channels.size(); i++){
				xpaths[i] = channels.get(i).getXPaths().trim();
			}
			//parse the document and make change to channels in the database
			XPathEngineImpl engine = new XPathEngineImpl();
			engine.setXPaths(xpaths);
			boolean[] results = engine.evaluate(d);
			for (int i=0; i<results.length;i++){
				System.out.println(results[i]);
				ArrayList<String> urls = channels.get(i).getUrls();
				if (results[i]){
					if (!urls.contains(doc.getUrl())) urls.add(doc.getUrl());
					channels.get(i).setUrls(urls);
					XPathCrawler.db.insertChannel(channels.get(i));
				}else{
					if (urls.contains(doc.getUrl())) urls.remove(doc.getUrl());
				}
			}
		}
		this.collector.emit(new Values<Object>(doc,unModified));
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
