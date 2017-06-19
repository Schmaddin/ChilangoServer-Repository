package go.chilan.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;

import com.graphhopper.chilango.network.Constants;

public class ConfirmationListener implements Runnable {

	@Override
	public void run() {
		System.out.println("ConfirmationListener: starts");

		ServerSocket server;
		try {
			System.out.println("try");
			server = new ServerSocket(Constants.PORT_MAIL_CHECK);
			System.out.println("ConfirmationListener: ServerSocket created");

			System.out.println("Listening for connection on port " + Constants.PORT_MAIL_CHECK + " ....");

			do {
				try (Socket socket = server.accept()) {
					System.out.println("client accepted");

					InputStreamReader isr = new InputStreamReader(socket.getInputStream());
					BufferedReader reader = new BufferedReader(isr);
					String request = reader.readLine(); // Now you get GET
														// index.html
														// HTTP/1.1`
					String[] requestParam = request.split(" ");
					String conf = requestParam[1];
					Connection connection = DBConnector.createDatabaseConnection();
					System.out.println(
							"database connection: " + !connection.isClosed() + " " + "  (mail confirmation listener)");
					DBHelper helper = new DBHelper(connection);
					boolean validated = helper.validateMail(conf.replaceAll("/", ""));

					String response = "wrong confirmation";
					if (validated)
						response = "mail confirmed";

					String httpResponse = "HTTP/1.1 200 OK\r\n\r\n" + response;
					socket.getOutputStream().write(httpResponse.getBytes("UTF-8"));

					reader.close();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} while (true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}
