package go.chilan.server;

import java.util.LinkedList;
import java.util.List;

import com.github.filosganga.geogson.model.Coordinates;
import com.github.filosganga.geogson.model.Geometry;
import com.github.filosganga.geogson.model.LineString;
import com.github.filosganga.geogson.model.Point;
import com.github.filosganga.geogson.model.positions.LinearPositions;
import com.github.filosganga.geogson.model.positions.SinglePosition;
import com.graphhopper.chilango.GeoHelper;
import com.graphhopper.chilango.data.UserStatus;
import com.vividsolutions.jts.geom.Coordinate;

public class ServerLogic {

	public static byte calculateTrust(DBHelper helper, String user, Geometry geometry) {
		// TODO Auto-generated method stub
		List<Coordinate> coordinates=new LinkedList<>();
		switch(geometry.type()){
		
		case POINT:
			Point point = ((Point)geometry);
			Coordinates transformation = point.coordinates();
			coordinates.add(new Coordinate(transformation.getLat(),transformation.getLon()));
		
			break;
		case LINE_STRING:
			LinearPositions line = ((LinearPositions)geometry.positions());
			for(SinglePosition p:line.children()){
				coordinates.add(new Coordinate(p.coordinates().getLat(),p.coordinates().getLon()));
			}
			break;
		}
		
		return calculateTrust(helper,user,coordinates);

	}

	private static byte calculateTrust(DBHelper helper, String user, List<Coordinate> coordinates) {
		
		UserStatus userStatus = helper.getUserStatus(user, System.currentTimeMillis());
		if(userStatus==null)
			return -1;
		System.out.println("we got the userStatus");
		
		byte trust=(byte)(userStatus.getStatus().getStatusValue()+(userStatus.getPoints()/1000));
		
		float distance=GeoHelper.distanceToMultiLine(new Coordinate(userStatus.getLat(),userStatus.getLon()), coordinates);
		
		if(distance<1500)
			trust+=10;
		else if(distance <3000)
		{
			trust+=(byte)(10.0f-((distance-1500.0f)/1500.0f*10.0f));
		}
		System.out.println("trust: "+trust);
		
		return trust;
	}
	
	private static byte calculateTrust(DBHelper helper, String user, double[] lat, double[] lon){

		List<Coordinate> coordinates=new LinkedList<>();
		for(int i=0;i<lat.length;i++)
		{
			coordinates.add(new Coordinate(lat[i],lon[i]));
		}
		
		return calculateTrust(helper,user,coordinates);
	}

}
