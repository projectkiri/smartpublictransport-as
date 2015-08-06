package travel.kiri.smarttransportapp.model;

import java.util.List;

import android.location.Location;

/**
 * Represents a place and its location.
 * @author pascal
 *
 */
public class Place {
	public String name;
	public Location location;
	
	/**
	 * Retrieves the name only from the list of places.
	 * @return the list of names.
	 */
	public static String[] getNames(List<Place> places) {
		String[] returnValue = new String[places.size()];
		for (int i = 0, size = places.size(); i < size; i++) {
			returnValue[i] = places.get(i).name;
		}
		return returnValue;
	}
}
