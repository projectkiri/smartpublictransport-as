package travel.kiri.smarttransportapp.model.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;

import travel.kiri.smarttransportapp.ErrorReporter;
import travel.kiri.smarttransportapp.R;
import travel.kiri.smarttransportapp.model.LocationUtilities;
import travel.kiri.smarttransportapp.model.Place;
import travel.kiri.smarttransportapp.model.Route;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * This class is responsible for all HTTP requests.
 * 
 * @author pascal
 * 
 */
public class CicaheumLedengProtocol {
	private final String apiKey;

	private static final String KIRI_HOSTNAME = "https://kiri.travel";
	private static final String KIRI_HANDLE_URL = KIRI_HOSTNAME + "/api";
	private static final String KIRI_ICONS_PATH = KIRI_HOSTNAME + "/images/means";
	private static final String KIRI_BALOON_START_URL = KIRI_HOSTNAME + "/images/stepicon-walkstart.png";
	private static final String KIRI_BALOON_FINISH_URL = KIRI_HOSTNAME + "/images/stepicon-finish.png";
	private static final String ICON_EXTENSION = ".png";
	

	public static final String MODIFIER_ICON = "";
	public static final String MODIFIER_MARKER = "baloon/";

	/**
	 * The display metrics that measures the screen density. Should be
	 * initialized before calling getXXXMarker().
	 */
	private static DisplayMetrics displayMetrics;

	/** Timeout for http requests. */
	private static final int TIMEOUT = 25000;

	/** The error handler in case of error. */
	private final ErrorReporter errorReporter;

	// Protocol constants.
	private static final String PROTO_APIKEY = "apikey";
	public static final String PROTO_ATTRIBUTIONS = "attributions";
	public static final String PROTO_FINISH = "finish";
	private static final String PROTO_LOCALE = "locale";
	private static final String PROTO_LOCATION = "location";
	private static final String PROTO_MESSAGE = "message";
	public static final String PROTO_MEANS_WALK = "walk";
	public static final String PROTO_MEANS_NONE = "none";
	private static final String PROTO_MODE = "mode";
	private static final String PROTO_MODE_FINDROUTE = "findroute";
	private static final String PROTO_MODE_SEARCHPLACE = "searchplace";
	private static final String PROTO_PLACENAME = "placename";
	private static final String PROTO_PRESENTATION = "presentation";
	private static final String PROTO_PRESENTATION_DESKTOP = "desktop";
	private static final String PROTO_QUERYSTRING = "querystring";
	private static final String PROTO_REGION = "region";
	private static final String PROTO_ROUTINGRESULTS = "routingresults";
	private static final String PROTO_SEARCHRESULT = "searchresult";
	private static final String PROTO_STEPS = "steps";	
	public static final String PROTO_START = "start";
	private static final String PROTO_STATUS = "status";
	private static final String PROTO_STATUS_OK = "ok";
	private static final String PROTO_TRAVELTIME = "traveltime";
	private static final String PROTO_VERSION = "version";
	private static final String PROTO_VERSION_2 = "2";

