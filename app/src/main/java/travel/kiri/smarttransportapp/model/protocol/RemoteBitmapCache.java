package travel.kiri.smarttransportapp.model.protocol;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;

/**
 * Cache the bitmap loaded from the internet.
 * 
 * @author pascal
 * 
 */
public class RemoteBitmapCache {

	protected static RemoteBitmapCache instance;

	private Map<String, Bitmap> cache;

	public static RemoteBitmapCache getInstance() {
		if (instance == null) {
			instance = new RemoteBitmapCache();
		}
		return instance;
	}

	public RemoteBitmapCache() {
		cache = new HashMap<String, Bitmap>();
	}

	public void cacheBitmap(String url, Bitmap bitmap) {
		cache.put(url, bitmap);
	}

	/**
	 * 
	 * @param url
	 *            the Bitmap URL
	 * @return the bitmap from URL or null if not available.
	 */
	public Bitmap getCachedVenue(String url) {
		return cache.get(url);
	}

	public boolean isCached(String url) {
		return cache.containsKey(url);
	}
}
