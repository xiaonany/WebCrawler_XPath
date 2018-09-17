package edu.upenn.cis455.storage;

import java.util.ArrayList;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;

@Entity
public class Channel {
	//primary key of channel object
	@PrimaryKey
	private String channelName;
	
	@SecondaryKey(relate = Relationship.MANY_TO_ONE)
	private String owner;
	
	private String xpath;
		
	private ArrayList<String> urls;
	
	private ArrayList<String> subscribers;
	
	public Channel() {
	}
	
	public Channel (String channelName, String xpath, String owner) throws Exception {
		this.channelName = channelName;
		this.xpath = xpath;
		this.owner = owner;
		this.urls = new ArrayList<String>();
		this.subscribers = new ArrayList<String>();
	}
	
	public String getChannelName() {
		return channelName;
	}
	
	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getXPaths() {
		return this.xpath;
	}

	public void setXPaths(String xpath) {
		this.xpath = xpath;
	}

	public String getowner() {
		return owner;
	}

	public void setowner(String owner) {
		this.owner = owner;
	}

	public ArrayList<String> getUrls() {
		return urls;
	}

	public void setUrls(ArrayList<String> urls) {
		this.urls = urls;
	}
	
	//check whether the given user subscribed the channel
	public boolean isSubscriber(String name){
		if (subscribers.contains(name)){
			return true;
		}else{
			return false;
		}
	}
	
	//add subscriber
	public void addSubscriber(String name){
		this.subscribers.add(name);
	}
	
	//delete subscriber
	public void deleteSubscriber(String name){
		ArrayList<String> subscribers1 = new ArrayList<String>();
		for (String s:subscribers){
			if (!s.equals(name)){
				subscribers1.add(s);
			}
		}
		this.subscribers = subscribers1;
	}
	
}
