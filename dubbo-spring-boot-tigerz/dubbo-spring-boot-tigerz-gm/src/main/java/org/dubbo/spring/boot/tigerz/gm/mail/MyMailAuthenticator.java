package org.dubbo.spring.boot.tigerz.gm.mail;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class MyMailAuthenticator extends Authenticator{
	String userName;
	String password;
	
	public MyMailAuthenticator(String userName, String password) {
		super();
		this.userName = userName;
		this.password = password;
	}

	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(userName, password);
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
