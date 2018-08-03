package org.dubbo.spring.boot.tigerz.gm.mail;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

/**
 * 发送邮件，具体发送给谁细节都在这里面
 * MailSendSuccess
 * @Desc: 
 * @Company: TigerZ
 * @author Wang Jingci
 * @date 2018年6月1日 下午3:02:22
 */
public class MailSendSuccess {
	private static Properties mPro = null;

	
	/**
	 * 发送邮件模板给中介，用户，还有我们自己
	 * @param title
	 * @param context
	 * @param email
	 * @return
	 */
	public static boolean sendEmail(String title,String context,String email) {
		try {
			MailSenderInfo info = getInfo();
			info.setSubject(title);
			info.setContent(context);
			SimpleMailSender sender = new SimpleMailSender(info.getMailServerHost(),info.getUserName(), info.getPassword());
			List<String> mAll = new ArrayList<String>();
			info.setToAddress(email);
			mAll.add("support@tigerz.nz");
			mAll.add(info.getToAddress());
			System.out.println("prepare to send email !!!!!");
			sender.send(mAll, info);
			return true;
		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * 把用户反馈发给自己
	 * @param title
	 * @param content
	 */
	public static void sendFeedBackSucess(String title,String content) {
		try {
			MailSenderInfo info = getInfo();
			info.setSubject(title);
			info.setContent(content);
			SimpleMailSender sender = new SimpleMailSender(info.getMailServerHost(),info.getUserName(), info.getPassword());
			List<String> mAll = new ArrayList<String>();
			mAll.add("86441350@qq.com");
			mAll.add("13161685634@163.com");
			mAll.add("hr@tigerz.nz");
			mAll.add("454776519@qq.com");
			mAll.add(info.getToAddress());
			sender.send(mAll, info);
		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}
	
	public static String refFormatNowDateForMin(long time) {
		  Date nowTime = new Date(time);
		  SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		  String retStrFormatNowDate = sdFormatter.format(nowTime);
		  return retStrFormatNowDate;
		}

	public  Properties  getPro(){
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("mail.properties");
		Properties mongoProp = new Properties();
		try {
			mongoProp.load(inputStream);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return mongoProp;
	}
	public static MailSenderInfo getInfo(){
		MailSenderInfo info = new MailSenderInfo();
		MailSendSuccess success = new MailSendSuccess();
		if(MailSendSuccess.mPro==null){
			MailSendSuccess.mPro = success.getPro();
		}
		info.setMailServerHost(mPro.getProperty("mail.host","smtp.ym.163.com"));
		info.setMailServerPort(mPro.getProperty("mail.port","24"));
		info.setUserName(mPro.getProperty("mail.username","support@tigerz.nz"));
		info.setFromAddress(mPro.getProperty("mail.username","support@tigerz.nz"));
		info.setPassword(mPro.getProperty("mail.password","zhu88jie"));
		info.setToAddress("459877659@qq.com"); 
//		info.setMailServerPort("24");
//		info.setFromAddress("support@tigerz.nz");
//		info.setUserName("support@tigerz.nz");
//		info.setMailServerHost("smtp.ym.163.com");
//		info.setMailServerPort("24");
//		info.setFromAddress("support@tigerz.nz");
//		info.setUserName("support@tigerz.nz");
//		info.setPassword("zhu99jie");
////		info.setMailServerHost("smtp.exmail.qq.com");
////		info.setMailServerPort("465");
////		info.setFromAddress("support@tigerz.nz");
////		info.setUserName("support@tigerz.nz");
////		info.setPassword("Laohumaifang0703");
////		info.setFromAddress("cl@tigerz.nz");
////		info.setUserName("cl@tigerz.nz");
////		info.setPassword("AAa121994554");
		
		return info;
	}
}
