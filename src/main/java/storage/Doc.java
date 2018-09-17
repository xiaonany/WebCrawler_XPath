package edu.upenn.cis455.storage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date; 

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

import crawler.info.HttpClient;

//contentStringument Entity with information including url,contentStringString,contentType,contentLength and the time of last crawled
@Entity
public class Doc {
	@PrimaryKey
	private String url;
	private String contentString;
	private String contentType;
	private long contentLength;
	private Date lastCrawled;
	
	public Doc() {}
	
	public Doc(String url, String contentString, String contentType, long contentLength) {
		this.url = url; 
		this.contentString = contentString;
		this.contentType = contentType;
		this.contentLength = contentLength;
		this.lastCrawled = new Date();
	}
	
	//get primary key of the entity: url
	public String getUrl() { 
		return this.url; 
	}
	//get contentStringument type
	public String getcontentType() { 
		return this.contentType; 
	}
	//get contentStringument length
	public long getcontentLength() { 
		return this.contentLength; 
	}
	//get contentStringument body 
	public String getcontentString() { 
		return this.contentString.substring(this.contentString.indexOf('>') + 1); 
	}
	//get the time of last crawl
	public Date getLastCrawled() { 
		return this.lastCrawled; 
	}
	//return the contentStringument object of the contentString
	public Document getDoc() throws MalformedURLException{
		URL URL = new URL(url);
		return Jsoup.parse(contentString, HttpClient.getBaseURL(URL));
		
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	public void setDoc(String contentString, String contentType) {
		this.contentString = contentString;
		this.contentType = contentType;
	}
	public void setLastCrawled() {
		this.lastCrawled = new Date();
	}
}