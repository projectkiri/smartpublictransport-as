package travel.kiri.smarttransportapp.model;

import java.util.List;

import android.location.Location;
import android.widget.TextView;

public class TextQueryPoint extends Point {

	/** The target text view. */
	public final TextView target;
	
	/** The place options to pick. */
	private List<Place> places = null;
	
	/** The place that has been hand picked by the user. */
	public Place placePicked = null;
	
	public TextQueryPoint(TextView target) {
		this.target = target;
	}
	
	@Override
	public String getEditTextRepresentation() {
		return target.getText().toString();
	}

	/**
	 * This method returns the text representation of the place picked.
	 * @return the text representation or null if has not been hand picked.
	 */
	public String getTextRepresentation() {
		return placePicked == null ? null : placePicked.name;
	}
	
	/**
	 * This method returns the location coordinate of the place picked.
	 * @return the location or null if has not been hand picked.
	 */
	@Override
	public Location getLocation() {
		return placePicked == null ? null : placePicked.location;
	}

	@Override
	public boolean isEditable() {
		return true;
	}
	
	/**
	 * Sets the place hand picked by the user, out of the places list.
	 * @param index the index of place hand picked.
	 */
	public void pick(int index) throws IndexOutOfBoundsException {
		placePicked = getPlaces().get(index);
	}

	public List<Place> getPlaces() {
		return places;
	}

	/**
	 * Sets the place and automatically pick if there's only one option.
	 * @param places the place list.
	 */
	public void setPlaces(List<Place> places) {
		this.places = places;
		if (places != null && places.size() == 1) {
			pick(0);
		}
	}

	@Override
	public void reset() {
		placePicked = null;
	}
}
