package go.chilan.server;

import static com.mongodb.client.model.Filters.eq;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bson.Document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.graphhopper.chilango.FileHelper;
import com.graphhopper.chilango.data.database.UserModel;
import com.graphhopper.chilango.network.Constants;
import com.graphhopper.chilango.network.ServerMessageAuth;
import com.graphhopper.chilango.network.ConnectionMessage;
import com.graphhopper.chilango.network.ConnectionMessage.ConnectionInformation;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;

/**
 * Hello world!
 *
 */
public class App {
	private static ExecutorService pool;

	public static File baseFolder;

	public static String inputFile = "basedirectory.info";

	public static void main(String[] args) throws Exception {

		// DBHelper helper = new DBHelper();
		/*
		 * ConnectionMessage message = helper.createUser(new
		 * ServerMessageAuth("Schmaddin", "chilanGoes",
		 * "martin.wuerflein@gmx.de", null, ConnectionInformation.CREATE_USER));
		 * / System.out.println(message.getInfoConnection().name() + " " +
		 * message.getAdditionalField());
		 */
		// System.out.println("validation:
		// +"+helper.validateMail("82b54d87-4e0d-477a-bed9-b6681080c04f"));

		/*
		 * message=helper.logIn(new ServerMessageAuth("Schmaddin", "",
		 * "martin.wuerflein@gmx.de",
		 * "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vY2hpbGFuZ28ubWUiLCJzdWIiOiJ1c2Vycy9tYXJ0aW4ud3VlcmZsZWluQGdteC5kZSIsImV4cCI6MTQ5ODEzNjA2NH0.Tq22NOBkZUaoz6PUxQJtSQqC5kwWW8XZOrofVsdfXYv0",
		 * ConnectionInformation.LOG_IN_TOKEN));
		 * System.out.println(message.getInfoConnection().name() + " " +
		 * message.getAdditionalField());
		 */

		NetworkService service = null;
		try {
			baseFolder = (File) FileHelper.readObject(new File(inputFile));
			getBaseFolderForTasks();
			pool = Executors.newFixedThreadPool(8);

			service = new NetworkService(Constants.SERVER_PORT_NUMBER, pool);
			// new Thread(service).start();
			pool.execute(service);

		} catch (IOException e2) {

			e2.printStackTrace();

		}

		do {
		} while (true);

	}

	public static String getBaseFolderForTasks() {
		File taskPath = new File(baseFolder, "ChilangoSubmits");
		if (!taskPath.exists())
			taskPath.mkdirs();

		System.out.println(baseFolder + "  " + taskPath.getAbsolutePath());
		return taskPath.getAbsolutePath();
	}

}
