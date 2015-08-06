package travel.kiri.smarttransportapp.model;

import java.io.IOException;
import java.util.List;

import travel.kiri.smarttransportapp.R;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;

public class SelectFromMapPoint extends Point {
	final Context context;
	final Location location;
	String textualRepresentation;
	
	public SelectFromMapPoint(Context context, Location location) {
		this.context = context;
		this.location = location;
		this.textualRepresentation = context.getResources().getString(R.string.selected_on_map);
		new AsyncGeocoder().execute(location);
	}
	
	@Override
	public String getEditTextRepresentation() {
		return textualRepresentation;
	}

	@Override
	public Location getLocation() throws NullPointerException {
		return location;
	}

	@Override
	public boolean isEditable() {
		return false;
	}
	
	private class AsyncGeocoder extends AsyncTask<Location, Integer, String> {

		@Override
		protected String doInBackground(Location... params) {
			Geocoder geocoder = new Geocoder(context);
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
