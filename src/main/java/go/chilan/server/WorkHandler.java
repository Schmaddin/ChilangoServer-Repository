package go.chilan.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.github.filosganga.geogson.model.Point;
import com.graphhopper.chilango.FileHelper;
import com.graphhopper.chilango.network.ConnectionMessage;
import com.graphhopper.chilango.network.ConnectionMessage.ConnectionInformation;
import com.graphhopper.chilango.network.EasyCrypt;
import com.graphhopper.chilango.network.RequestMessage;
import com.graphhopper.chilango.network.ServerMessageAuth;
import com.vividsolutions.jts.geom.Coordinate;

class WorkHandler implements Runnable {
	private Socket socket;
	private final EasyCrypt cryption;
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;
	private DBHelper helper;

	private ServerSocket serverSocket;

	WorkHandler(ServerSocket socket, EasyCrypt cryption) {
		// this.socket = socket;
		this.serverSocket = socket;
		this.cryption = cryption;
		helper = new DBHelper();
	}

	public void run() {

		System.out.println("new incoming connection");

		try {
			helper.resetUser();

			System.out.println("initialisation of connection");

			inputStream = new ObjectInputStream(socket.getInputStream());
			System.out.println("inputStream created");

			ServerMessageAuth auth = (ServerMessageAuth) FileHelper.readCryptedObject(inputStream, cryption);

			System.out.println("read object...decryption done");

			outputStream = null;
			outputStream = new ObjectOutputStream(socket.getOutputStream());

			outputStream.flush();

			boolean authCorrect = handleAuthRequest(auth);

			if (auth.getInformation() == ConnectionInformation.VERFIY_MAIL) {
				if (authCorrect)
					FileHelper.writeCryptedObject(outputStream, cryption,
							new ConnectionMessage(ConnectionInformation.CORRECT_TOKEN));
				else
					FileHelper.writeCryptedObject(outputStream, cryption,
							new ConnectionMessage(ConnectionInformation.WRONG_TOKEN));

			} else if (authCorrect) {
				String user = helper.getUserId(auth.getMail());
				UserRequestHandler handler = new UserRequestHandler(helper, inputStream, outputStream, user, cryption);

				System.out.println("user: " + user + " can do operation");

				boolean acceptInformation = true;
				while (acceptInformation) {

					RequestMessage request = (RequestMessage) FileHelper.readCryptedObject(inputStream, cryption);
					System.out.println("request received: "+request.getType().name());
					int response = handler.handleRequest(request);
					acceptInformation = response == -1 ? false : true;

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

		case VERFIY_MAIL:
			System.out.println("verify mail...");
			Boolean validated = helper.validateMail(auth.getAuth());
			return validated;

		case CREATE_USER:
			message = helper.createUser(auth);
			FileHelper.writeCryptedObject(outputStream, cryption, message);

			ArrayList<Coordinate> coordinates = (ArrayList<Coordinate>) FileHelper.readCryptedObject(inputStream, cryption);
			System.out.println("read1");
			
			helper.changeUserPosition(Point.from(coordinates.get(0).y, coordinates.get(0).x),
					helper.getUserId(auth.getMail()));
			if (coordinates.size() > 1)
				helper.changeUserWorkPosition(Point.from(coordinates.get(0).y, coordinates.get(0).x),
						helper.getUserId(auth.getMail()));

			Integer team = (Integer) FileHelper.readCryptedObject(inputStream, cryption);
			helper.changeUserTeam(team,
					helper.getUserId(auth.getMail()));
			
			System.out.println("read2: team: "+team);
			
			return false;
			
		default:
			return false;
		}
	}

	// seriel working
	public void runTask() {

		do {
			try {
				socket = serverSocket.accept();

				run();
			} catch (IOException e) {

			} finally {
				try {
					if (socket != null) {
						System.out.println("socket close");
						socket.close();
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		} while (true);

	}

}
