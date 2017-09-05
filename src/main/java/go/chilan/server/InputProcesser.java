package go.chilan.server;

import com.github.filosganga.geogson.model.Geometry;
import com.graphhopper.chilango.GeoHelper;
import com.graphhopper.chilango.data.Feedback;
import com.graphhopper.chilango.data.JsonHelper;
import com.graphhopper.chilango.data.ModerationTask;
import com.graphhopper.chilango.data.PointSystem;
import com.graphhopper.chilango.data.Route;
import com.graphhopper.chilango.data.RouteHelper;
import com.graphhopper.chilango.data.database.FeedbackModel;
import com.graphhopper.chilango.data.database.PointModel;
import com.graphhopper.chilango.data.database.SubmitType;
import com.graphhopper.chilango.tasks.ChilangoTask;
import com.graphhopper.chilango.tasks.DrawRouteTask;
import com.graphhopper.chilango.tasks.GPSRecordTask;
import com.graphhopper.chilango.tasks.RecordTask;
import com.graphhopper.chilango.tasks.TaskHelper;

public class InputProcesser {
	public InputProcesser(DBHelper client, String currentUser) {
		super();
		this.client = client;
		this.currentUser = currentUser;
	}

	DBHelper client;
	String currentUser;

	public int handleFeedback(Feedback feedback, int transactionId, String user) {

		int revisorPoints = 0;
		int creationPoints = 0;
		int status = 0;

		if (feedback.getQuestionary() != null)
			client.addQuestionary(new Feedback(feedback, transactionId));

		if (feedback.isSuggestion()) {
			client.addFeedback(new FeedbackModel(feedback, transactionId, false));
			return 0;
		}

		switch (feedback.getType()) {
		case route_not_found:
			if (true)
				status = 1;
			break;
		case route_not_exist:
			if (true)
				status = 1;

			break;
		case route_gps_Validation:
			Route route = feedback.getRoute();
			double distance = 0;

			int j = 0;
			for (int i = 0; i < route.getLat().length; i++) {
				distance += GeoHelper.distance(route.getLat()[i], route.getLon()[i], route.getLat()[j],
						route.getLon()[j]);

				j = i;
			}

			status = 1;
			revisorPoints = (int) (distance / 1000.0 * PointSystem.CHECK_IN_POINTS_PER_KM);
			break;
		case route_ok:
			// TODO
			status = 1;

			break;
		case route_alright:
			// TODO
			status = 1;

			break;
		case frequency_alright:
			// TODO
			status = 1;

			break;
		case timeTable_alright:
			// TODO
			status = 1;

			break;

		case route_points_wrong:
			// TODO check it!
			status = 1;
			// TODO
			// remove trust
			break;
		case route_time_wrong:
			// TODO check it!
			status = 1;
			// TODO
			// remove trust
			break;
		case route_frequency:
			// TODO check it!
			status = 1;
			// TODO
			// remove trust
			break;

		}

		revisorPoints += PointSystem.getRevisorPoints(feedback.getType(), feedback.isSuggestion());
		creationPoints += PointSystem.getCreationPoints(feedback.getType(), feedback.isSuggestion());
		client.addFeedback(new FeedbackModel(feedback, transactionId, (status == 0 ? false : true)));

		if (creationPoints != 0 || revisorPoints != 0 && status > 0) {
			addFeedbackPoints(transactionId, creationPoints, revisorPoints, feedback, user);
			System.out.println("add c-points: " + creationPoints + " rPoints: " + creationPoints + " for transaction: "
					+ transactionId);
		}

		return status;
	}

	public boolean acceptFeedback(int transactionId) {
		FeedbackModel feedback = (FeedbackModel) JsonHelper.parseJson(client.getFeedback(transactionId),
				FeedbackModel.class);
		if (feedback == null)
			return false;
		else {
			addFeedbackPoints(transactionId, PointSystem.getRevisorPoints(feedback.getType(), feedback.isSuggestion()),
					PointSystem.getCreationPoints(feedback.getType(), feedback.isSuggestion()), feedback,null);
			feedback.setValid(true);
			client.changeFeedback(feedback);
		}

		return true;
	}

	private void addFeedbackPoints(int transactionId, int creatonPoints, int revisorPoints, Feedback feedback,
			String user) {
		PointModel points = new PointModel(transactionId, creatonPoints, revisorPoints, System.currentTimeMillis(),
				feedback.getType().getValue());

		if (user == null)
			user = client.getUserId(transactionId);

		System.out.println("...to user: " + user);

		client.addPointsToUser(points, user);

		Geometry geometry = null;
		if (feedback.getRouteId() > 0) {
			geometry = RouteHelper.getGeometry(client.getRouteLastVersion(feedback.getRouteId()));
		} else if (feedback.getType() == SubmitType.route_gps_Validation) {
			geometry = RouteHelper.getGeometry(feedback.getRoute());
		}

		client.addPointsToMapBoard(geometry, points);
	}

	public int handleModeration(ModerationTask moderation, int transactionId) {
		return 0;
	}

	public int handleTask(ChilangoTask task, int transactionId, boolean accepted) {

		if (!accepted)
			return 0;
		else {
			PointModel points = new PointModel(transactionId, PointSystem.getCreationPoints(task.getType(), false), 0,
					System.currentTimeMillis(), task.getType().getValue());
			String user = client.getUserId(transactionId);
			client.addPointsToUser(points, user);
			client.addPointsToMapBoard(TaskHelper.getGeometry(task), points);

			MailUser.createThankYouMail(client.getMail(user), client.getUserName(user),
					"Registrar Ruta " + task.getName(), points.getCreatorPoints() + points.getRevisorPoints());
			return 1;
		}
	}

	public boolean acceptTask(int transactionId, SubmitType submit) {
		RecordTask task = null;
		switch (submit) {
		case submit_new_gps_route:
			task = (GPSRecordTask) JsonHelper.parseJson(client.getSubmit(transactionId), GPSRecordTask.class);
			break;
		case submit_new_draw_route:
			task = (DrawRouteTask) JsonHelper.parseJson(client.getSubmit(transactionId), DrawRouteTask.class);
			break;
		case submit_route_indication:
			task = (RecordTask) JsonHelper.parseJson(client.getSubmit(transactionId), RecordTask.class);
			break;
		}

		if (task == null)
			return false;
		task.getIconId();

		handleTask(task, transactionId, true);
		return true;
	}

}
