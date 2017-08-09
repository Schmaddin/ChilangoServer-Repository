package go.chilan.server;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.github.filosganga.geogson.model.Geometry;
import com.github.filosganga.geogson.model.Point;
import com.google.gson.reflect.TypeToken;
import com.graphhopper.chilango.FileHelper;
import com.graphhopper.chilango.data.Feedback;
import com.graphhopper.chilango.data.JsonHelper;
import com.graphhopper.chilango.data.ModerationTask;
import com.graphhopper.chilango.data.UserStatus;
import com.graphhopper.chilango.data.database.RouteVersionModel;
import com.graphhopper.chilango.data.database.SubmitTypeInterface;
import com.graphhopper.chilango.data.database.FeedbackModel;
import com.graphhopper.chilango.network.Constants;
import com.graphhopper.chilango.network.EasyCrypt;
import com.graphhopper.chilango.network.RequestMessage;
import com.graphhopper.chilango.network.RequestType;
import com.graphhopper.chilango.tasks.BusBaseTask;
import com.graphhopper.chilango.tasks.ChilangoTask;
import com.graphhopper.chilango.tasks.DrawRouteTask;
import com.graphhopper.chilango.tasks.GPSRecordTask;
import com.graphhopper.chilango.tasks.RecordTask;
import com.graphhopper.chilango.tasks.TaskHelper;
import com.vividsolutions.jts.geom.Coordinate;
import com.graphhopper.chilango.network.livedata.LiveRideUser;
import go.chilan.server.LiveDataHandler;

public class UserRequestHandler {

	private DBHelper helper;
	private ObjectInputStream inputStream;
	private String user;
	private ObjectOutputStream outputStream;
	private EasyCrypt cryption;
	private String taskPath;

	public UserRequestHandler(DBHelper helper, ObjectInputStream inputStream, ObjectOutputStream outputStream,
			String user, EasyCrypt cryption) {
		this.helper = helper;
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.user = user;
		this.cryption = cryption;
	}

