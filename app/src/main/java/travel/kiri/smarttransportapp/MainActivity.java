package travel.kiri.smarttransportapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import travel.kiri.smarttransportapp.model.City;
import travel.kiri.smarttransportapp.model.History;
import travel.kiri.smarttransportapp.model.LocationFinder;
import travel.kiri.smarttransportapp.model.MyLocationPoint;
import travel.kiri.smarttransportapp.model.Place;
import travel.kiri.smarttransportapp.model.Point;
import travel.kiri.smarttransportapp.model.SelectFromMapPoint;
import travel.kiri.smarttransportapp.model.StatisticCounter;
import travel.kiri.smarttransportapp.model.TextQueryPoint;
import travel.kiri.smarttransportapp.model.protocol.CicaheumLedengProtocol;
import travel.kiri.smarttransportapp.model.protocol.JSONResponseHandler;

public class MainActivity extends AppCompatActivity implements OnClickListener,
		ErrorReporter, OnCancelListener, DialogInterface.OnClickListener,
		LocationListener {

	public static final boolean DEBUG_MODE = false;
	
	private static final String PREF_REGION = "region";
	// related to ENDPONT
	private static final String[] PREF_ENDPOINT_MYLOCATION = {"startMyLoc", "finishMyLoc" };

	public static final int MY_PERMISSIONS_REQUEST_LOCATION = 0;

	private CicaheumLedengProtocol request;

	private static final int ENDPOINT_START = 0;
	private static final int ENDPOINT_FINISH = 1;
	private ImageButton[] endpointMyLocationButton = new ImageButton[2];
	private ImageButton[] endpointSelectOnMapButton = new ImageButton[2];
	private EditText[] endpointEditText = new EditText[2];
	private DialogInterface[] endpointDialog = new DialogInterface[2];
	private Point[] endpointPoint = new Point[2];
	private TextQueryPoint[] textQueryPoint = new TextQueryPoint[2];
	/** Text watcher that resets the text view to text query point. */
	private TextWatcher[] textQueryReverter = new TextWatcher[2];

	/** The single loading dialog instance for this activity. */
	private LoadingDialog loadingDialog;
	private TextView regionTextView;

	/** Keeps track of place search request not completed. */
	private int pendingPlaceSearch;

	/**
	 * Determines whether a dialog box has been cancelled or not. In such case
	 * pending operations should not continue.
	 */
	private boolean cancelled;

	/** City as detected by the GPS. Null means undetected yet */
	private City cityDetected;
	/** City selected manually, null means not selected yet (from detection). */
	private City citySelected;

	History history;

	LocationFinder locationFinder;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().hide();
		setContentView(R.layout.activity_main);

		locationFinder = LocationFinder.createInstance(this);
		StatisticCounter.createInstance(this);
		request = new CicaheumLedengProtocol(this, this);

		// Set up display metrics.
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		CicaheumLedengProtocol.setDisplayMetrics(metrics);

        history = new History(this);
		loadingDialog = new LoadingDialog(this);
		loadingDialog.setOnCancelListener(this);
		loadingDialog.setCanceledOnTouchOutside(false);
		findViewById(R.id.btn_settings).setOnClickListener(this);
		endpointMyLocationButton[ENDPOINT_START] = (ImageButton) findViewById(R.id.imageButtonGpsFrom);
		endpointMyLocationButton[ENDPOINT_FINISH] = (ImageButton) findViewById(R.id.imageButtonGpsTo);
		endpointSelectOnMapButton[ENDPOINT_START] = (ImageButton) findViewById(R.id.imageButtonMapPointFrom);
		endpointSelectOnMapButton[ENDPOINT_FINISH] = (ImageButton) findViewById(R.id.imageButtonMapPointTo);
		endpointEditText[ENDPOINT_START] = (EditText) findViewById(R.id.fromEditText);
		endpointEditText[ENDPOINT_FINISH] = (EditText) findViewById(R.id.toEditText);
		regionTextView = (TextView) findViewById(R.id.regionTextView);
		Button findButton = (Button) findViewById(R.id.findButton);
		TextView historyTextView = (TextView)findViewById(R.id.btn_history);
		
		for (int i = 0; i < endpointEditText.length; i++) {
			endpointMyLocationButton[i].setOnClickListener(this);
			endpointSelectOnMapButton[i].setOnClickListener(this);
			endpointPoint[i] = textQueryPoint[i] = new TextQueryPoint(
					endpointEditText[i]);
			final int index = i;
			textQueryReverter[i] = new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					// void
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// void
				}

				@Override
				public void afterTextChanged(Editable s) {
					endpointEditText[index].removeTextChangedListener(this);
					endpointEditText[index].setSelectAllOnFocus(false);
					endpointPoint[index] = textQueryPoint[index];
					endpointPoint[index].reset();
					saveStringPreference(PREF_ENDPOINT_MYLOCATION[index], Boolean.toString(false));
				}
			};
		}
		if (!locationFinder.startLocationDetection()) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
					MY_PERMISSIONS_REQUEST_LOCATION);
		}
		locationFinder.addLocationListener(this);
		cityDetected = null;
		citySelected = City.getCityFromCode(getStringPreference(PREF_REGION, null));
		updateRegionTextView(citySelected);
		regionTextView.setOnClickListener(this);
		findButton.setOnClickListener(this);
		historyTextView.setOnClickListener(this);

		// Quick find from my location
		if (getStringPreference(PREF_ENDPOINT_MYLOCATION[ENDPOINT_START], Boolean.toString(true)).equals(Boolean.toString(true))) {
			this.onClick(endpointMyLocationButton[ENDPOINT_START]);
			endpointEditText[ENDPOINT_FINISH].requestFocus();
		} else if (getStringPreference(PREF_ENDPOINT_MYLOCATION[ENDPOINT_FINISH], Boolean.toString(false)).equals(Boolean.toString(true))) {
			this.onClick(endpointMyLocationButton[ENDPOINT_FINISH]);
			endpointEditText[ENDPOINT_START].requestFocus();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
			if (ContextCompat.checkSelfPermission(this,	Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ||
					ContextCompat.checkSelfPermission(this,	Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED) {
				locationFinder.startLocationDetection();
			}
		}
	}

	@Override
	public void onClick(View sender) {
		if (sender.getId() == R.id.btn_settings) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
		} else {
			// Check if sender is one of endpoint buttons.
			for (int i = 0; i < endpointEditText.length; i++) {
				// Check for my location button
				if (sender == endpointMyLocationButton[i]) {
					endpointEditText[i]
							.removeTextChangedListener(textQueryReverter[i]);
					endpointPoint[i] = new MyLocationPoint(this);
					updateEditTextBehavior(i, endpointPoint[i]);
					saveStringPreference(PREF_ENDPOINT_MYLOCATION[i], Boolean.toString(true));
				}
				// Check for select on map button
				if (sender == endpointSelectOnMapButton[i]) {
					endpointEditText[i]
							.removeTextChangedListener(textQueryReverter[i]);
					Intent intent = new Intent(this, SelectOnMapActivity.class);
					intent.putExtra(
							SelectOnMapActivity.EXTRA_ENDPOINT_TYPE,
							i == ENDPOINT_START ? CicaheumLedengProtocol.PROTO_START
									: CicaheumLedengProtocol.PROTO_FINISH);
					startActivityForResult(intent, i);
					saveStringPreference(PREF_ENDPOINT_MYLOCATION[i], Boolean.toString(false));
				}
			}
			if (sender.getId() == R.id.findButton) {
				if (validateEmptyEditText(endpointEditText) == null) {
					cancelled = false;
					pendingPlaceSearch = 0;
					for (Point endpoint : endpointPoint) {
						if (endpoint instanceof TextQueryPoint) {
							final TextQueryPoint endpointCopy = (TextQueryPoint) endpoint;
							String regionCode;
							if (citySelected != null) {
								regionCode = citySelected.code;
							} else if (cityDetected != null) {
								regionCode = cityDetected.code;
							} else {
								Location lastLocation = locationFinder.getLastKnownLocation();
								if (lastLocation == null) {
									regionCode = City.CITIES[0].code;
								} else {
									regionCode = City
											.findNearestCity(lastLocation).code;
								}
							}
							pendingPlaceSearch++;
							final String textQuery = endpoint
									.getEditTextRepresentation();
							request.searchPlace(
									endpoint.getEditTextRepresentation(),
									regionCode,
									new JSONResponseHandler() {
                                        @Override
                                        public void jsonReceived(String jsonText) {
                                            List<Place> places = new ArrayList<Place>();
                                            List<String> attributions = new ArrayList<String>();
                                            try {
                                                CicaheumLedengProtocol.parseSearchPlaceJSON(jsonText, places, attributions);
                                            } catch (RuntimeException re) {
                                                cancelled = true;
                                                reportError(this, re);
                                            }
                                            if (places.size() == 0) {
                                                cancelled = true;
                                                loadingDialog.dismiss();
                                                Toast toast = Toast.makeText(getApplicationContext(), String.format(getString(R.string._not_found), textQuery), Toast.LENGTH_LONG);
                                                toast.show();
                                            } else {
                                                // One place search has been
                                                // completed
                                                if (!cancelled) {
                                                    pendingPlaceSearch--;
                                                    endpointCopy
                                                            .setPlaces(places);
                                                    if (pendingPlaceSearch == 0) {
                                                        loadingDialog.dismiss();
                                                        showPlaceOptionsPickDialog();
                                                    }
                                                } else {
                                                    loadingDialog.dismiss();
                                                }
                                            }
                                        }
                                    });
						}
					}
					if (pendingPlaceSearch == 0) {
						getDirectionAndShowResult();
					} else {
						loadingDialog.show();
					}
				} else {
					Toast toast = Toast.makeText(getApplicationContext(),
							getString(R.string.fill_both_textboxes),
							Toast.LENGTH_SHORT);
					toast.show();
				}
			} else if (sender.getId() == R.id.btn_history) {
				if (history.size() > 0) {
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle(R.string.history);
					final History.Adapter adapter = new History.Adapter(history);
					final Context context = this;
					builder.setAdapter(adapter, new AlertDialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							History.Item selected = (History.Item) adapter
									.getItem(which);
							if (selected != null) {
								DirectionActivity.startThisActivity(context,
										selected.start, selected.finish,
										selected.result);
							}
						}
					});
					builder.create().show();
				} else {
					Toast toast = Toast.makeText(this, R.string.no_data_in_history, Toast.LENGTH_LONG);
					toast.show();
				}
			} else if (sender == regionTextView) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.city);
				final CitiesAdapter adapter = new CitiesAdapter(this);
				builder.setAdapter(adapter, new AlertDialog.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Object selected = adapter.getItem(which);
						if (selected == null) {
							cityDetected = null;
							citySelected = null;
							updateRegionTextView(null);
							saveStringPreference(PREF_REGION, null);
						} else {
							citySelected = (City) selected;
							updateRegionTextView(citySelected);
							saveStringPreference(PREF_REGION, citySelected.code);
						}

					}
				});
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		}
	}

	/**
	 * Updates the edit text behavior according to the point object. Will update
	 * the textbox and add corresponding edit event handler.
	 * 
	 * @param targetIndex
	 *            the index of the endpoint
	 * @param point
	 *            the point.
	 */
	private void updateEditTextBehavior(final int targetIndex, Point point) {
		// Set text once to remove the text change listener.
		endpointEditText[targetIndex]
				.setText(point.getEditTextRepresentation());
		endpointEditText[targetIndex].setSelectAllOnFocus(true);
		if (!point.isEditable()) {
			endpointEditText[targetIndex]
					.addTextChangedListener(textQueryReverter[targetIndex]);
		}
	}

	/**
	 * Gets the direction from server and show it in result when done. It will
	 * take from the internal {@link #endpointPoint}s attribute as parameter.
	 */
	private void getDirectionAndShowResult() {
		final Activity activity = this;
		final Point start = endpointPoint[ENDPOINT_START];
		final Point finish = endpointPoint[ENDPOINT_FINISH];
		try {
			request.findRoute(getResources().getString(R.string.iso639code),
					start.getLocation(), finish.getLocation(),
					new JSONResponseHandler() {

                        @Override
                        public void jsonReceived(String jsonText) {
                            loadingDialog.dismiss();
                            if (!cancelled) {
                                try {
                                    CicaheumLedengProtocol.validateJSON(jsonText);
                                    resetUIState();
                                    history.add(new History.Item(start
                                            .getEditTextRepresentation(), finish
                                            .getEditTextRepresentation(),
                                            jsonText));
                                    DirectionActivity.startThisActivity(activity,
                                            start.getEditTextRepresentation(),
                                            finish.getEditTextRepresentation(),
                                            jsonText);
                                } catch (RuntimeException re) {
                                    reportError(this, re);
                                }
                            }
                        }
					});
			loadingDialog.show();
		} catch (NullPointerException nfe) {
			reportError(this, nfe);
		}
	}

	@Override
	public void reportError(Object source, Throwable tr) {
		Toast toast = Toast.makeText(getApplicationContext(), tr.toString(),
				Toast.LENGTH_LONG);
		toast.show();
		Log.e(source.getClass().toString(), tr.getMessage(), tr);
		loadingDialog.cancel();
	}

	@Override
	protected void onDestroy() {
		locationFinder.stopLocationDetection();
		super.onDestroy();
	}

	/**
	 * Checks if one of the {@link #endpointPoint} is a text query and need to
	 * be hand picked by the user. Requires all place search query to be
	 * finished before calling this method. When all points has been hand
	 * picked, this method will trigger the {@link #getDirectionAndShowResult()}
	 * to request the directions.
	 */
	private void showPlaceOptionsPickDialog() {
		boolean ready = true;
		for (int i = 0; i < endpointPoint.length; i++) {
			Point endPoint = endpointPoint[i];
			if (endPoint instanceof TextQueryPoint
					&& endPoint.getLocation() == null) {
				ready = false;
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(i == ENDPOINT_START ? R.string.from___
						: R.string.to___);
				builder.setItems(
						Place.getNames(((TextQueryPoint) endPoint).getPlaces()),
						this);
				builder.create();
				endpointDialog[i] = builder.show();
				break;
			}
		}
		if (ready) {
			getDirectionAndShowResult();
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		// When one of the dialogs is touched, pick the place.
		for (int i = 0; i < endpointDialog.length; i++) {
			if (dialog == endpointDialog[i]) {
				endpointDialog[i] = null;
				((TextQueryPoint) endpointPoint[i]).pick(which);
				showPlaceOptionsPickDialog();
				break;
			}
		}
	}

	/**
	 * Resets the UI state, as if it's just started.
	 */
	private void resetUIState() {
		// By default, the endpoints are textual query.
		for (Point endpoint : endpointPoint) {
			if (endpoint != null) {
				endpoint.reset();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			Location location = data
					.getParcelableExtra(SelectOnMapActivity.EXTRA_LOCATION);
			endpointPoint[requestCode] = new SelectFromMapPoint(this, location);
			updateEditTextBehavior(requestCode, endpointPoint[requestCode]);
		}
	}

	/**
	 * Find at least one edit text with empty string
	 * 
	 * @param editTexts
	 *            the edit texts to check.
	 * @return the edit text who is empty, or null if all are filled.
	 */
	private static EditText validateEmptyEditText(EditText[] editTexts) {
		for (EditText editText : editTexts) {
			if (editText.getText().length() == 0) {
				return editText;
			}
		}
		return null;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if (dialog == loadingDialog) {
			cancelled = true;
		}
	}

	/**
	 * Saves a string to permanent storage
	 * 
	 * @param key
	 *            the variable name
	 * @param value
	 *            the value to store or null to erase.
	 */
	private void saveStringPreference(String key, String value) {
		SharedPreferences settings = getSharedPreferences(this.getClass()
				.getName(), Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		if (value == null) {
			editor.remove(key);
		} else {
			editor.putString(key, value);
		}
		editor.apply();
	}

	/**
	 * Retrieves String from permanent storage
	 * 
	 * @param key
	 *            the variable name
	 * @return the value, or null if not set before.
	 */
	private String getStringPreference(String key, String defaultValue) {
		SharedPreferences settings = getSharedPreferences(this.getClass()
				.getName(), Context.MODE_PRIVATE);
		try {
			return settings.getString(key, defaultValue);
		} catch (ClassCastException cce) {
			// Fix for existing configuration
			saveStringPreference(key, defaultValue);
			return defaultValue;
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		if (cityDetected == null) {
			cityDetected = City.findNearestCity(location);
			if (citySelected == null) {
				updateRegionTextView(cityDetected);
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// void
	}

	@Override
	public void onProviderEnabled(String provider) {
		// void
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// void
	}

	private void updateRegionTextView(City city) {
		final String youAreInTemplate = getResources().getString(
				R.string.you_are_in);
		regionTextView
				.setText(Html.fromHtml(String.format(youAreInTemplate,
						city == null ? "..."
								: ("<a href=\"#\">" + city.name + "</a>"))));
	}
}
