package go.chilan.server;

import static com.mongodb.client.model.Filters.eq;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import javax.mail.MessagingException;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.jts.JtsAdapterFactory;
import com.github.filosganga.geogson.model.Coordinates;
import com.github.filosganga.geogson.model.Point;
import com.github.filosganga.geogson.model.positions.SinglePosition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.graphhopper.chilango.data.database.TransactionModel;
import com.graphhopper.chilango.data.database.UserModel;
import com.graphhopper.chilango.network.ConnectionMessage;
import com.graphhopper.chilango.network.ConnectionMessage.ConnectionInformation;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import com.mongodb.util.JSON;
import com.vividsolutions.jts.geom.Coordinate;
import com.graphhopper.chilango.network.Constants;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;

public class MongoDB {

	public static String createToken(String user) {
		byte[] key = getSignatureKey();

		Date expirationDate = new Date(System.currentTimeMillis() + 24 * 1000 * 60 * 60 * 14);
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
			e.printStackTrace();
			// don't trust the JWT!#
			return new ConnectionMessage(ConnectionInformation.WRONG_TOKEN, "token wrong or outdated");
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
	public static MongoClient createDatabaseConnection() {
		System.out.println("START INIT " + System.currentTimeMillis());
		String user = "admin"; // the user name
		String database = "admin"; // the name of the database in which the
									// user is defined
		char[] password = "chilango_pw".toCharArray(); // the password as a
														// character array
		// ...
		MongoCredential credential = MongoCredential.createCredential(user, database, password);
		MongoClient mongoClient = new MongoClient(new ServerAddress("mongo", 27017), Arrays.asList(credential));

		System.out.println("ready INIT " + System.currentTimeMillis());

		return mongoClient;

	}

	public static MongoDatabase getChilangoDatabase(MongoClient mongoClient) {
		MongoDatabase db = mongoClient.getDatabase("chilangodb");
		return db;
	}

	public static String getUserId(MongoDatabase db, String mail) {
		return idOfValueInDataBase(db, "users", "mail", mail);
	}

	public static ConnectionMessage loginUser(MongoDatabase db, String mail, String pw) {

		Document userDoc = findUser(db, mail);

		if (userDoc == null)
			return new ConnectionMessage(ConnectionInformation.WRONG_LOGIN);
		System.out.println("user found: ");
		try {
			if (!userDoc.get("pwHash").equals(PWHash.generateStorngPasswordHash(pw)))
				return new ConnectionMessage(ConnectionInformation.WRONG_LOGIN);
			else {
				System.out.println("user log in Ok");

				if (userDoc.getBoolean("mail_confirmation", false) == false)
					return new ConnectionMessage(ConnectionInformation.VERFIY_MAIL, "verify your mail");

				System.out.println("making token");

				return new ConnectionMessage(ConnectionInformation.LOGIN_OK, createAuthKey(db, mail));

			}
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			return new ConnectionMessage(ConnectionInformation.ERROR, "PW-Hash error");
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			return new ConnectionMessage(ConnectionInformation.ERROR, "Invalid Key Spec");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return new ConnectionMessage(ConnectionInformation.ERROR, "token creation failed");
		}
	}

	private static String createAuthKey(MongoDatabase db, String mail) throws Exception {
		String token = createToken(mail);
		System.out.println(token.length());

		if (updateValueInTable(db, "users", "token", token, "mail", mail) == false)
			throw new Exception("no update possible. Probably no user in db");
		else
			return token;

	}

	public static Document findUser(MongoDatabase db, String mail) {
		MongoCollection<Document> userCollection = db.getCollection("users");
		return userCollection.find(eq("mail", mail)).first();
	}

	public static ConnectionMessage checkAuthKey(MongoDatabase db, String mail, String authkey) {
		if (!prooveToken(authkey, mail).getInfoConnection().equals(ConnectionInformation.CORRECT_TOKEN))
			return prooveToken(authkey, mail);

		Document myDoc = findUser(db, mail);

		if (myDoc == null) {
			return new ConnectionMessage(ConnectionInformation.WRONG_LOGIN, "wrong mail");
		} else {

			if (myDoc.get("token").equals(authkey)) {
				return new ConnectionMessage(ConnectionInformation.WRONG_LOGIN, "Login with all your data");
			} else {
				return new ConnectionMessage(ConnectionInformation.CORRECT_TOKEN);
			}

		}

	}

	public static int addTransaction(MongoDatabase db, String path, int type, String userId, int routeId,
			Date creationTIme)  {

		MongoCollection<Document> transactionCollection = db.getCollection("transactions");

		Random random = new Random();
		int transactionId = 0;
		Document myDoc = null;
		while (transactionId == 0) {
			transactionId = random.nextInt();
			myDoc = transactionCollection.find(eq("transactionId", transactionId)).first();
			if (myDoc != null)
				transactionId = 0;
		}

		if (myDoc == null) {

			Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GeometryAdapterFactory())
					.registerTypeAdapterFactory(new JtsAdapterFactory()).create();

			TransactionModel transaction = new TransactionModel(routeId, path, type, userId, transactionId,
					creationTIme);

			String json = gson.toJson(transaction);// data is User DTO, just
													// pojo!
			System.out.println(json);

			BasicDBObject document1 = (BasicDBObject) JSON.parse(json);
			transactionCollection.insertOne(new Document(document1));

			return transactionId;
		} else
			return -1;

	}

