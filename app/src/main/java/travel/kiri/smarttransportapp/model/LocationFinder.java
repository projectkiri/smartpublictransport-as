package travel.kiri.smarttransportapp.model;

import java.util.LinkedList;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.LocationSource;

/**
 * Detect user location based on GPS, code based on Android's example
 * {@linkplain http
 * ://developer.android.com/guide/topics/location/strategies.html} but modified
 * for a more "background" detection. However, we get the bearing from sensor
 * instead of GPS for enhanced accuracy. A geocoding is performed every
 * {@link #GEOCODING_INTERVAL} times a location is retrieved.
 */
public class LocationFinder implements LocationSource {

	public static final String PREF_CURRENT_LOCATION = "travel.kiri.smarttransportapp.model.LocationFinder.currentLocation";

	/** Interval between two GPS updates, in milliseconds. */
	private static final int UPDATE_INTERVAL = 5 * 1000;

	/** The singleton instance of this class. */
	private static LocationFinder instance;

	/** The location manager to use for locating position. */
	private final LocationManager locationManager;

	/** Determines whether this listener is listening. */
	private boolean listening = false;

	/** The class' listener registered to the system. */
	private final InternalListener internalListenerInstance;

	/** The class that observe this location updates. */
	private final List<LocationListener> listeners;

	/** Special listener for google map. */
	OnLocationChangedListener googleMapListener;

	/** The last location found. */
	private Location currentLocation;

	/** A Shared Preference instance to save last known location. */
	protected final SharedPreferences locationSaver;

	private Activity activity;

	private class InternalListener implements LocationListener {

		public void onLocationChanged(Location location) {
			if (isBetterLocation(location, currentLocation)) {
				currentLocation = location;
				for (LocationListener listener: listeners) {
					listener.onLocationChanged(location);
				}
				if (googleMapListener != null) {
					googleMapListener.onLocationChanged(location);
				}
			}
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// Unnecessary for today case.
		}

		public void onProviderEnabled(String provider) {
			// Unnecessary for today case.
		}

		public void onProviderDisabled(String provider) {
			// Unnecessary for today case.
		}
	}

	protected LocationFinder(Activity activity) {
		this.activity = activity;
		this.locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
		this.listeners = new LinkedList<LocationListener>();
		this.googleMapListener = null;
		this.currentLocation = getLastKnownLocation();
		this.locationSaver = activity.getPreferences(Context.MODE_PRIVATE);
		// Define the listener that responds to location updates
		this.internalListenerInstance = new InternalListener();
	}

	/**
	 * Creates or reuse an instance of this class.
	 * 
	 * @param activity
	 *            the main activity
	 * @return the singleton instance of this class.
	 */
	public static LocationFinder createInstance(Activity activity) {
		if (instance == null) {
			instance = new LocationFinder(activity);
		}
		return instance;
	}

	public static LocationFinder getInstance(Activity backupActivity) throws NullPointerException {
		if (instance == null) {
			if (backupActivity != null) {
				instance = createInstance(backupActivity);
			} else {
				throw new NullPointerException("createInstance must be called first!");
			}
		}
		return instance;
	}

	/**
	 * Add a new listener for this service, and call the location changed event
	 * if a location is already available.
	 * 
	 * @param listener
	 *            the listener to add.
	 */
	public void addLocationListener(LocationListener listener) {
		listeners.add(listener);
		if (currentLocation != null) {
			listener.onLocationChanged(currentLocation);
		}
	}

	public boolean removeLocationListener(LocationListener listener) {
		return listeners.remove(listener);
	}

	/**
	 * Start detecting GPS Location and orientation.
	 */
	public boolean startLocationDetection() {

		if (!listening && (ContextCompat.checkSelfPermission(activity,	Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(activity,	Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED)) {
			// Register the listener with the Location Manager to receive location updates
			try {
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL, 0,
						internalListenerInstance);
			} catch (IllegalArgumentException iae) {
				// void
			}
			try {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL, 0,
						internalListenerInstance);
			} catch (IllegalArgumentException iae) {
				// void
			}
			listening = true;
		}
		return listening;
	}

	public void stopLocationDetection() {
		locationManager.removeUpdates(internalListenerInstance);
		listening = false;
	}

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > UPDATE_INTERVAL;
		boolean isSignificantlyOlder = timeDelta < -UPDATE_INTERVAL;

		// If it's been more than five seconds since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isMoreAccurate = accuracyDelta < 0;
		return isMoreAccurate;
	}

	public Location getCurrentLocation() {
		return currentLocation;
	}

	@Override
	public void activate(OnLocationChangedListener listener) {
		googleMapListener = listener;
	}

	@Override
	public void deactivate() {
		googleMapListener = null;
	}
	
	public Location getLastKnownLocation() {
		if (ContextCompat.checkSelfPermission(activity,	Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(activity,	Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED) {
			Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (lastKnownLocation == null) {
				lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}
			return lastKnownLocation;
		} else {
			return null;
		}
	}
}
