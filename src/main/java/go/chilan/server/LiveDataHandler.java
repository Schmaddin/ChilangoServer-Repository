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
		cleanValues(System.currentTimeMillis(), ride.getUserId());
		live_rides.put(System.currentTimeMillis(), ride);
		System.out.println(
				System.currentTimeMillis() + " lat:" + ride.getLat() + "  lon:" + ride.getLon() + " "+live_rides.size());

	}

	synchronized private static void cleanValues(long time, int userId) {
		
		
		 Iterator<Long> iterator = live_rides.keySet().iterator();
		 while (iterator.hasNext()) {
			 long value=iterator.next();
				if (time - value > 3000 * 60) {
					live_rides.remove(value);
					// delete value
				} else {
					if (live_rides.get(value).getUserId() == userId) {
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
			cleanValues(System.currentTimeMillis(), 0);
		}

		for(Long key:live_rides.keySet())
		{
			rides.add(live_rides.get(key));
		}
		lastRetrieved = System.currentTimeMillis();
		return rides;
	}

}
