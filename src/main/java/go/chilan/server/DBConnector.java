package go.chilan.server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import javax.mail.MessagingException;

import com.graphhopper.chilango.network.ConnectionMessage;
import com.graphhopper.chilango.network.ConnectionMessage.ConnectionInformation;
import com.graphhopper.chilango.network.Constants;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;

public class DBConnector {

	public static String createToken(String user) {
		byte[] key = getSignatureKey();

		Date expirationDate = new Date(System.currentTimeMillis() + 24 * 1000 * 60 * 60);
		String jwt = Jwts.builder().setIssuer("http://chilango.me").setSubject("users/" + user)
				.setExpiration(expirationDate).signWith(SignatureAlgorithm.HS256, key).compact();

		return jwt;
	}

	public static ConnectionMessage prooveToken(String plainjwt, String user) {
		String subject = "HACKER";
		try {

			Jws<Claims> claims = Jwts.parser().setSigningKey(getSignatureKey()).parseClaimsJws(plainjwt);

			Date currentTime = new Date(System.currentTimeMillis());
			if (claims.getBody().getSubject().equals("users/" + user)
					&& currentTime.before(claims.getBody().getExpiration())) {
				return new ConnectionMessage(ConnectionInformation.CORRECT_TOKEN);
			} else {
				if (!claims.getBody().getSubject().equals("users/" + user))
					return new ConnectionMessage(ConnectionInformation.WRONG_TOKEN, "wrong user");
				else if (!currentTime.before(claims.getBody().getExpiration()))
					return new ConnectionMessage(ConnectionInformation.EXPIRED_TOKEN);
				else
					return new ConnectionMessage(ConnectionInformation.WRONG_TOKEN, "unknown error");
			}

			// OK, we can trust this JWT

		} catch (SignatureException e) {

			// don't trust the JWT!#
			return new ConnectionMessage(ConnectionInformation.WRONG_TOKEN, "signature Exception");
		} catch (io.jsonwebtoken.ExpiredJwtException e) {

			// don't trust the JWT!#
			return new ConnectionMessage(ConnectionInformation.EXPIRED_TOKEN);
		}
	}

	private static byte[] getSignatureKey() {
		final byte[] key = { (byte) 5, (byte) 8, (byte) 9, (byte) 120, (byte) 12 };
		// TODO Auto-generated method stub
		return key;
	}

	/*
	 * The method creates a Connection object. Loads the embedded driver, starts
	 * and connects to the database using the connection URL.
	 */
	public static Connection createDatabaseConnection() throws SQLException, ClassNotFoundException {
		String driver = "org.apache.derby.jdbc.EmbeddedDriver";
		Class.forName(driver);
		String protocol = "jdbc:derby:";
		Properties props = new Properties();
		props.put("user", "chilango");
		props.put("password", "mexicocity");
		File file=new File(App.baseFolder,"ChilangoDB");
		System.out.println("file of app:"+(new File("")).getAbsolutePath());
		System.out.println(file.getAbsolutePath()+" "+file.exists());
		System.out.println((new File(file,"checkme")).getAbsolutePath()+" "+(new File(file,"checkme")).exists());
		Connection c = DriverManager.getConnection(protocol + file.getAbsolutePath(), props);
		return c;
	}

