package travel.kiri.smarttransportapp.model;

import java.util.List;
import java.util.Locale;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class LocationUtilities {
	
	public static Location createLocation(float latitude, float longitude) {
		Location location = new Location(LocationUtilities.class.getName());
		location.setLatitude(latitude);
		location.setLongitude(longitude);
		return location;
	}
	
	public static Location createLocation(String urlValue) {
		String[] latLng = urlValue.split(",");
		return createLocation(Float.parseFloat(latLng[0]), Float.parseFloat(latLng[1]));
	}
	
	public static String locationToString(Location location) {
		return String.format(Locale.US, "%.5f,%.5f", location.getLatitude(), location.getLongitude());
	}
	
	public static LatLng convertToLatLng(Location location) {
		return new LatLng(location.getLatitude(), location.getLongitude());
	}
	
	public static LatLng convertToLatLng(String urlValue) {
		String[] latLng = urlValue.split(",");
		return new LatLng(Float.parseFloat(latLng[0]), Float.parseFloat(latLng[1]));
	}
	
	public static LatLngBounds detectBounds(List<LatLng> points) {
		LatLng minLatLng, maxLatLng;
		double minLat = 90, minLng = 180, maxLat = -90, maxLng = -180;

		for (LatLng latlng : points) {
			minLat = Math.min(latlng.latitude, minLat);
			maxLat = Math.max(latlng.latitude, maxLat);
			minLng = Math.min(latlng.longitude, minLng);
			maxLng = Math.max(latlng.longitude, maxLng);
		}

		minLatLng = new LatLng(minLat, minLng);
		maxLatLng = new LatLng(maxLat, maxLng);

		LatLngBounds latlngBounds = new LatLngBounds(minLatLng, maxLatLng);

		return latlngBounds;
	}	
}
