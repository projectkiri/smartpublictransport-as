package travel.kiri.smarttransportapp.model;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class StatisticCounter {
	public static final String PREF_TOTAL_DISTANCE = "travel.kiri.smarttransportapp.DirectionActivity.totalDistance";

	/** A Shared Preference instance save the statistics. */
	protected final SharedPreferences statisticsSaver;	
	
	protected static StatisticCounter instance = null;
	
	protected StatisticCounter(Activity activity) {
		this.statisticsSaver = activity.getPreferences(Context.MODE_PRIVATE);
	}
	
	/**
	 * Creates or reuse an instance of this class.
	 * 
	 * @param activity
	 *            the main activity
	 * @return the singleton instance of this class.
	 */
	public static StatisticCounter createInstance(Activity activity) {
		if (instance == null) {
			instance = new StatisticCounter(activity);
		}
		return instance;
	}

	public static StatisticCounter getInstance(Activity backupActivity) throws NullPointerException {
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
	 * Adds a distance to the total distance
	 * @param value the new distance
	 */
	public void addTotalDistance(float value) {
		SharedPreferences.Editor editor = statisticsSaver.edit();
		editor.putFloat(PREF_TOTAL_DISTANCE, getTotalDistance() + value);
		editor.apply();
	}
	
	public float getTotalDistance() {
		return statisticsSaver.getFloat(PREF_TOTAL_DISTANCE, 0);
	}
}
