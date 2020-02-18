package travel.kiri.smarttransportapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import travel.kiri.smarttransportapp.model.City;
import travel.kiri.smarttransportapp.model.LocationFinder;
import travel.kiri.smarttransportapp.model.LocationUtilities;
import travel.kiri.smarttransportapp.model.Route;
import travel.kiri.smarttransportapp.model.StatisticCounter;
import travel.kiri.smarttransportapp.model.protocol.CicaheumLedengProtocol;
import travel.kiri.smarttransportapp.model.protocol.ImageResponseHandler;
import travel.kiri.smarttransportapp.model.protocol.MarkerOptionsResponseHandler;
import travel.kiri.smarttransportapp.view.SlidingUpPanelLayout;
import travel.kiri.smarttransportapp.view.SlidingUpPanelLayout.PanelSlideListener;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class DirectionActivity extends AppCompatActivity implements
		OnClickListener, OnItemClickListener,
		OnMarkerClickListener, ErrorReporter, LocationListener {

	public static final String EXTRA_ROUTE = "travel.kiri.smarttransportapp.intent.extra.route";
	public static final String EXTRA_DESTINATION = "travel.kiri.smarttransportapp.intent.extra.destination";
	public static final String EXTRA_FROM = "travel.kiri.smarttransportapp.intent.extra.from";

	public static final float FOCUS_ZOOM = 16;

	private static final int[] COLOR_VEHICLE = {0xFF339933, 0xFF8BB33B, 0xFF267373};
	private static final int COLOR_WALK = 0xFFCC3333;

	private static float DEFAULT_POLYLINE_WIDTH = 5.0f;

	private static int BOUNDS_PADDING = 70;

	private SlidingUpPanelLayout slidingUpLayout;

	private RouteAdapter stepAdapter;

	private GoogleMap map;
	private Route route;
	private List<Marker> markers;
	/** The marker to focus, or null to show everything. */
	private Integer selectedMarker;
	private LatLngBounds allPointsBounds;

	private View vRoot;
	private ListView stepListView;
	private TextView tvSelectedStep;
	private ImageButton previousImageButton;
	private ImageButton nextImageButton;
	private CicaheumLedengProtocol request;
	private Resources resources;

	private LocationFinder locationFinder;

	private Location lastLocation = null;
	private String from;
	private String destination;

	private boolean isMuted = true;
	private TextToSpeech ttobj;
	private SharedPreferences pref;
	private final static String KEY_SPEECHMUTED = "speechmuted";

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected void setupActionBar() {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
				&& getActionBar() != null) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_direction);

		setupActionBar();

		vRoot = findViewById(R.id.root);
		tvSelectedStep = (TextView) findViewById(R.id.tv_selected_step);
		stepListView = (ListView) findViewById(R.id.steplistview);
		previousImageButton = (ImageButton) findViewById(R.id.imageButtonPrevious);
		nextImageButton = (ImageButton) findViewById(R.id.imageButtonNext);
		slidingUpLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

		Intent intent = getIntent();
		String jsonRoute = intent.getStringExtra(EXTRA_ROUTE);
		destination = intent.getStringExtra(EXTRA_DESTINATION);
		from = intent.getStringExtra(EXTRA_FROM);
        try {
            route = CicaheumLedengProtocol.parseFindRouteJSON(jsonRoute);
        } catch (RuntimeException re) {
            reportError(this, re);
        }

		// Setup list
		stepAdapter = new RouteAdapter(this, route, this);
		stepListView.setAdapter(stepAdapter);
		stepListView.setOnItemClickListener(this);

		request = new CicaheumLedengProtocol(this, this);
		resources = getResources();
		locationFinder = LocationFinder.getInstance(this);
		locationFinder.addLocationListener(this);
		locationFinder.startLocationDetection();

		// Setup Sliding Up Panel
		slidingUpLayout.setAnchorPoint(0.4F);
		slidingUpLayout.setPanelSlideListener(new PanelSlideListener() {

			private void updateLayoutParams(View panel) {
				stepListView.setLayoutParams(new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT, vRoot
								.getHeight()
								- tvSelectedStep.getHeight()
								- panel.getTop()));
			}

			@Override
			public void onPanelSlide(View panel, float slideOffset) {
				updateLayoutParams(panel);
			}

			@Override
			public void onPanelExpanded(View panel) {
				updateLayoutParams(panel);
			}

			@Override
			public void onPanelCollapsed(View panel) {
				updateLayoutParams(panel);
			}

			@Override
			public void onPanelAnchored(View panel) {
				updateLayoutParams(panel);
			}
		});
		// END Setup Sliding Up Panel

		// TTS
		ttobj = new TextToSpeech(getApplicationContext(),
				new TextToSpeech.OnInitListener() {
					@Override
					public void onInit(int status) {
						if (status != TextToSpeech.ERROR) {
							ttobj.setLanguage(Locale.UK);
						}
					}
				});
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		if (ttobj != null)
			isMuted = pref.getBoolean(KEY_SPEECHMUTED, true);
		supportInvalidateOptionsMenu();

		// Initialize map
		final DirectionActivity thisActivity = this;
		SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().
				findFragmentById(R.id.mapfragment);
		mapFragment.getMapAsync(new OnMapReadyCallback() {
			@Override
			public void onMapReady(GoogleMap googleMap) {
				map = googleMap;
				if (ContextCompat.checkSelfPermission(thisActivity,
						Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
						ContextCompat.checkSelfPermission(thisActivity,
						Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					map.setMyLocationEnabled(true);
				}
				map.setLocationSource(locationFinder);
				map.setInfoWindowAdapter(new SimpleInfoWindowAdapter(
						LayoutInflater.from(thisActivity)));
				map.setOnMarkerClickListener(thisActivity);
				map.moveCamera(CameraUpdateFactory.newLatLngZoom(
						LocationUtilities.convertToLatLng(City.CITIES[1].location),
						11));
				map.getUiSettings().setZoomControlsEnabled(true);

				previousImageButton.setOnClickListener(thisActivity);
				nextImageButton.setOnClickListener(thisActivity);

				markers = new ArrayList<Marker>();

				List<LatLng> allPoints = new ArrayList<LatLng>();
				if (route.steps.size() == 1
						&& route.steps.get(0).means
						.equals(CicaheumLedengProtocol.PROTO_MEANS_NONE)) {
					Toast toast = Toast.makeText(getApplicationContext(),
							R.string.route_not_found, Toast.LENGTH_LONG);
					toast.show();
				}
				int vehicleCount = 0;
				for (int i = 0, iLength = route.steps.size(); i < iLength; i++) {
					final Route.Step step = route.steps.get(i);
					if (!step.means.equals(CicaheumLedengProtocol.PROTO_MEANS_NONE)) {
						PolylineOptions polyline = new PolylineOptions();
						if (step.means
								.equals(CicaheumLedengProtocol.PROTO_MEANS_WALK)) {
							polyline.color(COLOR_WALK);
						} else {
							polyline.color(COLOR_VEHICLE[(vehicleCount++) % COLOR_VEHICLE.length]);
						}
						polyline.width(DEFAULT_POLYLINE_WIDTH);
						polyline.addAll(step.path);
						map.addPolyline(polyline);
					}
					allPoints.addAll(step.path);
					// Request for marker, starts with default marker.
					MarkerOptions initialMarker = new MarkerOptions()
							.position(step.path.get(0))
							.title(String.format(
									resources.getString(R.string.step), i + 1))
							.snippet(step.description);
					markers.add(map.addMarker(initialMarker));
					final int markerIndex = i;
					MarkerOptionsResponseHandler markerResponseHandler = new MarkerOptionsResponseHandler() {
						@Override
						public void markerOptionsReady(MarkerOptions markerOptions) {
							// Replace marker with the one just downloaded.
							markers.get(markerIndex).remove();
							markers.set(markerIndex, map.addMarker(markerOptions));
						}
					};
					// Create markers.
					if (i == 0) {
						request.getStartMarker(initialMarker, markerResponseHandler);
					} else {
						request.getStepMarker(step, initialMarker,
								markerResponseHandler);
					}
					if (i == iLength - 1) {
						initialMarker = new MarkerOptions()
								.position(step.path.get(step.path.size() - 1))
								.title(destination)
								.snippet(
										resources
												.getString(R.string.you_have_reached));
						markers.add(map.addMarker(initialMarker));
						request.getFinishMarker(initialMarker,
								new MarkerOptionsResponseHandler() {
									@Override
									public void markerOptionsReady(
											MarkerOptions markerOptions) {
										markers.get(markerIndex + 1).remove();
										map.addMarker(markerOptions);
									}
								});
						// Set last point for ad location
						LatLng lastPoint = step.path.get(step.path.size() - 1);
					}
				}
				// Set initial location: current location
				Location location = locationFinder.getCurrentLocation();
				if (location != null) {
					map.moveCamera(CameraUpdateFactory.newLatLngZoom(
							LocationUtilities.convertToLatLng(location),
							Constants.DEFAULT_ZOOM));
				}

				allPointsBounds = LocationUtilities.detectBounds(allPoints);

				map.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
					@Override
					public void onCameraMove() {
						selectedMarker = null;
						focusOnSelectedMarker(null);
						map.setOnCameraMoveListener(null);
					}
				});

			}
		});

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (ttobj != null) {
			ttobj.stop();
			ttobj.shutdown();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_direction, menu);
		// toggleMapMenuItem = menu.findItem(R.id.menu_togglemap);
		if (isMuted) {
			menu.findItem(R.id.menu_volume_on).setVisible(false);
			menu.findItem(R.id.menu_volume_off).setVisible(true);
		} else {
			menu.findItem(R.id.menu_volume_on).setVisible(true);
			menu.findItem(R.id.menu_volume_off).setVisible(false);
		}
		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean(KEY_SPEECHMUTED, isMuted);
		editor.apply();
		return true;
	}

	private void speakText(String str) {
		if (!isMuted && ttobj != null) {
			String toSpeak = str;
			ttobj.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
		}
	}

	@Override
	public void onClick(View sender) {
		if (sender == previousImageButton) {
			Integer lastSelectedMarker = selectedMarker;
			if (selectedMarker == null) {
				selectedMarker = markers.size() - 1;
			} else if (selectedMarker == 0) {
				selectedMarker = null;
			} else {
				selectedMarker--;
			}
			focusOnSelectedMarker(lastSelectedMarker);
		} else if (sender == nextImageButton) {
			Integer lastSelectedMarker = selectedMarker;
			if (selectedMarker == null) {
				selectedMarker = 0;
			} else if (selectedMarker == markers.size() - 1) {
				selectedMarker = null;
			} else {
				selectedMarker++;
			}
			focusOnSelectedMarker(lastSelectedMarker);
		}
	}

	/**
	 * Zooms in to the {@link #selectedMarker}, or show all if null. Requires
	 * map to be fully drawn.
	 */
	private void focusOnSelectedMarker(Integer lastSelectedMarker) {
		if (map != null) {
			if (lastSelectedMarker != null) {
				markers.get(lastSelectedMarker).hideInfoWindow();
			}
			CameraUpdate cameraUpdate;
			if (selectedMarker == null) {
				cameraUpdate = CameraUpdateFactory.newLatLngBounds(
						allPointsBounds, BOUNDS_PADDING);
				initSelectedStep(null, null);
			} else {
				cameraUpdate = CameraUpdateFactory.newLatLngZoom(
						markers.get(selectedMarker).getPosition(), FOCUS_ZOOM);
				markers.get(selectedMarker).showInfoWindow();
				initSelectedStep(markers.get(selectedMarker).getTitle(),
						markers.get(selectedMarker).getSnippet());
				stepListView.setItemChecked(selectedMarker, true);
			}
			map.animateCamera(cameraUpdate);
			slidingUpLayout.collapsePane();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		case R.id.menu_volume_off:
			isMuted = false;
			pref.edit().putBoolean(KEY_SPEECHMUTED, true).apply();
			supportInvalidateOptionsMenu();
			return true;
		case R.id.menu_volume_on:
			if (ttobj != null)
				ttobj.stop();
			isMuted = true;
			pref.edit().putBoolean(KEY_SPEECHMUTED, false).apply();
			supportInvalidateOptionsMenu();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void reportError(Object source, Throwable tr) {
		Toast toast = Toast.makeText(getApplicationContext(),
				R.string.connection_error, Toast.LENGTH_LONG);
		toast.show();
		Log.e(source.getClass().toString(), tr.getMessage(), tr);
	}

	/**
	 * Custom info window adapter to show all snippet text.
	 * 
	 * @author nathan
	 * 
	 */
	private static class SimpleInfoWindowAdapter implements
			InfoWindowAdapter {
		private LayoutInflater inflater;

		SimpleInfoWindowAdapter(LayoutInflater inflater) {
			this.inflater = inflater;
		}

		@Override
		public View getInfoWindow(Marker marker) {
			return null;
		}

		@Override
		public View getInfoContents(Marker marker) {
			View popup = inflater.inflate(R.layout.view_infowindow, null);
			TextView textViewTitle = (TextView) popup.findViewById(R.id.title);
			textViewTitle.setText(marker.getTitle());
			return popup;
		}
	}

	/**
	 * The adapter for this class route steps list.
	 * 
	 * @author pascal
	 * 
	 */
	private static class RouteAdapter extends BaseAdapter {

		private final Route route;
		private final LayoutInflater inflater;
		private final CicaheumLedengProtocol request;

		public RouteAdapter(Context context, Route route,
				ErrorReporter errorReporter) {
			this.inflater = LayoutInflater.from(context);
			this.route = route;
			this.request = new CicaheumLedengProtocol(context, errorReporter);
		}

		@Override
		public int getCount() {
			return route.steps.size();
		}

		@Override
		public Object getItem(int position) {
			return route.steps.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Route.Step step = route.steps.get(position);
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.view_route_step_item,
						null);
			}
			final ImageView stepIcon = (ImageView) convertView
					.findViewById(R.id.stepIcon);
			request.getStepImage(step, CicaheumLedengProtocol.MODIFIER_ICON,
					new ImageResponseHandler() {
						@Override
						public void imageReceived(Bitmap bitmap) {
							stepIcon.setImageBitmap(bitmap);
						}
					});
			TextView stepDescription = (TextView) convertView
					.findViewById(R.id.stepDescription);
			stepDescription.setText(step.description);
			return convertView;
		}

	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View v, int position,
			long id) {
		// Switch to map if any of the item list is clicked.
		slidingUpLayout.collapsePane();
		Marker marker = markers.get(position);
		initSelectedStep(marker.getTitle(), marker.getSnippet());

		Integer lastMarker = selectedMarker;
		selectedMarker = position;
		focusOnSelectedMarker(lastMarker);
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		int index = markers.indexOf(marker);
		if (index != -1) {
			selectedMarker = index;
		}
		initSelectedStep(marker.getTitle(), marker.getSnippet());

		// Open the info window for the marker
		// marker.showInfoWindow();

		return false;
	}

	private void initSelectedStep(String txt1, String txt2) {
		String str;
		if (txt1 == null && txt2 == null) {
			str = String.format(
					getString(R.string.route_from_x_to_y), from, destination,
					route.travelTime == null ? getString(R.string.na) : route.travelTime);
		} else {
			str = txt1 + ": " + txt2;
		}

		tvSelectedStep.setText(str);
		speakText(str);

	}

	@Override
	public void onLocationChanged(Location currentLocation) {
		if (lastLocation != null) {
			float distance = lastLocation.distanceTo(currentLocation);
			if (distance > currentLocation.getAccuracy()) {
				StatisticCounter.getInstance(this).addTotalDistance(distance);
			}
		}
		lastLocation = currentLocation;
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// void
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// void
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// void
	}

	public static void startThisActivity(Context ctx, String from,
			String destination, String jsonRoute) {
		Intent intent = new Intent(ctx, DirectionActivity.class);
		intent.putExtra(EXTRA_FROM, from);
		intent.putExtra(EXTRA_DESTINATION, destination);
		intent.putExtra(EXTRA_ROUTE, jsonRoute);
		ctx.startActivity(intent);
	}

	@Override
	public void onBackPressed() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.stop_navigation_);
		builder.setPositiveButton(R.string.yes,
				new AlertDialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
		builder.setNegativeButton(R.string.no, null);
		builder.create().show();
	}
}
