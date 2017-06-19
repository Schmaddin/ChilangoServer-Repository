package go.chilan.server;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;

import com.graphhopper.chilango.FileHelper;
import com.graphhopper.chilango.data.ExplicitRouteMessage;
import com.graphhopper.chilango.network.Constants;
import com.graphhopper.chilango.network.EasyCrypt;
import com.graphhopper.chilango.network.RequestMessage;
import com.graphhopper.chilango.network.RequestType;
import com.graphhopper.chilango.network.ServerConnection;
import com.graphhopper.chilango.tasks.BusBaseTask;
import com.graphhopper.chilango.tasks.ChilangoTask;
import com.graphhopper.chilango.tasks.DrawRouteTask;
import com.graphhopper.chilango.tasks.GPSRecordTask;
import com.graphhopper.chilango.tasks.RecordTask;

import com.graphhopper.chilango.network.livedata.LiveRideUser;
import go.chilan.server.LiveDataHandler;

public class UserRequestHandler {

	private DBHelper helper;
	private ObjectInputStream inputStream;
	private int user;
	private ObjectOutputStream outputStream;
	private EasyCrypt cryption;
	private String taskPath;

	public UserRequestHandler(DBHelper helper, ObjectInputStream inputStream, ObjectOutputStream outputStream, int user,
			EasyCrypt cryption) {
		this.helper = helper;
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.user = user;
		this.cryption = cryption;
	}

	public int handleRequest(RequestMessage request) throws Exception {
		taskPath=App.getBaseFolderForTasks();
		
		switch (request.getType()) {
		case PutLiveGPS:
			LiveRideUser ride = (LiveRideUser) request.getInformation();
			LiveDataHandler.addValue(new LiveRideUser(ride.getLat(), ride.getLon(), ride.getTimeStamp(),
					ride.getTransportId(), ride.getHeading(),user, ride.isGame()));
			return -1;

		case GetLiveGPS:
			RequestMessage message = new RequestMessage(RequestType.GetLiveGPS, LiveDataHandler.getRides());
			FileHelper.writeCryptedObject(outputStream, cryption, message);
			return -0;

		case SubmitTask:
			ChilangoTask task = (ChilangoTask) request.getInformation();
			// process
			String path = (taskPath +"/"+ FileHelper.recoverDate.format(new Date(System.currentTimeMillis())) + "-"
					+ task.getType() + "-" + user) + ".task";

			Long transactionId = (long) helper.addTransaction(path, Constants.TRANSACTION_TASK);

			if (task instanceof GPSRecordTask) {
				task = new GPSRecordTask((GPSRecordTask) task, System.currentTimeMillis(), transactionId);
			} else if (task instanceof DrawRouteTask) {
				task = new DrawRouteTask((DrawRouteTask) task, System.currentTimeMillis(), transactionId);
			} else if (task instanceof RecordTask) {
				task = new RecordTask((RecordTask) task, System.currentTimeMillis(), transactionId);
			} else if (task instanceof BusBaseTask) {
				task = new BusBaseTask((BusBaseTask) task, System.currentTimeMillis(), transactionId);
			}

			FileHelper.writeObject(new File(path), task);
			FileHelper.writeCryptedObject(outputStream, cryption,
					new RequestMessage(RequestType.TransactionConfrimation, transactionId));
			return 0;
		case SubmitChange:

			HashMap<String, ExplicitRouteMessage> taskMap = (HashMap<String, ExplicitRouteMessage>) request
					.getInformation();
			HashMap<String, Long> transactionResultList = new HashMap<>();
			for (String currentPath : taskMap.keySet()) {
				ExplicitRouteMessage currentTask = taskMap.get(currentPath);
				
				
				// process
				path = (taskPath +"/"+ FileHelper.recoverDate.format(new Date(System.currentTimeMillis())) + "-"
						+ currentTask.getType() + "-" + user) + ".explicit";

				transactionId = (long) helper.addTransaction(path, Constants.TRANSACTION_MESSAGE);
				System.out.println("new task saved to: " + transactionId);
				// write object to folder
				FileHelper.writeObject(new File(path), new ExplicitRouteMessage(currentTask, transactionId));
				transactionResultList.put(currentPath,transactionId);
			}
			FileHelper.writeCryptedObject(outputStream, cryption,
					new RequestMessage(RequestType.TransactionConfrimation, transactionResultList));

			return 0;

		case close:
			System.out.println("close conncection to user: " + user);
			return -1;
		}

		return -1;
	}

}