	public int handleRequest(RequestMessage request) throws Exception {
		String info = request.getInformation();
		taskPath = App.getBaseFolderForTasks();	
		InputProcesser inputProcessor = new InputProcesser(helper, user);

		Type type;
		switch (request.getType()) {
		case SubmitError:
			String path = (taskPath + "/" + FileHelper.df.format(new Date(System.currentTimeMillis())) + "-" + user)
					+ ".html";

			type = new TypeToken<List<String>>(){}.getType();
			//List<String> errorLines = (List<String>) request.getInformation();
			List<String> errorLines = (List<String>) JsonHelper.parseJsonAndroid(info, type);
			
			Files.write(Paths.get(path), errorLines);
			return -1;

		case PutLiveGPS:
			//LiveRideUser ride = (LiveRideUser) request.getInformation();
			LiveRideUser ride = (LiveRideUser) JsonHelper.parseJsonAndroid(info, LiveRideUser.class);
			
			
			LiveDataHandler.addValue(new LiveRideUser(ride.getLat(), ride.getLon(), ride.getTimeStamp(),
					ride.getTransportId(), ride.getHeading(), user, ride.isGame()));
			
			return -1;

		case GetLiveGPS:
			RequestMessage message = new RequestMessage(RequestType.GetLiveGPS, LiveDataHandler.getRides());
			FileHelper.writeCryptedObject(outputStream, cryption, message);
			return 0;

		case SubmitTask:
//			ChilangoTask task = (ChilangoTask) request.getInformation();
			System.out.println(info);
			SubmitTypeInterface get = (SubmitTypeInterface) JsonHelper.parseJsonAndroid(info, SubmitTypeInterface.class);
			ChilangoTask task=(ChilangoTask)get;
			// process
			path = (taskPath + "/" + FileHelper.recoverDate.format(new Date(System.currentTimeMillis())) + "-"
					+ task.getType() + "-" + user) + ".task";

			int routeId = 0;
			byte trust = ServerLogic.calculateTrust(helper, user, TaskHelper.getGeometry(task));

			int transactionId = (int) helper.createTransactionId();
			int status = inputProcessor.handleTask(task, (int) transactionId);
			boolean done = helper.addTransaction(path, task.getType(), user, status, routeId,
					new Date(task.getLastEdit()), trust, TaskHelper.getGeneralizedGeometry(task, 0.25f),
					(int) transactionId, -1);

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
			if (!done)
				transactionId = 0;
			FileHelper.writeCryptedObject(outputStream, cryption,
					new RequestMessage(RequestType.TransactionConfirmation, transactionId));
			return 0;

		case SubmitFeedback:
//			HashMap<String, Feedback> feedbackMap = (HashMap<String, Feedback>) request
//					.getInformation();
			type = new TypeToken<HashMap<String,Feedback>>(){}.getType();
			HashMap<String, Feedback> feedbackMap = (HashMap<String, Feedback>)JsonHelper.parseJsonAndroid(info, type);
			
			HashMap<String, Integer> transactionResultList = new HashMap<>();
			for (String currentPath : feedbackMap.keySet()) {
				Feedback currentFeedback = feedbackMap.get(currentPath);
				

				// process
				path = (taskPath + "/" + FileHelper.recoverDate.format(new Date(System.currentTimeMillis())) + "-"
						+ currentFeedback.getType() + "-" + user) + ".explicit";

				// calculates trust depending on submitting position
				Point point = Point.from(feedbackMap.get(currentPath).getLon(), feedbackMap.get(currentPath).getLat());
				trust = ServerLogic.calculateTrust(helper, user, point);

				transactionId = (int) helper.createTransactionId();
				status = inputProcessor.handleFeedback(currentFeedback, (int) transactionId);
				done = helper.addTransaction(path, currentFeedback.getType(), user, status, currentFeedback.getRouteId(),
						new Date(currentFeedback.getTimestamp()), trust, null, (int) transactionId, -1);
				System.out.println("new task saved to: " + transactionId);

				// write object to folder
				FileHelper.writeObject(new File(path), new Feedback(currentFeedback, transactionId));

				if (!done) {
					transactionId = 0;
				}
				transactionResultList.put(currentPath, transactionId);

			}
			FileHelper.writeCryptedObject(outputStream, cryption,
					new RequestMessage(RequestType.TransactionConfirmation, transactionResultList));

			return 0;
		case RequestRouteExcludeList:
			FileHelper.writeCryptedObject(outputStream, cryption, new RequestMessage(
					RequestType.RequestRouteExcludeList, new LinkedList<Integer>(helper.findUserExcludeList(user))));
			break;
		case RequestModerationTasks:
			FileHelper.writeCryptedObject(outputStream, cryption, new RequestMessage(RequestType.RequestModerationTasks,
					new LinkedList<ModerationTask>(helper.findModerationTasks(user))));
			return 0;

		case SubmitModeration:
			ModerationTask moderation = (ModerationTask) JsonHelper.parseJsonAndroid(info, ModerationTask.class);
			//ModerationTask moderation = (ModerationTask) request.getInformation();
			trust = 0;

			// process
			// path = (taskPath + "/" + FileHelper.recoverDate.format(new
			// Date(System.currentTimeMillis())) + "-"
			// + currentTask.getType() + "-" + user) + ".explicit";
			// FileHelper.writeObject(new File(path), new
			// ExplicitRouteMessage(currentTask, transactionId));

			transactionId = (int) helper.createTransactionId();
			status = inputProcessor.handleModeration(moderation, (int) transactionId);
			done = helper.addTransaction(null, moderation.getType(), user, status, -1,
					new Date(System.currentTimeMillis()), trust, null, (int) transactionId,
					(int) moderation.getToModerateTransactionId());
			System.out.println("new moderation saved to: " + transactionId);

			if (!done)
				transactionId = 0;
			FileHelper.writeCryptedObject(outputStream, cryption,
					new RequestMessage(RequestType.TransactionConfirmation, transactionId));
			break;

		case GetUserData:
			Long userinfo=(Long) JsonHelper.parseJson(info, Long.class);
			if(userinfo==null)
				userinfo=0L;
			
			System.out.println(userinfo+ " "+user+" ");
			UserStatus userStatus = helper.getUserStatus(user, userinfo);//(Long) request.getInformation());
			System.out.println("Get User Data: " + (userStatus != null));
			FileHelper.writeCryptedObject(outputStream, cryption,
					new RequestMessage(RequestType.GetUserData, userStatus));
			return 0;

		case ChangeUserPosition:
			System.out.println("change Home Position");
			// action
			Coordinate c=(Coordinate) JsonHelper.parseJson(info, Coordinate.class);
			//Coordinate c = (Coordinate) request.getInformation();
			helper.changeUserPosition(Point.from(c.y, c.x), user);

			FileHelper.writeCryptedObject(outputStream, cryption,
					new RequestMessage(RequestType.ChangeUserPosition, null));
			return 0;

		case ChangeUserName:
			// TODO
			// action
			helper.changeUserName(/*(String) request.getInformation()*/(String)JsonHelper.parseJson(info, String.class), user);
	
			FileHelper.writeCryptedObject(outputStream, cryption, new RequestMessage(RequestType.ChangeUserName, null));
			return 0;

		case ChangeUserAvatar:

			// TODO

			FileHelper.writeCryptedObject(outputStream, cryption,
					new RequestMessage(RequestType.ChangeUserAvatar, null));
			return 0;

		case RequestTransactions:
			FileHelper.writeCryptedObject(outputStream, cryption, new RequestMessage(RequestType.RequestTransactions,
					new LinkedList<>(helper.getOpenTransactions())));
			return 0;

		case RequestTransaction:
			System.out.println("received");
		
			Integer requestId=(Integer)JsonHelper.parseJsonAndroid(info, Integer.class);
			if(requestId==null)
				requestId=0;
			
		//	System.out.println((Integer) request.getInformation() + " +transaction");
			FileHelper.writeCryptedObject(outputStream, cryption, new RequestMessage(RequestType.RequestTransaction,
					helper.getSubmit(requestId)));
			System.out.println("answered");
			return 0;

		case ChangeTransaction:
			helper.changeTransaction(request.getInformation());
			return -1;

		case ConfirmTransaction:
			// TODO
			return -1;

		case DismissTransaction:
			// TODO
			return -1;

		case RequestFeedbacks:
			type = new TypeToken<ArrayList<Long>>(){}.getType();
			ArrayList<Long> requestMessage=(ArrayList<Long>)JsonHelper.parseJsonAndroid(info, type);
			FileHelper.writeCryptedObject(outputStream, cryption, new RequestMessage(RequestType.RequestFeedbacks,new ArrayList<>(helper.getFeedbacks(requestMessage))));
			return 0;

		case RequestFeedback:
			Integer feedbackId=(Integer)JsonHelper.parseJsonAndroid(info, Integer.class);
			if(feedbackId==null)
				feedbackId=0;
			
			FileHelper.writeCryptedObject(outputStream, cryption, new RequestMessage(RequestType.RequestFeedback,(helper.getFeedback(feedbackId))));
			return 0;
			
		case ChangeFeedback:
			FeedbackModel model=(FeedbackModel)JsonHelper.parseJsonAndroid(info, FeedbackModel.class);
			helper.changeFeedback(model);
			return -1;
			
		case RequestRoutes:
			//(ArrayList<Integer>)request.getInformation()
			type = new TypeToken<ArrayList<Integer>>(){}.getType();
			ArrayList<Integer> rList=((ArrayList<Integer>)JsonHelper.parseJsonAndroid(info, type));
			FileHelper.writeCryptedObject(outputStream, cryption, new RequestMessage(RequestType.RequestRoutes,new LinkedList<>(helper.getRoutes(rList))));
			return 0;

		case RequestRoute:
			type = new TypeToken<ArrayList<Integer>>(){}.getType();
			rList=((ArrayList<Integer>)JsonHelper.parseJsonAndroid(info, type));
			
			FileHelper.writeCryptedObject(outputStream, cryption, new RequestMessage(RequestType.RequestRoute,new LinkedList<>(helper.getRoute(rList))));
			return 0;

		case ChangeRoute:
			RouteVersionModel rvModel=(RouteVersionModel)JsonHelper.parseJsonAndroid(info, RouteVersionModel.class);
			helper.addRoute(rvModel);
			return -1;

		case RequestUser:
			String rUser=(String)JsonHelper.parseJson(info, String.class);
			FileHelper.writeCryptedObject(outputStream, cryption, new RequestMessage(RequestType.RequestUser,
					helper.requestUserById(rUser)));
			return 0;
			
		case ChangeUser:
			helper.changeUser(info);
			return -1;

		case CreateHighscore:
			helper.createHighscore();
			return -1;
			
		case DeployRoutes:
			helper.deployRoutes();
			return -1;
			
		case close:
			System.out.println("close conncection to user: " + user);
			return -1;

		}

		return -1;
	}

}
