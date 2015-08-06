package travel.kiri.smarttransportapp.model;

import android.location.Location;

public class City {
	public final Location location;
	public final float radius;
	public final String code;
	public final String name;
	
	public City(float lat, float lon, float radius, String code, String name) {
		this.location = LocationUtilities.createLocation(lat, lon);
		this.radius = radius;
		this.code = code;
		this.name = name;
	}
	
	public static final City[] CITIES = {
		new City(-6.91474f, 107.60981f, 17000, "bdo", "Bandung"),
        new City(-6.38784f, 106.81779f, 15000, "depok", "Depok"),
		new City(-6.21154f, 106.84517f, 15000, "cgk", "Jakarta"),
		new City(-7.98129f, 112.63192f, 15000, "mlg", "Malang"),
		new City(-7.27421f, 112.71908f, 15000, "sub", "Surabaya")
	};

	public static City getCityFromCode(String code) {
		for (City city: CITIES) {
			if (city.code.equals(code)) {
				return city;
			}
		}
		return null;
	}
	
	public static City findNearestCity(Location location) {
		City nearestCity = null;
		for (City city: City.CITIES) {
			if (nearestCity == null || location.distanceTo(city.location) < location.distanceTo(nearestCity.location)) {
				nearestCity = city;
			}
		}
		return nearestCity;
	}
}
