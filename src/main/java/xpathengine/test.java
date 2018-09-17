import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

import crawler.info.HttpClient;

public class test {
	public static void main(String[] args) throws Exception{
		HttpClient Client = new HttpClient();
		String url = "http://crawltest.cis.upenn.edu:80/nytimes/Africa.xml";
		Client.sendGetRequest(url);
		Client.parseGetResponse();
		
		Document d = null;
		String type = Client.getContentType();
		if (type.equals("text/html")){
			Tidy tidy = new Tidy();
			tidy.setMakeBare(true);
			tidy.setShowWarnings(false);
			tidy.setXHTML(true);
			d = tidy.parseDOM(new ByteArrayInputStream(Client.getContentString().getBytes()) , null);
		}else if (type.endsWith("+xml") || type.equals("text/xml") || type.equals("application/xml")){
			DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
			DocumentBuilder buider = bf.newDocumentBuilder();
			d = buider.parse(new ByteArrayInputStream(Client.getContentString().getBytes()));
		}
		
		XPathEngineImpl x = new XPathEngineImpl();
		
		String[] paths = new String[]{
//			"/channel/description",
//			"/channel/image/url",
//			"/image/url",
//			"/channel[image]",	
//			"/channel/image/url[contains(text(),\"http://graphics.nytimes.com/images/section/NytSectionHeader.gif\")]",
//			"/channel/image/url[text()  =  \"http://graphics.nytimes.com/images/section/NytSectionHeader.gif\"]",
//			"/channel[image/url]",
//			"/channel/image[url[text()=\"http://graphics.nytimes.com/images/section/NytSectionHeader.gif\"]][title]",
//			"/head/title[text()=\"CSE455/CIS555 HW2 Sample Data\"]",
//			"/html/head/title[text()=\"CSE455/CIS555 HW2 Sample Data\"]",
//			"/body/h3[contains(text(),\"RSS Feeds\")]",
//			"/body/h3[text()=\"RSS Feeds\"]",
//			"/body/h3[text()=\"Marie's XML data\"]",
//			"/body/p[contains(text(),\"The HTML pages do not contain external links, so you shouldn't\")]"
			"/channel/image/url[contains(text(),\"http://graphics.nytimes.com/images/section/NytSectionHeader.gif\")]"
		};
		x.setXPaths(paths);
		boolean[] results = x.evaluate(d);
		for(boolean b:results)
			System.out.println(b);
	}
}
