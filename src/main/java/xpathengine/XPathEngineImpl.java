import java.io.InputStream;    
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.DefaultHandler;

public class XPathEngineImpl implements XPathEngine {

	private String xPaths[] = null;
	public XPathEngineImpl() {}
	
	public void setXPaths(String[] s) {
		this.xPaths = s;
	}
	
	public boolean isValid(int i) {
		if (xPaths != null) {
			String path = xPaths[i];
			System.out.println(path);
			if (path.startsWith("/")) {
				String[] steps = tokenizer(path);
				for (String step : steps) {			
					if (!isValidStep(step)){
						return false;
					}
				}
				return true; 
			}
			else
				return false;
		}
		else {
			return false;
		}
	}
	
	private String[] tokenizer(String path) {		
		ArrayList<String> tokens = new ArrayList<String>();
		StringBuilder step = new StringBuilder();	
		int bracketNum = 0;
		boolean inQuote = false;
		for (int i=0;i<path.length();i++) {
			char curtchar = path.toCharArray()[i];
			char prechar = ' ';
			if (i>0) prechar = path.toCharArray()[i-1];
			if(curtchar!='/'){
				if (Character.isWhitespace(curtchar)&&inQuote)
					step.append(curtchar);
				else if (!Character.isWhitespace(curtchar)){
					step.append(curtchar);
				}
				
				if (curtchar == '"' && prechar != '\\'){
					if (inQuote)  inQuote = false;
					else inQuote = true;		
				}
				
				if (curtchar=='[')
					bracketNum++;
				if (curtchar==']')
					bracketNum--;
			}else{
				if (inQuote)
					step.append(curtchar);
				else{
					if (bracketNum == 0 && step.length()!=0){
						tokens.add(step.toString().trim());
						step.delete(0, step.length());
					}else if (bracketNum != 0){
						step.append(curtchar);
					}
				}
			}
		}
		tokens.add(step.toString().trim());
		return tokens.toArray(new String[tokens.size()]);
	}
	
	private ArrayList<String> getTests(String step){
		ArrayList<String> tests = new ArrayList<>();
		StringBuilder test = new StringBuilder();
		if (step.indexOf("[")!=-1){
			int bracketNum = 0;
			for (char c:step.substring(step.indexOf("[")).toCharArray()){
				if (c=='['){
					if (bracketNum>0)
						test.append(c);
					bracketNum ++;
				}else if (c==']'){
					bracketNum--;			
					if (bracketNum == 0){
						tests.add(test.toString().trim());
						test.delete(0, test.length());
					}else
						test.append(c);
				}else
					test.append(c);			
			}
		}
		return tests;
	}
	
	private String getTestCase(String test){
		String type = null;
		if (test.startsWith("text()=\"")&&test.endsWith("\""))
			type = "text";
		else if (test.startsWith("contains(text(),\"")&&test.endsWith("\")"))
			type = "contains";
		else if(test.startsWith("@")&&test.endsWith("\"")){
			if (isValidIdentifier(test.substring(1,test.indexOf("=\""))))
				type = "att";
		}
		else{
			String[] steps = tokenizer(test);
			boolean valid = true;
			for (String step:steps){
				if(!isValidStep(step))
					valid = false;
			}
			if (valid) type = "step";			
		}		
		return type;
	}
	
	private boolean isValidIdentifier(String identifier){
		char first = identifier.toCharArray()[0];
		if (!Character.isAlphabetic(first)&&!(first=='_'))
			return false;
		if (identifier.toLowerCase().startsWith("xml"))
			return false;
		for (char c:identifier.toCharArray()){
			if (Character.isWhitespace(c))
				return false;
			else if (!Character.isAlphabetic(c)&&!Character.isDigit(c)&&!(c=='_')&&!(c=='-')&&!(c=='.'))
				return false;
		}
		return true;
	}
	
