package travel.kiri.smarttransportapp.model;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class Route {
	public String travelTime;
	public List<Route.Step> steps;

	public Route() {
		this.steps = new ArrayList<Route.Step>();
	}

	public static class Step {
		public String means;
		public String meansDetail;
		public String description;
		/** Here we use {@link LatLng} instead of {@link Location} to save space. */
		public List<LatLng> path;

		public Step() {
			this.path = new ArrayList<LatLng>();
		}

		public Step(Parcel parcel) {
			this.path = new ArrayList<LatLng>();
			means = parcel.readString();
			meansDetail = parcel.readString();
			description = parcel.readString();
			parcel.readTypedList(path, LatLng.CREATOR);
		}

		public void addToPath(LatLng latlng) {
			path.add(latlng);
		}
	}
}