	public static void createUser(MongoDatabase db, String mail, String username, String password) throws Exception {
		UserModel user;
		try {
			user = new UserModel(username, mail, PWHash.generateStorngPasswordHash(password));

			addNewUser(db, user);

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		} catch (Exception e) {
			throw e;
		}

	}

	private static String createMailConfirmationToken(MongoDatabase db) {
		MongoCollection<Document> userCollection = db.getCollection("users");

		boolean createKey = true;
		while (createKey) {
			String confirmation = UUID.randomUUID().toString();

			Document myDoc = userCollection.find(eq("mailConfirmationToken", confirmation)).first();
			if (myDoc == null) {
				createKey = false;
				return confirmation;
			}

		}

		return null;
	}

	public static void addNewUser(MongoDatabase db, UserModel user) throws Exception {

		MongoCollection<Document> userCollection = db.getCollection("users");

		BasicDBObject hasMail = new BasicDBObject();
		hasMail.put("mail", user.getMail());

		Document myDoc = userCollection.find(eq("mail", user.getMail())).first();

		if (myDoc == null) {
			System.out.println("hier her kommen wir");
			String mailConfirmation = createMailConfirmationToken(db);
			user.setMailConfirmationToken(mailConfirmation);
			// Point p = new Point(new SinglePosition(Coordinates.of(0.3,
			// 0.5)));
			// user.setHomePoint(p);
			// my save
			Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GeometryAdapterFactory())
					.registerTypeAdapterFactory(new JtsAdapterFactory()).create();

			String json = gson.toJson(user);// data is User DTO, just pojo!
			System.out.println(json);

			BasicDBObject document1 = (BasicDBObject) JSON.parse(json);
			userCollection.insertOne(new Document(document1));

			try {

				System.out.println("mail Confirmation: " + mailConfirmation);
				MailUser.createConfirmationMail(user.getMail(), mailConfirmation);
			} catch (MessagingException e) {
				// throw e;
			}

		} else {

			throw new Exception("adding rejected. Double entry");
		}

	}

	public static String idOfValueInDataBase(MongoDatabase db, String collection, String variable, Object value) {

		Document myDoc = documentOfValueInDataBase(db, collection, variable, value);
		if (myDoc != null) {
			return myDoc.getObjectId("_id").toString();
		}
		return null;

	}

	public static Document documentOfValueInDataBase(MongoDatabase db, String collection, String variable,
			Object value) {
		MongoCollection<Document> userCollection = db.getCollection(collection);

		return userCollection.find(eq(variable, value)).first();
	}

	public static boolean updateValueInTableById(MongoDatabase db, String collection, String variable, Object value,
			String id) {
		MongoCollection<Document> currentCollection = db.getCollection(collection);

		Bson updates = Updates.set(variable, value);

		Document doc = currentCollection.findOneAndUpdate(eq("_id", new ObjectId(id)), updates);

		if (doc != null)
			return true;

		return false;
	}

	public static boolean updateValueInTable(MongoDatabase db, String collection, String variable, Object value,
			String identifierName, Object identifier) {
		MongoCollection<Document> currentCollection = db.getCollection(collection);

		Bson updates = Updates.set(variable, value);

		Document doc = currentCollection.findOneAndUpdate(eq(identifierName, identifier), updates);

		if (doc != null)
			return true;

		return false;
	}

}
