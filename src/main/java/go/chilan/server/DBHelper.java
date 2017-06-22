package go.chilan.server;

import java.util.Date;

import javax.mail.MessagingException;

import com.graphhopper.chilango.network.ConnectionMessage;
import com.graphhopper.chilango.network.EasyCrypt;
import com.graphhopper.chilango.network.ServerConnection;
import com.graphhopper.chilango.network.ServerMessageAuth;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.graphhopper.chilango.network.ConnectionMessage.ConnectionInformation;

public class DBHelper {

	private MongoClient connection;
	private MongoDatabase db;

	private String userId = null;

	public DBHelper(MongoClient connection) {
		this.connection = connection;
		db = MongoDB.getChilangoDatabase(connection);
	}

	public DBHelper() {
		this.connection = MongoDB.createDatabaseConnection();
		db = MongoDB.getChilangoDatabase(connection);
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
				MongoDB.createUser(db, auth.getUser(), auth.getMail(), auth.getPw());
				return new ConnectionMessage(ConnectionInformation.VERFIY_MAIL, "Verify your mail");
			} catch (MessagingException e) {
				return new ConnectionMessage(ConnectionInformation.ERROR, "We could not write you a mail");
			} catch (Exception e) {
				return new ConnectionMessage(ConnectionInformation.ERROR, "User already created");
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
				return MongoDB.loginUser(db, auth.getMail(), auth.getPw());
		} else if (auth.getInformation() == ConnectionInformation.LOG_IN_TOKEN) {
			return MongoDB.prooveToken(auth.getAuth(), auth.getMail());
		} else {
			return null;
		}
	}

	public String getUserId(String mail) {
		if (userId != null)
			return userId;
		else
			userId = MongoDB.getUserId(db, mail);

		return userId;
	}

	public long addTransaction(String path, int type, String userId, int routeId, Date creationTIme)
			throws MessagingException {

		return MongoDB.addTransaction(db, path, type, userId, routeId, creationTIme);
	}

	public boolean validateMail(String conf) {

		return MongoDB.updateValueInTable(db, "users", "mail_confirmation", true, "mailConfirmationToken", conf);

	}

	public void close() {
		connection.close();
	}
}
