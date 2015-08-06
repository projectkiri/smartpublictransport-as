package travel.kiri.smarttransportapp.model;

import java.io.IOException;
import java.util.List;

import travel.kiri.smarttransportapp.R;
import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;

public class MyLocationPoint extends Point {

	final LocationFinder locationFinder;
	final Activity activity;
	String textualRepresentation;
	
	public MyLocationPoint(Activity activity) {
		locationFinder = LocationFinder.getInstance(activity);
		locationFinder.startLocationDetection();
		this.activity = activity;
		textualRepresentation = activity.getResources().getString(R.string.my_location);
		if (locationFinder.getCurrentLocation() != null) {
			new AsyncGeocoder().execute(locationFinder.getCurrentLocation());
		}
	}
	
	@Override
	public String getEditTextRepresentation() {
		return textualRepresentation;
	}

	@Override
	public Location getLocation() throws NullPointerException {
		Location location = locationFinder.getCurrentLocation();
		if (location == null) {
			throw new NullPointerException(activity.getResources().getString(R.string.gps_not_ready));
		}
		new AsyncGeocoder().execute(location);
		return location;
	}

	@Override
	public boolean isEditable() {
		return false;
	} 
	
	private class AsyncGeocoder extends AsyncTask<Location, Integer, String> {

		@Override
		protected String doInBackground(Location... params) {
			Geocoder geocoder = new Geocoder(activity);
			List<Address> addresses;
			try {
				addresses = geocoder.getFromLocation(
						params[0].getLatitude(), params[0].getLongitude(), 1);
			} catch (IOException e) {
				return null;
			}
			if (addresses != null & addresses.size() > 0) {
				return addresses.get(0).getLocality();
			}
			return null;
		}
		
	     protected void onPostExecute(String result) {
	    	 if (result != null) {
	    		 textualRepresentation = result;
	    	 }
	     }
	}
}
