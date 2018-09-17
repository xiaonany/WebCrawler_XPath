package edu.upenn.cis455.storage;

import java.nio.charset.StandardCharsets; 
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class User {
	@PrimaryKey
	private String username;
	private byte [] password; 

	public User(){
	
	}
	
	public User(String username, String password, MessageDigest digest) throws NoSuchAlgorithmException {
		this.username = username;
		// hash the given password with SHA256 hashing and keep the hashing result as what will be stored in the data base 
		this.password = digest.digest(password.getBytes(StandardCharsets.UTF_8));
	}
	
	//get username
	public String getUsername() { 
		return this.username; 
	}
	//set username
	public void setUsername(String username) {
		this.username = username;
	}
	//get the hashed password
	public byte [] getPassword() {
		return this.password; 
	}
	//set the password
	public void setPassword(String password) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		this.password = digest.digest(password.getBytes(StandardCharsets.UTF_8));
	}
}