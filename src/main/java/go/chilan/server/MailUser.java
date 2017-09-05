package go.chilan.server;

import java.util.Properties;  
import javax.mail.*;  
import javax.mail.internet.*;

import com.graphhopper.chilango.network.Constants;  
  
public class MailUser {  
 public static void writeMail(String to, String htmlText, String subject) throws MessagingException {  
  

  final String user="chilango@mail.de";//change accordingly  
  final String password="mexicocity";//change accordingly  
    
   //Get the session object  
   Properties properties = new Properties();  
   properties.put("mail.transport.protocol", "smtp");
   properties.put("mail.smtp.host", "smtp.mail.de");
   properties.put("mail.smtp.port", "587");
   properties.put("mail.smtp.auth", "true");
   properties.put("mail.smtp.user", user);
   properties.put("mail.smtp.password", password);
   properties.put("mail.smtp.starttls.enable", "true");
     
   Session session = Session.getDefaultInstance(properties,  
    new javax.mail.Authenticator() {  
      protected PasswordAuthentication getPasswordAuthentication() {  
    return new PasswordAuthentication(user,password);  
      }  
    });  
  
   //Compose the message  
    try {  
        System.out.println("message in sending process...");  
     MimeMessage message = new MimeMessage(session);  
     message.setFrom(new InternetAddress(user));  
     message.addRecipient(Message.RecipientType.TO,new InternetAddress(to));  
     message.setSubject(subject);  
     message.setContent(htmlText, "text/html; charset=utf-8");
       
     //send the message  
     Transport.send(message);  
  
     System.out.println("message sent successfully...");  
   
     } catch (MessagingException e) {e.printStackTrace();throw e;}  
 }

	public static void createConfirmationMail(String mail, String confirmation) throws MessagingException {
		if (mail != null && !mail.trim().equals("")) {
			String content = "<html>Welcome by ChilanGo! </br> Confirm your mail here: <a href=\"https://srv.chilango.me/WebService/api/mail?verification="
					+ confirmation
					+ "\">click me</a> </br> if the link does not work copy following into your brwoser: https://srv.chilango.me/WebService/api/mail?verification="
					+ confirmation + " </html>";
			writeMail(mail, content, "Confirm ChilanGo Account");
		}

	}  
	
	public static void createContentMail(String b,String text){
		try {
			writeMail("webmaster@chilango.me", text, b);
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void createThankYouMail(String mail,String username,String action,int points){
		try {
			String content="Gracias "+username+" por tu contribución: "+action+" .</br> Está confirmado y va estar en el sistema proximamente</br>.Has ganaste "+points +" puntos</br></br>Vamos a conquistar la ciudad a través de conocimiento </br> Saludos Martin";
			writeMail(mail,content, "ChilanGo! Gracias por tu contribución");
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}  
