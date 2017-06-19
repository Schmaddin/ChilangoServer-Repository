package go.chilan.server;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.graphhopper.chilango.FileHelper;
import com.graphhopper.chilango.network.Constants;

/**
 * Hello world!
 *
 */
public class App {
	private static ExecutorService pool;
	
	public static File baseFolder;

	public static String inputFile="basedirectory.info";

	public static void main(String[] args) throws Exception {
		NetworkService service = null;
		try {
			baseFolder=(File)FileHelper.readObject(new File(inputFile));
			getBaseFolderForTasks();
			pool = Executors.newFixedThreadPool(8);

			try {
				pool.execute(new ConfirmationListener());
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("ConfirmationListener: " + e.getMessage());
			}

			service = new NetworkService(Constants.SERVER_PORT_NUMBER, pool);
			// new Thread(service).start();
			pool.execute(service);

		} catch (IOException e2) {

			service.closeDown();
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		/*
		 * String user = "martin.wuerflein@gmx.de"; String token =
		 * DBConnector.createToken(user); ConnectionMessage message =
		 * DBConnector.prooveToken(token, user);
		 * System.out.println(message.getInfoConnection().name() + " " +
		 * message.getAdditionalField());
		 * 
		 * Connection myCon = null; try { myCon =
		 * DBConnector.createDatabaseConnection(); } catch
		 * (ClassNotFoundException e1) { // TODO Auto-generated catch block
		 * e1.printStackTrace(); } catch (SQLException e1) { // TODO
		 * Auto-generated catch block e1.printStackTrace(); }
		 */

		// Create User Table
		/*
		 * try { DBConnector.makeStatement(myCon, "DROP TABLE USERS");
		 * DBConnector.makeStatement(myCon,
		 * "CREATE TABLE USERS (ID INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),MAIL VARCHAR(50) NOT NULL,CONFIRMATION BOOLEAN NOT NULL, PW VARCHAR(12) NOT NULL, NAME VARCHAR(20) NOT NULL, PRIMARY KEY (ID), UNIQUE (MAIL),UNIQUE (NAME))"
		 * ); } catch (SQLException e2) { // TODO Auto-generated catch block
		 * e2.printStackTrace(); }
		 */
		/*
		 * try { DBConnector.makeStatement(myCon,
		 * "CREATE TABLE TOKEN (ID INTEGER NOT NULL,AUTHKEY VARCHAR(350) NOT NULL,UNIQUE (AUTHKEY),PRIMARY KEY (ID))"
		 * ); } catch (SQLException e1) { // TODO Auto-generated catch block
		 * e1.printStackTrace(); }
		 */
		/*
		 * try {
		 * DBConnector.createUser(myCon,"Schmaddin","martin.wuerflein@gmx.de",
		 * "chilanGoes"); } catch (Exception e1) { // TODO Auto-generated catch
		 * block e1.printStackTrace(); }
		 */
		/*
		 * message = DBConnector.loginUser(myCon, user, "chilanGoes");
		 * System.out.println(message.getInfoConnection().name() + " " +
		 * message.getAdditionalField());
		 * 
		 * try { myCon.close(); } catch (SQLException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); }
		 */

		// CREATE TABLE TRANSACTIONS (ID INTEGER NOT NULL GENERATED ALWAYS AS
		// IDENTITY (START WITH 1, INCREMENT BY 1),PATH VARCHAR(50) NOT
		// NULL,USERID INTEGER NOT NULL,TRANSACTION_TYPE INTEGER NOT NULL, TIME
		// DATE NOT NULL, STATUS INTEGER NOT NULL, PRIMARY KEY (ID))

		System.out.println("SimpleApp finished");

		boolean running = true;

		try {
			int i = System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("closing server");

		service.closeDown();
	}

	public static String getBaseFolderForTasks() {
		File taskPath=new File(baseFolder,"ChilangoSubmits");
		if(!taskPath.exists())
			taskPath.mkdirs();
		return taskPath.getAbsolutePath();
	}

}
