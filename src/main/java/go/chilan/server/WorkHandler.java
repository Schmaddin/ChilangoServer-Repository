package go.chilan.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;

import com.graphhopper.chilango.FileHelper;
import com.graphhopper.chilango.network.ConnectionMessage;
import com.graphhopper.chilango.network.ConnectionMessage.ConnectionInformation;
import com.graphhopper.chilango.network.EasyCrypt;
import com.graphhopper.chilango.network.RequestMessage;
import com.graphhopper.chilango.network.ServerMessageAuth;

class WorkHandler implements Runnable {
	private Socket socket;
	private final EasyCrypt cryption;
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;
	private Connection connection;
	private DBHelper helper;
	/*
	 * WorkHandler(Socket socket, EasyCrypt cryption) { this.socket = socket;
	 * this.cryption = cryption;
	 * 
	 * connection = null; try { connection =
	 * DBConnector.createDatabaseConnection();
	 * System.out.println("Database Connection created"); } catch
	 * (ClassNotFoundException e1) { // TODO Auto-generated catch block
	 * e1.printStackTrace(); } catch (SQLException e) { // TODO Auto-generated
	 * catch block e.printStackTrace(); } }
	 */
	private ServerSocket serverSocket;

	WorkHandler(ServerSocket socket, EasyCrypt cryption) {
		// this.socket = socket;
		this.serverSocket = socket;
		this.cryption = cryption;

		connection = null;
		try {
			connection = DBConnector.createDatabaseConnection();
			System.out.println("database connection: "+!connection.isClosed() + " (Server)");
			System.out.println("Database Connection created");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {

		System.out.println("new incoming connection");

		try

		{
			System.out.println("initialisation of connection");

			inputStream = new ObjectInputStream(
					/* cryption.decryptInputStream( */socket.getInputStream()/* ) */);
			System.out.println("inputStream created");

			ServerMessageAuth auth = (ServerMessageAuth) FileHelper.readCryptedObject(inputStream, cryption);

			System.out.println("read object...decryption done");

			outputStream = null;
			outputStream = new ObjectOutputStream(
					/* cryption.encryptOutputStream( */socket.getOutputStream()/* ) */);
			outputStream.flush();
			helper = new DBHelper(connection);
			boolean authCorrect = handleAuthRequest(auth);

			if (authCorrect) {
				int user = helper.getUserId(auth.getMail());
				UserRequestHandler handler = new UserRequestHandler(helper, inputStream, outputStream, user, cryption);

				System.out.println("user: " + user + " can do operation");

				boolean acceptIfnformation = true;
				while (acceptIfnformation) {

					RequestMessage request = (RequestMessage) FileHelper.readCryptedObject(inputStream, cryption);
					int response = handler.handleRequest(request);
					acceptIfnformation = response == -1 ? false : true;

				}

			}

			if (outputStream != null)
				outputStream.close();

			if (inputStream != null)
				inputStream.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private boolean handleAuthRequest(ServerMessageAuth auth) throws Exception {
		switch (auth.getInformation()) {
		case LOG_IN:
			System.out.println("log in user");
			ConnectionMessage message = helper.logIn(auth);
			FileHelper.writeCryptedObject(outputStream, cryption, message);
			if (message.getInfoConnection() == ConnectionInformation.LOGIN_OK)
				return true;
			else
				return false;

		case LOG_IN_TOKEN:
			System.out.println("log in user by token");
			message = helper.logIn(auth);
			FileHelper.writeCryptedObject(outputStream, cryption, message);

			if (message.getInfoConnection() == ConnectionInformation.CORRECT_TOKEN)
				return true;
			else
				return false;

		case CREATE_USER:
			message = helper.createUser(auth);
			FileHelper.writeCryptedObject(outputStream, cryption, message);

			return false;
		default:
			return false;
		}
	}

	public void runTask() throws IOException {
		// TODO Auto-generated method stub

		do {
			socket = serverSocket.accept();

			run();

		} while (true);

		/*
		if (connection != null)
			if (!connection.isClosed())
				connection.close();*/
	}

}
