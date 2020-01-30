package travel.kiri.smarttransportapp;

import travel.kiri.smarttransportapp.model.StatisticCounter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
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

public class SettingsActivity extends AppCompatActivity implements OnItemClickListener, ErrorReporter {

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
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setMessage(R.string.purchasena);
			dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					dialogInterface.dismiss();
				}
			});
			dialog.setCancelable(true);
			dialog.create().show();
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
				return resources.getString(R.string.purchasena);
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
