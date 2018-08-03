package org.dubbo.spring.boot.tigerz.gm.mail;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;


public class SimpleMailSender {
  

    private final transient Properties props = System.getProperties();

    private transient MyMailAuthenticator authenticator;
  

    private transient Session session;
  

    public SimpleMailSender(final String smtpHostName, final String username,
        final String password) {
        init(username, password, smtpHostName);
    }
  

    public SimpleMailSender(final String username, final String password) {
        final String smtpHostName = "smtp." + username.split("@")[1];
        init(username, password, smtpHostName);
    }
  

    /**
     * 配置基本信息
     * @param username
     * @param password
     * @param smtpHostName
     */
    private void init(String username, String password, String smtpHostName) {
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.host", smtpHostName);
        props.setProperty("mail.smtp.starttls.enable", "true");
        props.setProperty("mail.transport.protocol", "smtp");  
        authenticator = new MyMailAuthenticator(username, password);
        session = Session.getInstance(props, authenticator);
    }
  

    /**
     * 单人发送邮件
     * @param recipient
     * @param fromAddress
     * @param subject
     * @param content
     * @throws AddressException
     * @throws MessagingException
     */
    public void send(String recipient,String fromAddress, String subject, Object content)
        throws AddressException, MessagingException {
        final MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromAddress));
        message.setRecipient(RecipientType.TO, new InternetAddress(recipient));
        message.setSubject(subject);
        message.setContent(content.toString(), "text/html;charset=utf-8");
        Transport.send(message);
    }
  

    /**
     * 发送邮件给多人
     * @param recipients
     * @param fromAddress
     * @param subject
     * @param content
     * @throws AddressException
     * @throws MessagingException
     */
    public void send(List<String> recipients,String fromAddress, String subject, Object content)
        throws AddressException, MessagingException {
        final MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromAddress));
        final int num = recipients.size();
        InternetAddress[] addresses = new InternetAddress[num];
        for (int i = 0; i < num; i++) {
            addresses[i] = new InternetAddress(recipients.get(i));
        }
        message.setRecipients(RecipientType.TO, addresses);
        message.setSubject(subject);
        message.setContent(content.toString(), "text/html;charset=utf-8");
        Transport.send(message);
    }
  
    /**
     * 以邮件体方式发送邮件给单人
     * @param recipient
     * @param mail
     * @throws AddressException
     * @throws MessagingException
     */
    public void send(String recipient, MailSenderInfo mail)
        throws AddressException, MessagingException {
        send(recipient,mail.getFromAddress(), mail.getSubject(), mail.getContent());
    }
  
    /**
     * 以有邮件体方式发送邮件给多人
     * @param recipients
     * @param mail
     * @throws AddressException
     * @throws MessagingException
     */
    public void send(List<String> recipients, MailSenderInfo mail)
        throws AddressException, MessagingException {
        send(recipients,mail.getFromAddress(), mail.getSubject(), mail.getContent());
    }
  
}