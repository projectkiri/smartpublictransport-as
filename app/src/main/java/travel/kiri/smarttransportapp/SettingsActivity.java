package travel.kiri.smarttransportapp;

import java.text.DateFormat;
import java.util.Locale;

import travel.kiri.smarttransportapp.model.InAppSubscription;
import travel.kiri.smarttransportapp.model.StatisticCounter;
import travel.kiri.smarttransportapp.util.IabHelper;
import travel.kiri.smarttransportapp.util.IabResult;
import travel.kiri.smarttransportapp.util.Purchase;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends ActionBarActivity implements OnItemClickListener, ErrorReporter {

	InAppSubscription inappSubscription;
	SettingsItemsAdapter adapter;

	
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
		setContentView(R.layout.activity_settings);
		
		inappSubscription = InAppSubscription.getInstance(this);

		// Construct settings items

		ListView settingsListView = (ListView) findViewById(R.id.settingsListView);
		adapter = new SettingsItemsAdapter(this);
		settingsListView.setAdapter(adapter);
		settingsListView.setOnItemClickListener(this);

		setupActionBar();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final Activity activity = this;
		switch (position) {
		case 0:
			try {
				inappSubscription.purchase1Year(activity, new IabHelper.OnIabPurchaseFinishedListener() {
					
					@Override
					public void onIabPurchaseFinished(IabResult result, Purchase info) {
						if (result.isFailure()) {
							switch (result.getResponse()) {
							case IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED:
								AlertDialog.Builder builder;
								if (MainActivity.DEBUG_MODE) {
									DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
									    @Override
									    public void onClick(DialogInterface dialog, int which) {
									        switch (which){
									        case DialogInterface.BUTTON_POSITIVE:
									        	inappSubscription.consumeSubscription(new IabHelper.OnConsumeFinishedListener() {
													
													@Override
													public void onConsumeFinished(Purchase purchase, IabResult result) {
														if (result.isFailure()) {
															throw new RuntimeException("Error: Consume fail/" + result.getMessage());
														}
													}
												});
									            break;
		
									        case DialogInterface.BUTTON_NEGATIVE:
									            break;
									        }
									    }
									};
		
									builder = new AlertDialog.Builder(activity);
									builder.setTitle("DONT PRESS OK/YES! PRESS CANCEL/NO!!!");
									builder.setMessage("DEBUG: Cancel subscription? (warning: no refund!)").setPositiveButton("Yes", dialogClickListener)
									    .setNegativeButton("No", dialogClickListener).show();
								}
	
								builder = new AlertDialog.Builder(activity);
								builder.setTitle(R.string.subscription);
								builder.setMessage(R.string.you_have_subscribed);
								builder.setPositiveButton(R.string.ok, new OnClickListener() {
									@Override
									public void onClick(DialogInterface arg0, int arg1) {
										arg0.dismiss();
									}
								});
								builder.show();
								
								break;
							case IabHelper.IABHELPER_USER_CANCELLED:
							case IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED:
								// User intent, no action
								break;
							default:
								reportError(this, new RuntimeException(String.format(getResources().getString(R.string.purchasefail), result.getMessage())));
							}
						} else {
							AlertDialog.Builder builder = new AlertDialog.Builder(activity);
							builder.setTitle(R.string.subscription);
							builder.setMessage(R.string.thanks_for_subscribing);
							builder.setPositiveButton(R.string.ok, new OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0, int arg1) {
									arg0.dismiss();
								}
							});
							builder.show();
						}
					}
				});
			} catch (IllegalStateException ise) {
				reportError(this, ise);
			}
			break;
		case 1:
			Intent intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
			break;
		case 2:
			Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.putExtra(Intent.EXTRA_TEXT, String.format(getResources().getString(R.string.itracked), 0.001 * StatisticCounter.getInstance(this).getTotalDistance()));
			sendIntent.setType("text/plain");
			startActivity(sendIntent);
			break;

		}
	}

	@Override
	public void onActivityResult (int requestCode, int resultCode, Intent data) {
		// Pass activity result to iab helper.
		if (!inappSubscription.iabHelper.handleActivityResult(requestCode, resultCode, data)) {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void reportError(Object source, Throwable tr) {
		Toast toast = Toast.makeText(this, tr.getMessage() == null ? tr.toString() : tr.getMessage(), Toast.LENGTH_LONG);
		toast.show();
	}
	
	private class SettingsItemsAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private Resources resources;
		final Activity activity;
		
		public SettingsItemsAdapter(Activity activity) {
			inflater = getLayoutInflater();
			resources = getResources();
			this.activity = activity;
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		private String getTitle(int index) {
			switch (index) {
			case 0:
				return resources.getString(R.string.subscription);
			case 1:
				return resources.getString(R.string.about);
			case 2:
				return String.format(resources.getString(R.string._km_tracked), 0.001 * StatisticCounter.getInstance(activity).getTotalDistance());
			}
			return null;
		}
		
		private String getDescription(int index) {
			switch (index) {
			case 0:
				if (inappSubscription.isSubscriptionActive()) {
					return String.format(
							resources.getString(R.string.subscription_active),
							DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()).format(inappSubscription.getExpiryTime()));
				} else {
					return resources.getString(R.string.pay_subscription);
				}
			case 1:
				return resources.getString(R.string.knowmore);
			case 2:
				return resources.getString(R.string.share_to_friend);
			}
			return null;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
			TextView tv = (TextView) v.findViewById(android.R.id.text1);
			tv.setText(getTitle(position));
			tv = (TextView) v.findViewById(android.R.id.text2);
			tv.setText(getDescription(position));			
			return v;
		}
		
	}
}
