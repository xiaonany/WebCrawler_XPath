import java.util.Map; 
import java.util.UUID;
import stormlite.OutputFieldsDeclarer;
import stormlite.TopologyContext;
import stormlite.bolt.IRichBolt;
import stormlite.bolt.OutputCollector;
import stormlite.routers.IStreamRouter;
import stormlite.tuple.Fields;
import stormlite.tuple.Tuple;



public class URLFilterBolt implements IRichBolt{
	String executorId = UUID.randomUUID().toString();
	Fields schema = new Fields();
	private OutputCollector collector;
	
	public URLFilterBolt (){}
	
	@Override
	public String getExecutorId() {
		return this.executorId;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(schema);
	}

	@Override
	public void cleanup() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void execute(Tuple input) {
		String URL = input.getStringByField("URL");
		//put urls not in the frontierQueue into it
		if (!XPathCrawler.frontierQueue.contains(URL)){
			XPathCrawler.frontierQueue.add(URL);
		}
		
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
