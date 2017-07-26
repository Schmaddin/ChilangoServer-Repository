package go.chilan.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.graphhopper.chilango.FileHelper;
import com.graphhopper.chilango.network.ConnectionMessage;
import com.graphhopper.chilango.network.Constants;
import com.graphhopper.chilango.network.EasyCrypt;
import com.graphhopper.chilango.network.ServerConnection;
import com.graphhopper.chilango.network.ServerMessageAuth;
import com.graphhopper.chilango.network.ConnectionMessage.ConnectionInformation;

public class ConfirmationListener implements Runnable {

	private EasyCrypt ec;

	List<Long> lastRequestTimeStamps = new LinkedList<>();

	public static void main(String[] args) throws Exception {
		ConfirmationListener listener = new ConfirmationListener();
		listener.run();
	}

	@Override
	public void run() {
		System.out.println("ConfirmationListener: starts");

		ServerSocket server;
		try {
			System.out.println("try");
			ec = new EasyCrypt(null, EasyCrypt.aes);
			ec.setKey(ec.readKey(new File("aes.key")));

			server = new ServerSocket(Constants.PORT_MAIL_CHECK_INTERN);

			System.out.println("ConfirmationListener: ServerSocket created");

			System.out.println("Listening for connection on port " + Constants.PORT_MAIL_CHECK_INTERN + " ....");

			do {
				try (Socket socket = server.accept()) {
					System.out.println("client accepted");

					InputStreamReader isr = new InputStreamReader(socket.getInputStream());
					BufferedReader reader = new BufferedReader(isr);
					String request = reader.readLine(); // Now you get GET
														// index.html
														// HTTP/1.1`
					String[] requestParam = request.split(" ");
					String conf = (requestParam[1]).substring(1);
					System.out.println("value: " + conf);

					String response = "wrong confirmation";

					boolean locked = checkRequests();

					if (!locked) {
						lastRequestTimeStamps.add(System.currentTimeMillis());
						try {
							ServerConnection connection = new ServerConnection(ec, new ServerMessageAuth("", "", "",
									conf, ConnectionMessage.ConnectionInformation.VERFIY_MAIL), "server");

							ConnectionMessage validated = (ConnectionMessage) connection.getConnectionMessage();

							if (validated.getInfoConnection() == ConnectionMessage.ConnectionInformation.CORRECT_TOKEN)
								response = "mail confirmed";
						} catch (IOException e) {
							e.printStackTrace();
							response = "Server error - confirmation could not be performed";
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else
						response = "too many requests. Server denied call. Try later!";

					String httpResponse = "HTTP/1.1 200 OK\r\n\r\n" + response;
					socket.getOutputStream().write(httpResponse.getBytes("UTF-8"));

					reader.close();
				}
			} while (true);
		} catch (

		IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private boolean checkRequests() {
		long time = System.currentTimeMillis();
		Iterator<Long> iterator = lastRequestTimeStamps.iterator();
		while (iterator.hasNext()) {
			long value = iterator.next();
			if (time - value > 10000) {
				iterator.remove();
			}

		}
		if (lastRequestTimeStamps.size() > 3)
			return true;
		else
			return false;
	}

}
