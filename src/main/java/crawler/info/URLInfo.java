public class URLInfo {
	private String hostName;
	private int portNo;
	private String filePath;
	private String scheme;
	
	/**
	 * Constructor called with raw URL as input - parses URL to obtain host name and file path
	 */
	public URLInfo(String docURL){
		if(docURL == null || docURL.equals(""))
			return;
		docURL = docURL.trim();
		if(!docURL.startsWith("http://") || docURL.length() < 8)
			if(!docURL.startsWith("https://") || docURL.length() < 9)
				return;
		// Stripping off 'http://'
		if(docURL.startsWith("http://")){
			this.scheme = "http";
			docURL = docURL.substring(7);
		}
		else{
			this.scheme = "https";
			docURL = docURL.substring(8);
		}
		/*If starting with 'www.' , stripping that off too
		if(docURL.startsWith("www."))
			docURL = docURL.substring(4);*/
		int i = 0;
		while(i < docURL.length()){
			char c = docURL.charAt(i);
			if(c == '/')
				break;
			i++;
		}
		String address = docURL.substring(0,i);
		if(i == docURL.length())
			filePath = "/";
		else
			filePath = docURL.substring(i); //starts with '/'
		if(address.equals("/") || address.equals(""))
			return;
		if(address.indexOf(':') != -1){
			String[] comp = address.split(":",2);
			hostName = comp[0].trim();
			try{
				portNo = Integer.parseInt(comp[1].trim());
			}catch(NumberFormatException nfe){
				if (scheme.equals("http"))
					portNo = 80;
				else
					portNo = 443;
			}
		}else{
			hostName = address;
			if (scheme.equals("http"))
				portNo = 80;
			else
				portNo = 443;
		}
	}
	
	public URLInfo(String hostName, String filePath ,String scheme){
		this.hostName = hostName;
		this.filePath = filePath;
		this.scheme = scheme;
		this.portNo = 80;
	}
	
	public URLInfo(String hostName,int portNo,String filePath, String scheme){
		this.hostName = hostName;
		this.portNo = portNo;
		this.filePath = filePath;
		this.scheme = scheme;
	}
	
	public String getHostName(){
		return hostName;
	}
	
	public void setHostName(String s){
		hostName = s;
	}
	
	public int getPortNo(){
		return portNo;
	}
	
	public void setPortNo(int p){
		portNo = p;
	}
	
	public String getFilePath(){
		return filePath;
	}
	
	public void setFilePath(String fp){
		filePath = fp;
	}
	
	public String getScheme(){
		return scheme;
	}
	
	public void setScheme(String scheme){
		this.scheme = scheme;
	}
	
	public String getRootURL(){
		return scheme + "://" + hostName+ ":" + portNo;
	}
	
	public String getFullURL(){
		return scheme + "://" + hostName+ ":" + portNo + filePath;
	}
	
	
}
