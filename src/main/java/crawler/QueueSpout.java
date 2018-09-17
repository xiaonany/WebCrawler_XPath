import java.util.Map; 
import java.util.UUID;

import org.apache.log4j.Logger;


import stormlite.OutputFieldsDeclarer;
import stormlite.TopologyContext;
import stormlite.routers.IStreamRouter;
import stormlite.spout.IRichSpout;
import stormlite.spout.SpoutOutputCollector;
import stormlite.tuple.Fields;
import stormlite.tuple.Values;

public class QueueSpout implements IRichSpout{
	static Logger log = Logger.getLogger(QueueSpout.class);
	String executorId = UUID.randomUUID().toString();
	SpoutOutputCollector collector;
	
	public QueueSpout(){
		log.debug("Starting spout");
	}
	
	@Override
 	public void nextTuple() {
		//each time the nextTuple method is executed, get a URL from the frontierQueue
    	if (XPathCrawler.frontierQueue != null && !XPathCrawler.frontierQueue.isEmpty()) {
    		String URL = XPathCrawler.frontierQueue.remove();
    		this.collector.emit(new Values<Object>(URL));
    	}
        Thread.yield();
    }

	@Override
	public String getExecutorId() {
		return executorId;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("URL"));
	}

	@Override
	public void open(Map<String, String> config, TopologyContext topo,
			SpoutOutputCollector collector) {
		this.collector = collector;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRouter(IStreamRouter router) {
		this.collector.setRouter(router);
	}
	
	public void addToQueue(String URL){
		XPathCrawler.frontierQueue.add(URL);
	}
}
