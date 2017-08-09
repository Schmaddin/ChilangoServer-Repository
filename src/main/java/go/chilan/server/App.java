package go.chilan.server;


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



		NetworkService service = null;
		try {
			baseFolder = (File) FileHelper.readObject(new File(inputFile));
			getBaseFolderForTasks();
			pool = Executors.newFixedThreadPool(8);

			service = new NetworkService(Constants.SERVER_PORT_NUMBER_INTERN, pool);
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

		return taskPath.getAbsolutePath();
	}

	public static String getBaseFolderForCreation() {
		
		File taskPath = new File(baseFolder, "creation");
		if (!taskPath.exists())
			taskPath.mkdirs();

		return taskPath.getAbsolutePath();
	}
	
}
