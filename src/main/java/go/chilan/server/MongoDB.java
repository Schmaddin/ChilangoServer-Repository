package go.chilan.server;

import static com.mongodb.client.model.Filters.eq;


import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.mail.MessagingException;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.jts.JtsAdapterFactory;
import com.github.filosganga.geogson.model.AbstractGeometry;
import com.github.filosganga.geogson.model.Coordinates;
import com.github.filosganga.geogson.model.Geometry;
import com.github.filosganga.geogson.model.Point;
import com.github.filosganga.geogson.model.positions.Positions;
import com.github.filosganga.geogson.model.positions.SinglePosition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.graphhopper.chilango.FileHelper;
import com.graphhopper.chilango.data.JsonHelper;
import com.graphhopper.chilango.data.Route;
import com.graphhopper.chilango.data.RouteHelper;
import com.graphhopper.chilango.data.Status;
import com.graphhopper.chilango.data.UserStatus;
import com.graphhopper.chilango.data.database.PointModel;
import com.graphhopper.chilango.data.database.RouteModel;
import com.graphhopper.chilango.data.database.RouteVersionModel;
import com.graphhopper.chilango.data.database.SubmitType;
import com.graphhopper.chilango.data.database.TransactionModel;
import com.graphhopper.chilango.data.database.UserModel;