	public static int getUserId(Connection connection, String mail) {
		try {

			ResultSet rs = null;
			Statement s = connection.createStatement();

			rs = s.executeQuery("SELECT ID FROM USERS WHERE MAIL = '" + mail+"'");

			if (!rs.next()) {
				return -1;
			} else {
				int id = rs.getInt("ID");
				return id;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public static ConnectionMessage loginUser(Connection connection, String mail, String pw) {
		try {

			ResultSet rs = null;
			Statement s = connection.createStatement();

			rs = s.executeQuery("SELECT * FROM USERS WHERE MAIL = '" + mail + "' AND PW='" + pw + "'");

			if (!rs.next()) {
				return new ConnectionMessage(ConnectionInformation.WRONG_LOGIN);
			} else {
				System.out.println("user found");

				if (rs.getBoolean("CONFIRMATION") == false)
					return new ConnectionMessage(ConnectionInformation.VERFIY_MAIL, "verify your mail");
				// else
				System.out.println("making token");
				return new ConnectionMessage(ConnectionInformation.LOGIN_OK, createAuthKey(connection, mail));
			}

		} catch (SQLException e) {
			e.printStackTrace();
			return new ConnectionMessage(ConnectionInformation.ERROR);
		}
	}

	private static String createAuthKey(Connection connection, String mail) throws SQLException {
		String token = createToken(mail);
		System.out.println(token.length());

		try {
			Statement s;
			s = connection.createStatement();

			ResultSet rs = s.executeQuery("SELECT ID FROM USERS WHERE MAIL = '" + mail + "'");
			if (!rs.next()) {
				throw new SQLException("no user found");
			} else {
				int id = rs.getInt("ID");
				System.out.println(id);
				s.execute("UPDATE TOKEN SET AUTHKEY = '" + token + "' WHERE ID=" + id);
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}

		return token;
	}

	public static ConnectionMessage checkAuthKey(Connection connection, String mail, String authkey) {
		if (!prooveToken(authkey, mail).getInfoConnection().equals(ConnectionInformation.CORRECT_TOKEN))
			return prooveToken(authkey, mail);

		try {
			Statement s;
			s = connection.createStatement();

			ResultSet rs = s.executeQuery("SELECT ID FROM USERS WHERE MAIL = '" + mail + "'");
			if (!rs.next()) {
				return new ConnectionMessage(ConnectionInformation.WRONG_LOGIN, "wrong mail");
			} else {
				int id = rs.getInt("ID");
				System.out.println(id);
				rs = s.executeQuery("SELECT * FROM TOKEN WHERE ID=" + id + "AND AUTHKEY='" + authkey + "'");
				if (!rs.next()) {
					return new ConnectionMessage(ConnectionInformation.WRONG_LOGIN, "Login with all your data");
				} else {
					return new ConnectionMessage(ConnectionInformation.CORRECT_TOKEN);
				}
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static boolean makeStatement(Connection connection, String statement) throws SQLException {
		Statement s;
		try {
			s = connection.createStatement();

			s.execute(statement);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			return false;
		}

		return true;
	}

	
	// CREATE TABLE TRANSACTIONS (ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),PATH VARCHAR(50) NOT NULL,USERID INTEGER NOT NULL,TRANSACTION_TYPE INTEGER NOT NULL, DAY DATE NOT NULL, STATUS INTEGER NOT NULL, PRIMARY KEY (ID))
	public static int addTransactoin(Connection connection,String path,int type,int userId)
			throws SQLException, MessagingException {

		String confirmation = "";
		try {
			Statement s = connection.createStatement();
			LocalDate todayLocalDate = LocalDate.now( ZoneId.of( "America/Montreal" ) );
			java.sql.Date sqlDate = java.sql.Date.valueOf( todayLocalDate );
		String text = "INSERT INTO TRANSACTIONS(PATH , USERID, TRANSACTION_TYPE, DAY, STATUS) values('" + path + "',"+ userId+", " + type+ ",'"+sqlDate.toString()+"'," + Constants.TRANSACTION_PENDING + ")";
		System.out.println(text);
		s.execute(text);

			ResultSet rs = s.executeQuery("SELECT ID FROM TRANSACTIONS WHERE PATH = '" + path + "'");
		
		if (!rs.next()) {
			throw new SQLException("no new entry");
		} else {
		int id = rs.getInt("ID");
		return id;
		}
		}catch(SQLException e)
		{
			e.printStackTrace();
			return -1;
		}

	}




	public static void createUser(Connection connection, String username, String mail, String password)
			throws SQLException, MessagingException {

		String confirmation = "";
		try {
			Statement s = connection.createStatement();
			String text = "INSERT INTO USERS(MAIL , CONFIRMATION, PW, NAME) values('" + mail + "', false, '" + password
					+ "','" + username + "')";
			System.out.println(text);
			s.execute(text);

			ResultSet rs = s.executeQuery("SELECT ID FROM USERS WHERE MAIL = '" + mail + "'");
			if (!rs.next()) {
				throw new SQLException("no new entry");
			} else {
				int id = rs.getInt("ID");

				confirmation = UUID.randomUUID().toString();

				boolean createKey = true;
				while (createKey) {
					rs = s.executeQuery(
							"SELECT ID FROM EMAIL_CONFIRMATION WHERE CONFIRMATION = '" + confirmation + "'");
					if (!rs.next()) {
						s.execute("INSERT INTO EMAIL_CONFIRMATION(ID,CONFIRMATION) values(" + id + ",'" + confirmation
								+ "')");
						createKey = false;
					} else
						createKey = true;
				}

			}

		} catch (SQLException e) {
			System.out.println(e.getSQLState() + " " + e.getMessage() + " " + e.getLocalizedMessage());
			throw e;
		}

		try {
			MailUser.createConfirmationMail(mail, confirmation);

		} catch (MessagingException e) {
			throw e;
		}
	}

	public static int idOfValueInDatabse(Connection connection, String table, String row, String value) {

		Statement s;
		try {
			s = connection.createStatement();

			ResultSet rs = s.executeQuery("SELECT ID FROM " + table + " WHERE " + row + " = '" + value + "'");
			if (!rs.next()) {
				return 0;
			} else {
				int id = rs.getInt("ID");
				return id;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;

	}

	public static boolean updateValueInTableById(Connection connection, String table, String row, Object value,
			int id) {

		Statement s;
		try {
			s = connection.createStatement();

			String insert_value = "";
			if (value instanceof String) {
				insert_value = "'" + (String) value + "'";

			} else if (value instanceof Boolean) {
				insert_value = ((Boolean) value).toString();
			} else if (value instanceof Integer || value instanceof Float || value instanceof Double) {
				insert_value = value.toString();
			}

			s.execute("UPDATE " + table + " SET " + row + " = " + insert_value + " WHERE ID=" + id);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;

	}

}
