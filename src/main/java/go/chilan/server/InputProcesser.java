package go.chilan.server;

import com.graphhopper.chilango.GeoHelper;
import com.graphhopper.chilango.data.Feedback;
import com.graphhopper.chilango.data.ModerationTask;
import com.graphhopper.chilango.data.PointSystem;
import com.graphhopper.chilango.data.Route;
import com.graphhopper.chilango.data.database.FeedbackModel;
import com.graphhopper.chilango.tasks.ChilangoTask;

public class InputProcesser {
	public InputProcesser(DBHelper client, String currentUser) {
		super();
		this.client = client;
		this.currentUser = currentUser;
	}

	DBHelper client;
	String currentUser;

	public int handleFeedback(Feedback feedback, int transactionId){
		
		int revisorPoints = 0;
		int creationPoints = 0;
		int status=0;
		
		if(feedback.isSuggestion())
		{
			client.addFeedback(feedback);
		}
		
		if(feedback.getQuestionary()!=null)
			client.addQuestionary(new Feedback(feedback,transactionId));
		

		
		switch(feedback.getType()){
		case route_not_found:
			//TODO
			//remove trust
			if(true)
			status=1;
			

			revisorPoints=PointSystem.FEEDBACK_NORMAL;
			
			break;
		case route_not_exist:
			//TODO
			//remove trust
			if(true)
			status=1;

			revisorPoints=PointSystem.FEEDBACK_NORMAL;
			break;
		case route_gps_Validation:
			Route route=feedback.getRoute();
			double distance=0;
			
			int j=0;
			for(int i=0;i<route.getLat().length;i++)
			{
				distance+=GeoHelper.distance(route.getLat()[i], route.getLon()[i], route.getLat()[j], route.getLon()[j]);
						
				j=i;
			}
			
			status=1;
			revisorPoints=(int)(distance/1000.0*PointSystem.CHECK_IN_POINTS_PER_KM);
			break;
		case route_ok:
			//TODO
			status=1;
			
			//add trust to route
			revisorPoints=PointSystem.FEEDBACK_NORMAL;
			
			break;
		case route_alright:
			//TODO
			status=1;
			
			//add trust to route
			revisorPoints=PointSystem.FEEDBACK_NORMAL;
			break;
		case frequency_alright:
			//TODO
			status=1;
			
			//add trust to route
			revisorPoints=PointSystem.FEEDBACK_NORMAL;
			break;
		case timeTable_alright:
			//TODO
			status=1;
			
			//add trust to route
			revisorPoints=PointSystem.FEEDBACK_NORMAL;
			break;
			
		case route_points_wrong:
			//TODO check it!
			
			//TODO
			//remove trust
			break;
		case route_time_wrong:
			//TODO check it!
			
			//TODO
			//remove trust
			break;
		case route_frequency:
			//TODO check it!
			
			//TODO
			//remove trust
			break;
			
		
		}
		
		client.addPointsToUser(transactionId,currentUser,revisorPoints,creationPoints,feedback.getType().getValue());
		client.addFeedback(new FeedbackModel(feedback,transactionId,(status==0 ? false : true)));
		
		return status;
	}

	public int handleModeration(ModerationTask moderation, int transactionId) {
		return 0;
	}

	public int handleTask(ChilangoTask task) {
		return 0;
	}

	public int handleTask(ChilangoTask task, int transactionId) {
		// TODO Auto-generated method stub
		return 0;
	}
	

}
