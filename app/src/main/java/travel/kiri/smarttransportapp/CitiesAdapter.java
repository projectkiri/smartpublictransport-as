package travel.kiri.smarttransportapp;

import java.util.ArrayList;
import java.util.List;

import travel.kiri.smarttransportapp.model.City;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

// TODO combine with City class?
public class CitiesAdapter extends BaseAdapter {

	private List<City> cities;
	private LayoutInflater inflater;
	private Context context;

	public CitiesAdapter(Context context) {
		inflater = LayoutInflater.from(context);
		this.context = context;
		cities = new ArrayList<City>(City.CITIES.length + 1);
		for (City city: City.CITIES) {
			cities.add(city);
		}
		cities.add(null); // auto detect
	}

	@Override
	public int getCount() {
		return cities.size();
	}

	@Override
	public Object getItem(int position) {
		return cities.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
		TextView tv = (TextView) v.findViewById(android.R.id.text1);
		final String label = getItem(position) == null ? context.getResources().getString(R.string.autodetect) : cities.get(position).name;
		tv.setText(label);
		tv.setContentDescription(label);
		return v;
	}
}
