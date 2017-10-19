package travel.kiri.smarttransportapp.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import travel.kiri.smarttransportapp.R;

public class History extends SQLiteOpenHelper {

	public static final int CAPACITY = 3;
	
    public static final String DATABASE_NAME = "history";
    public static final String TABLE_NAME = "history";
    public static final int DATABASE_VERSION = 1;

    private Context context;

	public History(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
	}

	public void add(Item newItem) {
        SQLiteDatabase db = getWritableDatabase();
        db.insertOrThrow(TABLE_NAME, null, newItem.tosContentValues());
        Cursor cursor = db.rawQuery("SELECT MIN(_id), COUNT(_id) FROM " + TABLE_NAME, null);
        cursor.moveToNext();
        int minId = cursor.getInt(0);
        int size = cursor.getInt(1);
        while (size > CAPACITY) {
            db.delete(TABLE_NAME, "_id=" + minId, null);
            cursor.close();
            cursor = db.rawQuery("SELECT MIN(_id), COUNT(_id) FROM " + TABLE_NAME, null);
            cursor.moveToNext();
            minId = cursor.getInt(0);
            size = cursor.getInt(1);
        }
        cursor.close();
	}

    public List<Item> getItems() {
        final String[] columns = {"_id", "start, finish, result"};
        Cursor cursor = getReadableDatabase().query(TABLE_NAME, columns, null, null, null, null, "_id DESC", null);
        List<Item> items = new ArrayList<Item>(cursor.getCount());
        while (cursor.moveToNext()) {
            items.add(Item.fromCursor(cursor));
        }
        return items;
    }

    public int size() {
        final String[] columns = {"_id"};
        Cursor cursor = getReadableDatabase().query(TABLE_NAME, columns, "", null, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + "(" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "start TEXT, " +
            "finish TEXT, " +
            "result TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public static class Item {
		public String start;
		public String finish;
		public String result;
		public Item(String start, String finish, String result) {
			super();
			this.start = start;
			this.finish = finish;
			this.result = result;
		}
		public Item() {
			// void
		}

        private ContentValues tosContentValues() {
            ContentValues contentValues = new ContentValues(4);
            contentValues.put("start", start);
            contentValues.put("finish", finish);
            contentValues.put("result", result);
            return contentValues;
        }

        private static Item fromCursor(Cursor cursor) {
            Item item = new Item();
            item.start = cursor.getString(1);
            item.finish = cursor.getString(2);
            item.result = cursor.getString(3);
            return item;
        }

        /**
         * Joins list of strings into a single encoded string
         * @param stringList the list of strings
         * @return the single string
         */
        private static String join(List<String> stringList) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            try {
                for (String string: stringList) {
                    if (!first) {
                        sb.append(" ");
                    }
                        sb.append(URLEncoder.encode(string, "UTF-8"));
                    first = false;
                }
            } catch (UnsupportedEncodingException uee) {
                // Shouldn't happen unless UTF-8 is not supported (??)
                throw new RuntimeException(uee);
            }
            return sb.toString();
        }

        /**
         * Unpacks encoded string into list of string
         * @param originalString the original string
         * @return list of string
         */
        private static List<String> split(String originalString) {
            String[] stringArray = originalString.split(" ");
            List<String> returnValue = new ArrayList<String>(stringArray.length);
            try {
                for (String string : stringArray) {
                    returnValue.add(URLDecoder.decode(string, "UTF-8"));
                }
            } catch (UnsupportedEncodingException uee) {
                // Shouldn't happen unless UTF-8 is not supported (??)
                throw new RuntimeException(uee);
            }
            return returnValue;
        }
	}

    public static class Adapter extends BaseAdapter {

        LayoutInflater inflater;
        Context context;
        List<Item> items;
        public Adapter(History history) {
            inflater = LayoutInflater.from(history.context);
            this.context = history.context;
            this.items = history.getItems();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
            Item item = items.get(position);
            textView.setText(item.start + context.getResources().getString(R.string._to_) + item.finish);
            textView.setContentDescription(textView.getText());
            return convertView;
        }
    }
}
