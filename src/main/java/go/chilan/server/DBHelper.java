package go.chilan.server;

import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.SQLException;

import javax.mail.MessagingException;

import com.graphhopper.chilango.network.ConnectionMessage;
import com.graphhopper.chilango.network.EasyCrypt;
import com.graphhopper.chilango.network.ServerConnection;
import com.graphhopper.chilango.network.ServerMessageAuth;
import com.graphhopper.chilango.network.ConnectionMessage.ConnectionInformation;

public class DBHelper {

	private Connection connection;
	
	private int userId=-1;

	public DBHelper(Connection connection) {
		this.connection = connection;
	}

	public ConnectionMessage createUser(ServerMessageAuth auth) throws Exception {
		if (auth.getUser() == null || auth.getMail() == null || auth.getUser() == null || auth.getPw() == null)
			return new ConnectionMessage(ConnectionInformation.ERROR, "Corrupt Information");
		else if (auth.getMail().trim().equals("") || !auth.getMail().contains("@") || !auth.getMail().contains("."))
			return new ConnectionMessage(ConnectionInformation.ERROR, "Mail can not be empty or wrong formated");
		else if (auth.getUser().trim().equals("") || auth.getUser().length() < 5)
			return new ConnectionMessage(ConnectionInformation.ERROR, "Username has to be longer than 4 signs");
		else if (auth.getPw().trim().equals("") || auth.getPw().length() < 7)
			return new ConnectionMessage(ConnectionInformation.ERROR, "Password has to be longer than 6 signs");
		else {
			try {
				DBConnector.createUser(connection, auth.getUser(), auth.getMail(), auth.getPw());
				return new ConnectionMessage(ConnectionInformation.VERFIY_MAIL, "Verify your mail");
			} catch (SQLException e) {
				return new ConnectionMessage(ConnectionInformation.ERROR, "Try another name or mail");
			} catch (MessagingException e) {
				return new ConnectionMessage(ConnectionInformation.ERROR, "We could not write you a mail");
			}
		}
	}

	public ConnectionMessage logIn(ServerMessageAuth auth) throws Exception {

		if (auth.getMail() == null || auth.getPw() == null)
			return new ConnectionMessage(ConnectionInformation.ERROR, "Corrupt Information");
		else if (auth.getMail().trim().equals("") || !auth.getMail().contains("@") || !auth.getMail().contains("."))
			return new ConnectionMessage(ConnectionInformation.ERROR, "Mail can not be empty or wrong formated");

		if (auth.getInformation() == ConnectionInformation.LOG_IN) {
			if (auth.getPw().trim().equals("") || auth.getPw().length() < 7)
				return new ConnectionMessage(ConnectionInformation.ERROR, "Password has to be longer than 6 signs");
			else
				return DBConnector.loginUser(connection, auth.getMail(), auth.getPw());
		} else if (auth.getInformation() == ConnectionInformation.LOG_IN_TOKEN) {
			return DBConnector.prooveToken(auth.getAuth(), auth.getMail());
		} else {
			return null;
		}
	}
	
	public int getUserId(String mail){
		if(userId!=-1)
			return userId;
		else
			userId=DBConnector.getUserId(connection, mail);
		
		return userId;
	}
	
	public long addTransaction(String path,int type) throws SQLException, MessagingException{
		
		return DBConnector.addTransactoin(connection, path, type, userId);
	}
	

	public boolean validateMail(String conf) {
		int user = DBConnector.idOfValueInDatabse(connection, "EMAIL_CONFIRMATION", "CONFIRMATION", conf);
		System.out.println(conf+" "+user);
		if (user > 0)
		{
			Boolean setToTrue=true;
			DBConnector.updateValueInTableById(connection,"USERS","CONFIRMATION",setToTrue,user);
			return true;
		}
			
		return false;

	}
}
