package go.chilan.server;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.mail.MessagingException;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.jts.JtsAdapterFactory;
import com.github.filosganga.geogson.model.Geometry;
import com.github.filosganga.geogson.model.Point;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.graphhopper.chilango.FileHelper;
import com.graphhopper.chilango.data.Feedback;
import com.graphhopper.chilango.data.JsonHelper;
import com.graphhopper.chilango.data.ModerationTask;
import com.graphhopper.chilango.data.RouteQuestionary;
import com.graphhopper.chilango.data.UserStatus;
import com.graphhopper.chilango.data.database.FeedbackModel;
import com.graphhopper.chilango.data.database.PointModel;
import com.graphhopper.chilango.data.database.QuestionaryModel;
import com.graphhopper.chilango.data.database.RouteVersionModel;
import com.graphhopper.chilango.data.database.SubmitType;
import com.graphhopper.chilango.data.database.SubmitTypeInterface;
import com.graphhopper.chilango.data.database.TransactionModel;
import com.graphhopper.chilango.data.database.UserModel;
import com.graphhopper.chilango.network.ConnectionMessage;
import com.graphhopper.chilango.network.ConnectionMessage.ConnectionInformation;
import com.graphhopper.chilango.network.ServerMessageAuth;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

public class DBHelper {

	private Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GeometryAdapterFactory())
			.registerTypeAdapterFactory(new JtsAdapterFactory()).create();
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

	public boolean addTransaction(String path, SubmitType type, String userId, int status, int routeId,
			Date creationTime, byte trust, Geometry geometry, int transactionId, int baseTransactionId)
			throws MessagingException {
		return MongoDB.addTransaction(db, path, type, userId, status, routeId, creationTime, trust, geometry,
				transactionId, baseTransactionId);
	}

	public boolean validateMail(String conf) {

		return MongoDB.updateValueInTable(db, "users", "mail_confirmation", true, "mailConfirmationToken", conf);

	}

	public void close() {
		connection.close();
	}

	public UserStatus getUserStatus(String id, long time) {
		UserStatus status = MongoDB.getUserStatus(db, id);
		if (status == null)
			return null;

		List<PointModel> filtered = new LinkedList<>();
		for (PointModel points : status.getPointSinceUpdate()) {
			if (time - points.getTime() > 0)
				filtered.add(points);
		}
		return new UserStatus(status.getPoints(), status.getPlaceExplore(), status.getPlaceRevise(), status.getName(),
				status.getStatus().getStatusValue(), status.getPointsUpdate(), filtered, status.getLat(),
				status.getLon(), status.getWorkLat(), status.getWorkLon(), status.getTeam(), status.getTrust());
	}

	public void resetUser() {
		userId = null;

	}

	public void changeUserPosition(Point information, String id) {
		MongoDB.updateValueThroughJsonInTableById(db, "users", "homePoint",
				JsonHelper.createJsonFromObject(information), id);
	}

	public void changeUserWorkPosition(Point information, String id) {
		MongoDB.updateValueThroughJsonInTableById(db, "users", "workPoint",
				JsonHelper.createJsonFromObject(information), id);
	}

	public void changeUserName(String name, String id) {
		MongoDB.updateValueInTableById(db, "users", "name", name, id);
	}

	public void changeUserTrust(byte trust, String id) {
		MongoDB.updateValueInTableById(db, "users", "trust", trust, id);
	}

	public void changeUserTeam(int team, String id) {
		MongoDB.updateValueInTableById(db, "users", "team", team, id);
	}

	public List<ModerationTask> findModerationTasks(String user) {
		List<Integer> exclude = MongoDB.findDoneUserTasks(db, user);
		System.out.println("exclude of user: " + user + " elements excludeded:" + exclude.size());
		List<TransactionModel> transactions = MongoDB.findModerationTasksCloseToHomeBase(db, user);

		System.out.println("number of tasks: " + transactions.size());
		List<ModerationTask> moderationTasks = new LinkedList<>();
		for (TransactionModel transaction : transactions) {
			if (transaction.getPath() != null) {
				if (!exclude.contains(transaction.getTransactionId())) {
					SubmitTypeInterface type = (SubmitTypeInterface) FileHelper
							.readObject(new File(transaction.getPath()));
					moderationTasks.add(
							new ModerationTask(type, (long) transaction.getTransactionId(), transaction.getTrust()));
				}
			}
		}

		return moderationTasks;
	}

	public List<Integer> findUserExcludeList(String user) {
		return MongoDB.findDoneUserRoutes(db, user);

	}

	public int createTransactionId() {

		return MongoDB.createNewTransactionId(db);
	}

	public void addPointsToUser(int transactionId, String currentUser, int revisorPoints, int creationPoints,
			int submitType) {
		MongoDB.addPointsToUser(db, transactionId, currentUser, revisorPoints, creationPoints, submitType, userId);
	}

	public void changeTransactionPoints(int transactionId, int revisorPoints, int creationPoints, String user) {
		MongoDB.changeTransactionPoints(db, transactionId, revisorPoints, creationPoints, user);
	}

	public List<String> getOpenTransactions() {
		return MongoDB.getOpenTransactions(db, userId);
	}

	public String getSubmit(Integer id) {
		if (!checkAdmin())
			return null;

		Document doc = MongoDB.documentOfValueInDataBase(db, "transactions", "transactionId", id);
		System.out.println("document found");
		String entry = MongoDB.convertToJson(doc);

		TransactionModel transaction = (TransactionModel) JsonHelper.parseJson(entry, TransactionModel.class);
		System.out.println("transaction");
		if (transaction.getPath() != null) {
			SubmitTypeInterface type = (SubmitTypeInterface) FileHelper.readObject(new File(transaction.getPath()));
			String json = gson.toJson(type);
			System.out.println("created: " + json);
			return json;
		}
		return null;
	}

	private boolean checkAdmin() {
		return true;
	}

	public void changeTransaction(String json) {
		if (!checkAdmin())
			return;
		TransactionModel model = (TransactionModel) JsonHelper.parseJson(json, TransactionModel.class);
		MongoDB.replaceEntry(db, "transactions", "transactionId", model.getTransactionId(), model);
	}

	public String requestUserById(String id) {
		if (!checkAdmin())
			return null;

		return MongoDB.convertToJson(MongoDB.documentOfValueInDataBase(db, "users", "_id", new ObjectId(id)));
	}

	public void changeUser(String json) {
		if (!checkAdmin())
			return;

		UserModel model = (UserModel) JsonHelper.parseJson(json, UserModel.class);
		MongoDB.replaceEntry(db, "users", "mail", model.getMail(), model);
	}

	public void addRoute(RouteVersionModel route) {
		MongoDB.changeRoute(db, route);

	}

	public List<RouteVersionModel> getRoutes(ArrayList<Integer> information) {
		if (information == null) {
			// still ignored (filter routes by number)
		}
		return MongoDB.getRoutesLastVersion(db);

	}

	public List<RouteVersionModel> getRoute(ArrayList<Integer> information) {
		if (information == null) {
			return null;
		}
		int versions = 10;
		if (information.size() > 1)
			versions = information.get(1);
		return MongoDB.getRouteWithVersions(db, information.get(0), versions);

	}

	public boolean addQuestionary(Feedback feedback) {
		if (!checkAdmin())
			return false;
		QuestionaryModel model = new QuestionaryModel(feedback.getQuestionary(), feedback.getRouteId(),
				feedback.getTransactionId());
		return MongoDB.addDocumentByObject(db, "questionary", model, "transactionId", feedback.getTransactionId());

	}

	public boolean addFeedback(Feedback feedback) {
		if (!checkAdmin())
			return false;
		return MongoDB.addDocumentByObject(db, "feedbacks", feedback, "transactionId", feedback.getTransactionId());

	}

	public void changeFeedback(FeedbackModel feedbackModel) {
		if (!checkAdmin())
			return;
		MongoDB.replaceEntry(db, "feedbacks", "transactionId", feedbackModel.getTransactionId(), feedbackModel);
	}

	public String getFeedback(int transactionId) {

		return MongoDB
				.convertToJson(MongoDB.documentOfValueInDataBase(db, "feedbacks", "transactionId", transactionId));

	}

	public List<FeedbackModel> getFeedbacks(ArrayList<Long> arrayList) {
		if (!checkAdmin())
			return null;

		long since = 0;
		long routeId = 0;
		if (arrayList != null)
			if (arrayList.size() > 0) {
				since = arrayList.size() > 1 ? arrayList.get(1) : 0;

				routeId = arrayList.get(0);
			}

		return MongoDB.getFeedbacks(db, since, (int) routeId);

	}
}
