package go.chilan.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.graphhopper.chilango.network.livedata.LiveRide;
import com.graphhopper.chilango.network.livedata.LiveRideUser;

public class LiveDataHandler {
	private static Map<Long, LiveRideUser> live_rides = new TreeMap<Long, LiveRideUser>();
	private static long lastCleaned = 0;
	private static long lastRetrieved = 0;
	private static ArrayList<LiveRide> rides = new ArrayList<>();

	synchronized public static void addValue(LiveRideUser ride) {
		if(ride.getTransportId()==1203 && ride.getUserId().equals("599d1d459194be000102c07b"))
			ride=new LiveRideUser(ride.getLat(),ride.getLon(),ride.getTimeStamp(),1206, ride.getHeading(),ride.getFull(), ride.getUserId()
			,ride.getRat(),ride.isGame());
		
		if(ride.getTransportId()==1202 && ride.getUserId().equals("599d1d459194be000102c07b"))
			ride=new LiveRideUser(ride.getLat(),ride.getLon(),ride.getTimeStamp(),1203, ride.getHeading(),ride.getFull(), ride.getUserId()
			,ride.getRat(),ride.isGame());
		

		
		cleanValues(System.currentTimeMillis(), ride.getUserId());
		live_rides.put(System.currentTimeMillis(), ride);
		System.out.println(System.currentTimeMillis() + " lat:" + ride.getLat() + "  lon:" + ride.getLon() + " "
				+ live_rides.size());

	}

	synchronized private static void cleanValues(long time, String userId) {

		Iterator<Long> iterator = live_rides.keySet().iterator();
		while (iterator.hasNext()) {
			long value = iterator.next();
			if (time - value > 3000 * 60) {
				live_rides.remove(value);
				// delete value
			} else {
				if (live_rides.get(value).getUserId().equals(userId)) {
					live_rides.remove(value);
				}
			}
		}

		lastCleaned = time;
	}

	public static ArrayList<LiveRide> getRides() {

		if (System.currentTimeMillis() - lastRetrieved < 1 * 60 * 1000) {
			return rides;
		}
		rides.clear();

		if (System.currentTimeMillis() - lastCleaned > 3 * 60 * 1000) {
			cleanValues(System.currentTimeMillis(), "");
		}

		for (Long key : live_rides.keySet()) {
			rides.add((LiveRide)live_rides.get(key));
		}
		lastRetrieved = System.currentTimeMillis();
		return rides;
	}

}
