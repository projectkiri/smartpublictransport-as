package travel.kiri.smarttransportapp.model;

import android.location.Location;

/**
 * Describes a lat/lng point that is human readable.
 * 
 * @author pascal
 * 
 */
public abstract class Point {

	/**
	 * Retrieves the textual representation of this object.
	 * 
	 * @return the textual representation
	 */
	public abstract String getEditTextRepresentation();

	/**
	 * Retrieves the location of this object.
	 * 
	 * @return the location
	 * @throws NullPointerException
	 *             if location is not available.
	 */
	public abstract Location getLocation() throws NullPointerException;

	/**
	 * Determines whether the location is editable or not.
	 * 
	 * @return true if editable, false to regard all as one object.
	 */
	public abstract boolean isEditable();

	/**
	 * Resets the state of this point. By default do nothing.
	 */
	public void reset() {
		// void
	}
}
