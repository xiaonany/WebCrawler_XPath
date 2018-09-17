package edu.upenn.cis455.storage;
import java.io.File;    
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;


public class DBWrapper {
	private String envDirectory = null;
	private Environment Envir;
	private EntityStore store;
	private MessageDigest digest;
	
	public DBWrapper(String Dir) {
		//set up the database with the given directory
		envDirectory = Dir;
		EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setTransactional(true);
		envConfig.setAllowCreate(true);
		File file = new File(this.envDirectory);
		if (!file.exists()) {
			file.mkdir();
			file.setWritable(true,false);
		}
		this.Envir = new Environment(file, envConfig);
		StoreConfig config = new StoreConfig();
		config.setAllowCreate(true);
		config.setTransactional(true);
		this.store = new EntityStore(this.Envir, "store", config);
		//set the Message digest for hashing of password
		try {
			this.digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {

		}
	}
	
	//get the environment of the database
	public Environment getEnvironment() { 
		return this.Envir; 
	}
	
	//set the environment of the database 
	public void setEnvironment(Environment Envir) {
		this.Envir = Envir;
	}
	
	//get the entity store of the database
	public EntityStore getStore() {
		return this.store; 
	}
	//set the entity store of the database
	public void setStore(EntityStore store) {
		this.store = store;
	}
	
	//get the user entity with given username from the database
	public User getUser(String username) {
		PrimaryIndex<String,User> userInfo = store.getPrimaryIndex(String.class, User.class);
		return userInfo.get(username);
	}
	
	//insert an user entity with given username and password 
	//return true if insert successfully, else if the user entity with the primary key already exits
	public boolean insertUser(String username, String password) {
		PrimaryIndex<String,User> userInfo = store.getPrimaryIndex(String.class, User.class);
		System.out.println("here");
		if (userInfo.contains(username)) {
			return false;
		} else {
			try {
				userInfo.put(new User(username, password , digest));
			} catch (DatabaseException e) {				
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			return true;
		}
	}
	
	//get the Doc entity with given username from the database
	public Doc getDoc(String url) {
		PrimaryIndex<String,Doc> DocInfo = store.getPrimaryIndex(String.class, Doc.class);
		return DocInfo.get(url);
	}
	
	//insert a new doc into the database
	public boolean insertDoc(Doc doc, Transaction t) {
		PrimaryIndex<String,Doc> DocInfo = store.getPrimaryIndex(String.class, Doc.class);
		return DocInfo.putNoOverwrite(t, doc);
	}
	
	//overwrite the existing doc with the updated doc
	public void updateCrawlDoc(Doc doc, Transaction t) {
		PrimaryIndex<String,Doc> DocInfo = store.getPrimaryIndex(String.class, Doc.class);
		doc.setLastCrawled();
		DocInfo.put(t, doc);
	}
	
	public ArrayList<Doc> getAllDocs(){
		ArrayList<Doc> results = new ArrayList<Doc>();
		PrimaryIndex<String,Doc> DocInfo = store.getPrimaryIndex(String.class, Doc.class);
		EntityCursor<Doc> Docs = DocInfo.entities();
		for (Doc d:Docs){
			results.add(d);
		}
		Docs.close();
		return results;
	}
	
	public Channel getChannel(String name){
		PrimaryIndex<String,Channel> channels = store.getPrimaryIndex(String.class, Channel.class);
		return channels.get(name);
	}
	
	public boolean insertChannel(String name, String xpath, String owner) throws DatabaseException, Exception{
		PrimaryIndex<String,Channel> channels = store.getPrimaryIndex(String.class, Channel.class);
		if (channels.contains(name)){
			System.out.println("1");
			return false;
		}else{
			System.out.println("2");
			channels.put(new Channel(name,xpath,owner));
			return true;
		}
	}
	
	public void insertChannel(Channel c){
		PrimaryIndex<String,Channel> channels = store.getPrimaryIndex(String.class, Channel.class);
		channels.put(c);
	}
	
	public boolean deleteChannel(String name, String owner){
		PrimaryIndex<String,Channel> channels = store.getPrimaryIndex(String.class, Channel.class);
		Channel channel = channels.get(name);
		if (channel.getowner().equals(owner)){
			channels.delete(name);
			return true;
		}else{
			return false;
		}
	}
	
	public boolean containsChannel(String name){
		PrimaryIndex<String,Channel> channels = store.getPrimaryIndex(String.class, Channel.class);
		return channels.contains(name);
	}
	
	public ArrayList<Channel> getAllChannels(){
		PrimaryIndex<String,Channel> channels = store.getPrimaryIndex(String.class, Channel.class);
		ArrayList<Channel> result = new ArrayList<Channel>();
		EntityCursor<Channel> cursor = channels.entities();
		for (Channel c:cursor){
			result.add(c);
		}
		cursor.close();
		return result;
	}
	
	public Transaction getTransaction() {
		return this.Envir.beginTransaction(null, null);
	}
	
	public MessageDigest getDigest(){
		return this.digest;
	}
	
	public void close() {
		if (store!= null)
			store.close();
		if (Envir != null)
			Envir.close();
	}
}