	public CicaheumLedengProtocol(Context context, ErrorReporter errorReporter) {
		apiKey = context.getResources().getString(R.string.KIRI_APIKEY);
		this.errorReporter = errorReporter != null ? errorReporter : new ErrorReporter() {
			@Override
			public void reportError(Object source, Throwable tr) {
				// silent on error.
			}
		};
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, TIMEOUT);
	}

	private static HttpClient createDefaultHttpClient() {
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, TIMEOUT);
		return new DefaultHttpClient(params);
	}

	private class JSONAsyncTask extends AsyncTask<HttpPost, Object, String> {
		/** Errors saved during process in separate threads. */
		protected Throwable savedError = null;

        /** JSON handler when it is received. */
        protected final JSONResponseHandler handler;

        public JSONAsyncTask(JSONResponseHandler handler) {
            this.handler = handler;
        }

		@Override
		protected String doInBackground(HttpPost... params) {
			try {
				HttpResponse response = createDefaultHttpClient().execute(params[0]);
				BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StringBuilder stringBuilder = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					stringBuilder.append(line);
				}
				reader.close();
				return stringBuilder.toString();
			} catch (ClientProtocolException cpe) {
				savedError = cpe;
				return null;
			} catch (IOException ioe) {
				savedError = ioe;
				return null;
			}
		}

		/**
		 * This method should be called by the subclass to check if there is
		 * pending exception to handle.
		 */
		@Override
		protected void onPostExecute(String result) {
			if (savedError != null) {
				errorReporter.reportError(this, savedError);
			} else {
                handler.jsonReceived(result);
            }
		}
	}

	public void searchPlace(String query, String regionCode, final JSONResponseHandler handler) {
		HttpPost post = new HttpPost(KIRI_HANDLE_URL);
		List<NameValuePair> parameters = new ArrayList<NameValuePair>(2);
		parameters.add(new BasicNameValuePair(PROTO_VERSION, PROTO_VERSION_2));
		parameters.add(new BasicNameValuePair(PROTO_APIKEY, apiKey));
		parameters.add(new BasicNameValuePair(PROTO_MODE, PROTO_MODE_SEARCHPLACE));
		parameters.add(new BasicNameValuePair(PROTO_QUERYSTRING, query));
		parameters.add(new BasicNameValuePair(PROTO_REGION, regionCode));
        try {
            post.setEntity(new UrlEncodedFormEntity(parameters));
            JSONAsyncTask requester = new JSONAsyncTask(handler);
            requester.execute(post);
        } catch (UnsupportedEncodingException uee) {
            errorReporter.reportError(this, uee);
        }
	}

    /**
     * Parse search place result
     * @param jsonText the JSON text input
     * @param places list, where array of places will be stored (old values will be cleared)
     * @param attributions list, where array of attributions will be stored (old values will be cleared)
     * @throws java.lang.RuntimeException on various exception during parsing.
     */
    public static void parseSearchPlaceJSON(String jsonText, List<Place> places, List<String> attributions) throws RuntimeException {
        places.clear();
        attributions.clear();
        try {
            JSONObject jsonObject = new JSONObject(jsonText);
            if (jsonObject.getString(PROTO_STATUS).equals(PROTO_STATUS_OK)) {
                JSONArray jsonArray = jsonObject.getJSONArray(PROTO_SEARCHRESULT);
                for (int i = 0, iLength = jsonArray.length(); i < iLength; i++) {
                    JSONObject placeJson = jsonArray.getJSONObject(i);
                    Place newPlace = new Place();
                    newPlace.name = placeJson.getString(PROTO_PLACENAME);
                    newPlace.location = LocationUtilities.createLocation(placeJson
                            .getString(PROTO_LOCATION));
                    places.add(newPlace);
                }
                jsonArray = jsonObject.optJSONArray(PROTO_ATTRIBUTIONS);
                if (jsonArray != null) {
                    for (int i = 0, iLength = jsonArray.length(); i < iLength; i++) {
                        attributions.add(jsonArray.getString(i));
                    }
                }
            } else {
                throw new RuntimeException(jsonObject.getString(PROTO_MESSAGE));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	public void findRoute(String locale, Location start, Location finish, final JSONResponseHandler handler) {
		HttpPost post = new HttpPost(KIRI_HANDLE_URL);
		List<NameValuePair> parameters = new ArrayList<NameValuePair>(2);
		parameters.add(new BasicNameValuePair(PROTO_VERSION, PROTO_VERSION_2));
		parameters.add(new BasicNameValuePair(PROTO_APIKEY, apiKey));
		parameters.add(new BasicNameValuePair(PROTO_MODE, PROTO_MODE_FINDROUTE));
		parameters.add(new BasicNameValuePair(PROTO_LOCALE, locale));
		parameters.add(new BasicNameValuePair(PROTO_START, LocationUtilities.locationToString(start)));
		parameters.add(new BasicNameValuePair(PROTO_FINISH, LocationUtilities.locationToString(finish)));
		parameters.add(new BasicNameValuePair(PROTO_PRESENTATION, PROTO_PRESENTATION_DESKTOP));
        try {
            post.setEntity(new UrlEncodedFormEntity(parameters));
            JSONAsyncTask requester = new JSONAsyncTask(handler);
            requester.execute(post);
        } catch (UnsupportedEncodingException uee) {
            errorReporter.reportError(this, uee);
        }
	}

    /**
     * Parse find route JSON response to Java object
     * @param jsonText the JSON text
     * @return the route object
     * @throws java.lang.RuntimeException on various errors during parsing
     */
    public static Route parseFindRouteJSON(String jsonText) throws RuntimeException {
        Route route = new Route();
        try {
            JSONObject jsonObject = new JSONObject(jsonText);
            if (jsonObject.getString(PROTO_STATUS).equals(PROTO_STATUS_OK)) {
                JSONObject firstRoutingResult = jsonObject.getJSONArray(PROTO_ROUTINGRESULTS).getJSONObject(0);
                JSONArray jsonArray = firstRoutingResult.getJSONArray(PROTO_STEPS);
                for (int i = 0, iLength = jsonArray.length(); i < iLength; i++) {
                    JSONArray stepArray = jsonArray.getJSONArray(i);
                    Route.Step newStep = new Route.Step();
                    newStep.means = stepArray.getString(0);
                    newStep.meansDetail = stepArray.getString(1);
                    JSONArray pathArray = stepArray.getJSONArray(2);
                    for (int j = 0, jLength = pathArray.length(); j < jLength; j++) {
                        LatLng latlng = LocationUtilities.convertToLatLng(pathArray.getString(j));
                        newStep.addToPath(latlng);
                    }
                    newStep.description = stepArray.getString(3);
                    route.steps.add(newStep);
                }
                route.travelTime = jsonGetString(firstRoutingResult, PROTO_TRAVELTIME);
            } else {
                throw new RuntimeException(jsonObject.getString(PROTO_MESSAGE));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return route;
    }

    /**
     * Validates CicaheumLedeng response, will throw exception if response cannot be used.
     * TODO: Performance improvement if API can return non-200 status code on error.
     * @param jsonText the JSON text
     * @throws java.lang.RuntimeException on various errors during parsing
     */
    public static void validateJSON(String jsonText) throws RuntimeException {
        try {
            JSONObject jsonObject = new JSONObject(jsonText);
            if (jsonObject.getString(PROTO_STATUS).equals(PROTO_STATUS_OK)) {
                // void. Everything looks good.
            } else {
                throw new RuntimeException(jsonObject.getString(PROTO_MESSAGE));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	/**
	 * Downloads an image from URL.
	 * 
	 * @param url
	 *            the url to retrieve
	 * @param responseHandler
	 *            the handler to be executed when image is retrieved.
	 */
	public void getImage(final String url, final ImageResponseHandler responseHandler) {
		final RemoteBitmapCache cache = RemoteBitmapCache.getInstance();
		if (cache.isCached(url)) {
			responseHandler.imageReceived(cache.getCachedVenue(url));
		} else {
			HttpGet get = new HttpGet(url);
			new AsyncTask<HttpGet, Object, Bitmap>() {
				protected Throwable savedError = null;

				@Override
				protected Bitmap doInBackground(HttpGet... params) {
					try {
						HttpResponse response = createDefaultHttpClient().execute(params[0]);
						if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
							throw new RuntimeException("Error while loading image.");
						}
						return BitmapFactory.decodeStream(response.getEntity().getContent());
					} catch (Exception e) {
						savedError = e;
						return null;
					}
				}

				@Override
				protected void onPostExecute(Bitmap bitmap) {
					if (bitmap == null) {
						errorReporter.reportError(this, savedError);
					} else {
						responseHandler.imageReceived(bitmap);
						cache.cacheBitmap(url, bitmap);
					}
				}
			}.execute(get);
		}
	}

	/**
	 * Construct the URL and retrieves a step image
	 * 
	 * @param step
	 *            the step info to retrieve
	 * @param modifier
	 *            the image type, one of MODIFIER_xxx
	 * @param responseHandler
	 *            the handler when response is received.
	 */
	public void getStepImage(Route.Step step, String modifier, ImageResponseHandler responseHandler) {
        getImage(CicaheumLedengProtocol.KIRI_ICONS_PATH + '/' + step.means + '/' + modifier + step.meansDetail + ICON_EXTENSION, responseHandler);
	}

	/**
	 * Retrieves a marker for a step.
	 * 
	 * @param step
	 *            the step information
	 * @param initialOptions
	 *            the initial marker options, will be added with bitmap and
	 *            anchor
	 * @param responseHandler
	 *            the handler when marker options is ready.
	 */
	public void getStepMarker(final Route.Step step, final MarkerOptions initialOptions,
			final MarkerOptionsResponseHandler responseHandler) {
		getStepImage(step, MODIFIER_MARKER, new ImageResponseHandler() {
			@Override
			public void imageReceived(Bitmap bitmap) {
				initialOptions.icon(BitmapDescriptorFactory.fromBitmap(getNormalizedBitmap(bitmap)));
				if (step.means.equals(PROTO_MEANS_WALK)) {
					initialOptions.anchor(1, 1);
				} else {
					initialOptions.anchor(0, 1);
				}
				responseHandler.markerOptionsReady(initialOptions);
			}
		});
	}

	/**
	 * Retrieves start marker
	 * 
	 * @param initialOptions
	 *            the initial marker options, will be added with bitmap and
	 *            anchor
	 * @param responseHandler
	 *            the handler when marker options is ready.
	 */
	public void getStartMarker(final MarkerOptions initialOptions, final MarkerOptionsResponseHandler responseHandler) {
		getImage(KIRI_BALOON_START_URL, new ImageResponseHandler() {
			@Override
			public void imageReceived(Bitmap bitmap) {
				initialOptions.icon(BitmapDescriptorFactory.fromBitmap(getNormalizedBitmap(bitmap)));
				initialOptions.anchor(1, 1);
				responseHandler.markerOptionsReady(initialOptions);
			}
		});
	}

	/**
	 * Retrieves finish marker
	 * 
	 * @param initialOptions
	 *            the initial marker options, will be added with bitmap and
	 *            anchor
	 * @param responseHandler
	 *            the handler when marker options is ready.
	 */
	public void getFinishMarker(final MarkerOptions initialOptions, final MarkerOptionsResponseHandler responseHandler) {
		getImage(KIRI_BALOON_FINISH_URL, new ImageResponseHandler() {
			@Override
			public void imageReceived(Bitmap bitmap) {
				initialOptions.icon(BitmapDescriptorFactory.fromBitmap(getNormalizedBitmap(bitmap)));
				initialOptions.anchor(0, 1);
				responseHandler.markerOptionsReady(initialOptions);
			}
		});
	}

	/**
	 * Resizes the bitmap to the current system density. Returns the same bitmap
	 * if {@link #displayMetrics} is null.
	 * 
	 * @param bitmap
	 *            the bitmap to resize
	 * @return the resized bitmap
	 */
	private static Bitmap getNormalizedBitmap(Bitmap bitmap) {
		if (displayMetrics == null) {
			Log.w(CicaheumLedengProtocol.class.getName(), "Display metrics is not initialized");
			return bitmap;
		} else {
			Matrix matrix = new Matrix();
			matrix.postScale(displayMetrics.scaledDensity, displayMetrics.scaledDensity);
			return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		}
	}

	/**
	 * Optionally retrieves string from JSON object or null if not available.
	 * 
	 * @return the string value or null if it's a null or not available.
	 */
	private static String jsonGetString(JSONObject json, String attr) {
		if (json.isNull(attr)) {
			return null;
		}
		return json.optString(attr, null);
	}

	public static void setDisplayMetrics(DisplayMetrics displayMetrics) {
		CicaheumLedengProtocol.displayMetrics = displayMetrics;
	}
}