	private boolean isValidStep(String step) {			
		//check that everything in each step is okay with our grammar
		int bracketIndex = step.indexOf('[');
		if ( bracketIndex == -1) { //no brackets, only a node name
			if (!isValidIdentifier(step)) //check that matches proper syntax for nodename
				return false;
		}else { //check that part before matches proper syntax for nodename
			String nodename = step.substring(0, bracketIndex).trim();
			if (!isValidIdentifier(nodename))
				return false;
			else { //evaluate each test
				ArrayList<String> tests = new ArrayList<String>();
				tests = getTests(step);
				for (String test : tests) {
					if (getTestCase(test)==null)
						return false;
					else if (getTestCase(test).equals("text")||getTestCase(test).equals("contains")||getTestCase(test).equals("att"))
						continue;
					else if (getTestCase(test).equals("step")){
						String[] stepsInsides = tokenizer(test);
						//for each step, check that it is good
						for (String s : stepsInsides) {
							//check that everything in each step is okay with our grammar
							if (!isValidStep(s))
								return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	public boolean[] evaluate(Document d) {
		boolean[] evaluate = new boolean[xPaths.length];
		if (xPaths != null) {
			//check if each xpath is in the document
			for (int i = 0; i < xPaths.length; i++) {
				if (isValid(i)) { //only care about valid xpaths, leave false otherwise					
					//first split xpath into steps	
					String xPath = xPaths[i];
					
					boolean isHtml = false;
					if (d.getDoctype()!=null && d.getDoctype().getName().trim().startsWith("html"))
						isHtml = true;
					
					String steps[] =  tokenizer(xPath);

					Node node = d.getDocumentElement();

					if (match(node,steps,true,isHtml))
						evaluate[i] = true;
					else
						evaluate[i] = false;
				} else 
					evaluate[i] = false;
			}
			return evaluate;
		}
		else {
			return null;
		}
	}
	
	public boolean match(Node node, String[] steps, boolean firstTime, boolean isHtml){
		String curtStep = null;
		String[] newSteps = null;
		if (steps!=null){
			curtStep = steps[0];
			if(steps.length>1){
				newSteps = new String[steps.length-1];
				for (int i = 1; i < steps.length; i++){
					newSteps[i-1] = steps[i];
				}
			}
		}
		while (curtStep!=null){
			String nodeName = "";
			ArrayList<String> tests = getTests(curtStep);
			if (tests.size()>0)
				nodeName = curtStep.substring(0,curtStep.indexOf("["));
			else
				nodeName = curtStep;
			
			NodeList lst = node.getChildNodes();
			for (int i = 0; i < lst.getLength(); i++){
				Node n = lst.item(i);
				if (node.getNodeName().equals(nodeName) && firstTime){
					if(tests.size()>0){
						boolean testMatch = true;
						for (String test:tests){
							if (getTestCase(test).equals("text")){
								String txt = test.substring(test.indexOf("\"")+1,test.lastIndexOf("\""));
								if (isHtml&&!node.getFirstChild().getNodeValue().equals(txt))
									testMatch = false;
								if (!isHtml&&!node.getTextContent().equals(txt))
									testMatch = false;									
							}else if(getTestCase(test).equals("contains")){
								String txt = test.substring(test.indexOf("\"")+1,test.lastIndexOf("\""));
								if (isHtml&&!node.getFirstChild().getNodeValue().contains(txt))
									testMatch = false;
								if (!isHtml&&!node.getTextContent().contains(txt))
									testMatch = false;				
							}else if(getTestCase(test).equals("att")){
								String key = test.substring(test.indexOf("@")+1,test.indexOf("=")).trim();
								String value = test.substring(test.indexOf("\"")+1,test.lastIndexOf("\""));										
								if(!node.getAttributes().getNamedItem(key).equals(value))
									testMatch = false;
							}else if(getTestCase(test).equals("step")){
								if(!match(node,tokenizer(test),false,isHtml))
									testMatch = false;
							}else{
								testMatch = false;
							}
						}
						if (testMatch && match(node,newSteps,false,isHtml)) return true;
					}else
						if (match(node,newSteps,false,isHtml)) return true;
				}else if(n.getParentNode().equals(node)&&n.getNodeName().equals(nodeName)){
					if(tests.size()>0){
						boolean testMatch = true;
						for (String test:tests){
							if (getTestCase(test).equals("text")){
								String txt = test.substring(test.indexOf("\"")+1,test.lastIndexOf("\""));
								if (isHtml&&!n.getFirstChild().getNodeValue().equals(txt))
									testMatch = false;
								if (!isHtml&&!n.getTextContent().equals(txt))
									testMatch = false;									
							}else if(getTestCase(test).equals("contains")){
								String txt = test.substring(test.indexOf("\"")+1,test.lastIndexOf("\""));
								if (isHtml&&!n.getFirstChild().getNodeValue().contains(txt))
									testMatch = false;
								if (!isHtml&&!n.getTextContent().contains(txt))
									testMatch = false;		
							}else if(getTestCase(test).equals("att")){
								String key = test.substring(test.indexOf("@")+1,test.indexOf("=")).trim();
								String value = test.substring(test.indexOf("\"")+1,test.lastIndexOf("\""));
								if(!n.getAttributes().getNamedItem(key).equals(value))
									testMatch = false;
							}else if(getTestCase(test).equals("step")){
								if(!match(n,tokenizer(test),false,isHtml))
									testMatch = false;
							}else{
								testMatch = false;
							}
						}
						if (testMatch && match(n,newSteps,false,isHtml)) return true;
					}else
						if (match(n,newSteps,false,isHtml)) return true;
				}
			}
			return false;
		}
		return true;
	}
	
	@Override
	public boolean isSAX() {
		return false;
	}

	@Override
	public boolean[] evaluateSAX(InputStream document, DefaultHandler handler) {
		return null;
	}
	        
}