import com.graphhopper.chilango.data.database.FeedbackModel;
import com.graphhopper.chilango.network.ConnectionMessage;
import com.graphhopper.chilango.network.ConnectionMessage.ConnectionInformation;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
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
	static private Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GeometryAdapterFactory())
			.registerTypeAdapterFactory(new JtsAdapterFactory()).create();

	public static String createToken(String user) {
		byte[] key = getSignatureKey();

		Date expirationDate = new Date(System.currentTimeMillis() + 24 * 1000 * 60 * 60 * 14);
		String jwt = Jwts.builder().setIssuer("http://chilango.me").setSubject("users/" + user)
				.setExpiration(expirationDate).signWith(SignatureAlgorithm.HS256, key).compact();

		return jwt;
	}

	public static boolean addDocumentByObject(MongoDatabase db, String collectionName, Object object,
			String doubleEnryRow, Object doubleEntryValue) {
		MongoCollection<Document> collection = db.getCollection(collectionName);

		Document myDoc = null;

		if (doubleEnryRow != null && doubleEntryValue!=null)
			myDoc=collection.find(eq(doubleEnryRow, doubleEntryValue)).first();

		if (myDoc == null) {
			String json = gson.toJson(object);// data is User DTO, just pojo!
			System.out.println(json);

			BasicDBObject document1 = (BasicDBObject) JSON.parse(json);
			collection.insertOne(new Document(document1));
			return true;
		} else
			return false;

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

	public static int createNewTransactionId(MongoDatabase db) {
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

		return transactionId;
	}

	public static boolean addTransaction(MongoDatabase db, String path, SubmitType type, String userId, int status,
			int routeId, Date creationTime, byte trust, Geometry geometry, int transactionId, int baseTransactionId) {

		MongoCollection<Document> transactionCollection = db.getCollection("transactions");

		Random random = new Random();

		if (transactionId != 0) {

			TransactionModel transaction = new TransactionModel(routeId, path, type, userId, transactionId, status,
					creationTime, trust, geometry, baseTransactionId);

			String json = gson.toJson(transaction);// data is User DTO, just
													// pojo!
			System.out.println(json);

			BasicDBObject document1 = (BasicDBObject) JSON.parse(json);
			transactionCollection.insertOne(new Document(document1));

			return true;
		} else
			return false;

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
			String mailConfirmation = createMailConfirmationToken(db);
			user.setMailConfirmationToken(mailConfirmation);
			// Point p = new Point(new SinglePosition(Coordinates.of(0.3,
			// 0.5)));
			// user.setHomePoint(p);
			// my save

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

	public static boolean updateValueThroughJsonInTableById(MongoDatabase db, String collection, String variable,
			String value, String id) {
		MongoCollection<Document> currentCollection = db.getCollection(collection);

		Bson updates = Updates.set(variable, JSON.parse(value));

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

	private static UserModel getUserModel(MongoDatabase db, String identifier) {
		Document document = documentOfValueInDataBase(db, "users", "_id", new ObjectId(identifier));

		if (document == null) {
			System.out.println("document not found");
			return null;
		}

		String json = com.mongodb.util.JSON.serialize(document);

		System.out.println("retrieved: " + json);

		return (UserModel) gson.fromJson(json, UserModel.class);
	}

	public static UserStatus getUserStatus(MongoDatabase db, String identifier) {

		UserModel user = getUserModel(db, identifier);

		if (user == null)
			return null;

		if (user.getPointModel() == null)
			user.setPointModel(new LinkedList<PointModel>());

		if (user.getHomePoint() == null)
			user.setHomePoint(Point.from(0.0, 0.0));

		if (user.getWorkPoint() == null)
			user.setWorkPoint(Point.from(0.0, 0.0));

		return new UserStatus(user.getPoints(), 120, 130, user.getName(), user.getStatus(), user.getPointsUpdateTime(),
				user.getPointModel(), user.getHomePoint().lat(), user.getHomePoint().lon(), user.getWorkPoint().lat(),
				user.getWorkPoint().lon(), user.getTeam(), user.getTrust());

	}

	public static List<Integer> findDoneUserTasks(MongoDatabase db, String user) {
		List<Integer> transactions = new LinkedList<>();

		MongoCollection<Document> currentCollection = db.getCollection("transactions");
		FindIterable<Document> results = currentCollection.find(eq("userId", user));
		MongoCursor<Document> cursor = results.iterator();

		try {
			while (cursor.hasNext()) {

				Document doc = cursor.next();
				Integer transactionId = doc.getInteger("basedTransactionId", 0);

				// exclude GPS Validations(check-in)
				if (transactionId != 0 && transactionId != -1)
					transactions.add(transactionId);
			}
		} finally {
			cursor.close();
		}
		return transactions;
	}

	public static List<Integer> findDoneUserRoutes(MongoDatabase db, String user) {
		UserStatus status = getUserStatus(db, user);
		List<Integer> doneRoutes = new LinkedList<>();

		MongoCollection<Document> currentCollection = db.getCollection("transactions");
		FindIterable<Document> results = currentCollection.find(eq("userId", user));
		MongoCursor<Document> cursor = results.iterator();

		try {
			while (cursor.hasNext()) {

				Document doc = cursor.next();
				Integer routeId = doc.getInteger("routeId", 0);
				Integer type = doc.getInteger("type");
				String creationDataString = doc.getString("creationTime");
				DateFormat df = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a");

				try {
					Date creationDate = df.parse(creationDataString);
					// older than a week. GPSValidation (check-in) ignored
					if (routeId != 0 && routeId != -1
							&& creationDate.after(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 14))
							&& status.getStatus().getStatusValue() < 30
							&& type != SubmitType.route_gps_Validation.getValue())
						doneRoutes.add(routeId);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		} finally {
			cursor.close();
		}
		return doneRoutes;
	}

	public static List<String> getOpenTransactions(MongoDatabase db, String user) {

		List<String> transactions = new LinkedList<>();
		UserStatus status = getUserStatus(db, user);
		MongoCollection<Document> currentCollection = db.getCollection("transactions");

		FindIterable<Document> results = currentCollection.find(eq("status", 0));
		MongoCursor<Document> cursor = results.iterator();

		try {
			while (cursor.hasNext()) {
				Document doc = cursor.next();
				String json = com.mongodb.util.JSON.serialize(doc);
				transactions.add(json);
			}
		} finally {
			cursor.close();
		}

		return transactions;
	}

	public static String convertToJson(Document doc) {
		String json = com.mongodb.util.JSON.serialize(doc);
		return json;
	}

	public static List<TransactionModel> findModerationTasksCloseToHomeBase(MongoDatabase db, String user) {
		List<TransactionModel> transactions = new LinkedList<>();
		UserStatus status = getUserStatus(db, user);
		MongoCollection<Document> currentCollection = db.getCollection("transactions");
		int maxDistance = 4000;
		if (status.getStatus().getStatusValue() > 0)
			maxDistance += status.getStatus().getStatusValue() * 150;
		String queryString = "{  geometry: { $near :     {     $geometry: { type: \"Point\",  coordinates: [ "
				+ status.getLon() + "," + status.getLat() + " ] },        $minDistance: 0,      $maxDistance: "
				+ maxDistance + "       }     },     status: 0,     userId: {$ne:\"" + user + "\"} }";
		System.out.println(queryString);
		BasicDBObject query = BasicDBObject.parse(queryString);
		FindIterable<Document> find = currentCollection.find(query);
		MongoCursor<Document> cursor = find.iterator();

		try {
			while (cursor.hasNext()) {
				Document doc = cursor.next();
				String json = com.mongodb.util.JSON.serialize(doc);
				TransactionModel transaction = (TransactionModel) gson.fromJson(convertToJson(doc),
						TransactionModel.class);
				System.out.println("found: " + SubmitType.getByValue(transaction.getType()).name());
				if (SubmitType.getByValue(transaction.getType()) == SubmitType.submit_base_indication
						|| SubmitType.getByValue(transaction.getType()) == SubmitType.submit_new_gps_route
						|| SubmitType.getByValue(transaction.getType()) == SubmitType.submit_new_draw_route
						|| SubmitType.getByValue(transaction.getType()) == SubmitType.submit_route_indication) {
					transactions.add(transaction);
				}

			}
		} finally {
			cursor.close();
		}

		return transactions;
	}

	public static void addPointsToUser(MongoDatabase db, int transactionId, String currentUser, int revisorPoints,
			int creationPoints, int submitType, String user) {
		MongoCollection<Document> currentCollection = db.getCollection("users");
		PointModel newPoints = new PointModel(transactionId, creationPoints, revisorPoints, System.currentTimeMillis(),
				submitType);

		BasicDBObject push = (BasicDBObject) JSON.parse(gson.toJson(newPoints));

		Document doc = currentCollection.findOneAndUpdate(eq("_id", new ObjectId(user)),
				Updates.push("pointModel", push));

		if (doc == null)
			return;

		if (updateUserPoints(db, user) == false)
			System.out.println("error");
		else
			System.out.println("points added :-");

	}

	private static boolean updateUserPoints(MongoDatabase db, String user) {
		Document doc;
		UserModel userModel = getUserModel(db, user);
		MongoCollection<Document> currentCollection = db.getCollection("users");
		int points = 0;
		for (PointModel point : userModel.getPointModel()) {
			points += point.getCreatorPoints() + point.getRevisorPoints();
		}
		userModel.setPoints(points);

		doc = currentCollection.findOneAndUpdate(eq("_id", new ObjectId(user)), Updates.set("points", points));
		if (doc == null)
			return false;
		return true;
	}

	public static void changeTransactionPoints(MongoDatabase db, int transactionId, int revisorPoints,
			int creationPoints, String user) {

		MongoCollection<Document> currentCollection = db.getCollection("users");

		BasicDBObject selectQuery = new BasicDBObject("_id", new ObjectId(user));
		selectQuery.append("pointModel.transactionId", transactionId);

		BasicDBObject updateFields = new BasicDBObject();
		updateFields.put("pointModel.$.creatorPoints", creationPoints);
		updateFields.put("pointModel.$.revisorPoints", revisorPoints);

		BasicDBObject updateQuery = new BasicDBObject();
		updateQuery.put("$set", updateFields);

		currentCollection.findOneAndUpdate(selectQuery, updateQuery);

		updateUserPoints(db, user);
	}

	public static void replaceEntry(MongoDatabase db, String collection, String variable, Object value,
			Object replaceEntry) {
		MongoCollection<Document> currentCollection = db.getCollection(collection);

		String json = gson.toJson(replaceEntry);// data is User DTO, just pojo!
		System.out.println(json);

		BasicDBObject updateFields = (BasicDBObject) JSON.parse(json);

		BasicDBObject updateQuery = new BasicDBObject();
		updateQuery.put("$set", updateFields);

		BasicDBObject searchQuery = new BasicDBObject().append(variable, value);

		currentCollection.findOneAndUpdate(searchQuery, updateQuery);
	}

	public static void replaceEntryByJson(MongoDatabase db, String collection, String variable, Object value,
			Object replaceEntry) {
		MongoCollection<Document> currentCollection = db.getCollection(collection);

		String json = gson.toJson(replaceEntry);// data is User DTO, just pojo!
		System.out.println(json);

		BasicDBObject updateFields = (BasicDBObject) JSON.parse(json);

		BasicDBObject updateQuery = new BasicDBObject();
		updateQuery.put("$set", updateFields);

		BasicDBObject searchQuery = new BasicDBObject().append(variable, value);

		currentCollection.findOneAndUpdate(searchQuery, updateFields);
	}

	public static void changeRoute(MongoDatabase db, RouteVersionModel route) {
		MongoCollection<Document> routeCollection = db.getCollection("routes");
		Document myDoc = routeCollection.find(eq("routeId", route.getRouteId())).first();
		if (myDoc != null) {

			BasicDBObject push = (BasicDBObject) JSON.parse(gson.toJson(route));

			Document doc = routeCollection.findOneAndUpdate(eq("routeId", route.getRouteId()),
					Updates.push("routes", push));

			if (doc == null)
				return;

		} else {
			int routeId = 0;
			if (route.getRouteId() <= 0) {
				// search Route Id
				FindIterable<Document> docs = routeCollection.find().sort(new BasicDBObject("routeId", -1)).limit(1);
				if (docs == null) {
					routeId = 1;
				} else {
					routeId = 1 + docs.first().getInteger("routeId");
				}
				Route adaptRoute = new Route(route.getRoute(), routeId);
				route = new RouteVersionModel(adaptRoute, route.getUserId(), route.getCreationMode());
			} else
				routeId = route.getRouteId();
			System.out.println("add new RouteModel with RouteId " + routeId);

			RouteModel model = new RouteModel();
			model.setRouteId(route.getRouteId());
			model.setGeometry(RouteHelper.getGeneralizedGeometry(route.getRoute(), 0.25f));
			model.addToRoutes(route);

			String json = gson.toJson(model);// data is User DTO, just
			// pojo!
			System.out.println(json);

			BasicDBObject document1 = (BasicDBObject) JSON.parse(json);
			routeCollection.insertOne(new Document(document1));

		}
	}

	public static List<RouteVersionModel> getRouteWithVersions(MongoDatabase db, int routeId, int lastVersions) {
		Document doc = documentOfValueInDataBase(db, "routes", "routeId", routeId);
		if (doc == null)
			return null;

		RouteModel model = (RouteModel) JsonHelper.parseJson(convertToJson(doc), RouteModel.class);

		if (lastVersions >= model.getRoutes().size())
			return model.getRoutes();
		else {
			List<RouteVersionModel> versions = new LinkedList<>();
			for (int i = model.getRoutes().size() - 1; i >= model.getRoutes().size() - lastVersions; i--) {
				versions.add(model.getRoutes().get(i));
			}
			return versions;
		}

	}

	public static List<RouteVersionModel> getRoutesLastVersion(MongoDatabase db) {
		MongoCollection<Document> routeCollection = db.getCollection("routes");

		MongoCursor<Document> curs = routeCollection.find().iterator();
		List<RouteVersionModel> routes = new LinkedList<>();
		try {
			while (curs.hasNext()) {
				Document doc = curs.next();

				RouteModel model = (RouteModel) JsonHelper.parseJson(convertToJson(doc), RouteModel.class);
				routes.add(model.getRoutes().get(model.getRoutes().size() - 1));
			}
			return routes;
		} finally {
			curs.close();
		}
	}
	
	
	public static List<FeedbackModel> getFeedbacks(MongoDatabase db,long since,int routeId) {
		MongoCollection<Document> routeCollection = db.getCollection("feedbacks");

		MongoCursor<Document> curs = routeCollection.find().iterator();
		List<FeedbackModel> routes = new LinkedList<>();
		try {
			while (curs.hasNext()) {
				Document doc = curs.next();

				
				FeedbackModel model = (FeedbackModel) JsonHelper.parseJson(convertToJson(doc), FeedbackModel.class);
				if(model.getTimestamp()>since)
				if(routeId >0 && model.getRouteId()==routeId || routeId <= 0)
				{
					routes.add(model);
				}
			}
			return routes;
		} finally {
			curs.close();
		}
	}

}
