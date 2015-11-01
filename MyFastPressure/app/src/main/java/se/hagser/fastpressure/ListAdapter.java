package se.hagser.fastpressure;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class ListAdapter extends BaseAdapter {

    protected Activity activity;
    private ArrayList<HashMap<String, String>> data;
    private static LayoutInflater inflater=null;
 
    public ListAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
        activity = a;
        data=d;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
 
    public int getCount() {
        return data.size();
    }
 
    public Object getItem(int position) {
        return data.get(position);
    }
 
    public long getItemId(int position) {
        return position;
    }
 
	public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        if(convertView==null) {
            vi = inflater.inflate(R.layout.listitem, null);
        }

        TextView name = (TextView)vi.findViewById(R.id.thepressure);
        TextView at = (TextView)vi.findViewById(R.id.at);
 
        HashMap<String, String> song = new HashMap<String, String>();
        song = data.get(position);
 
        name.setText(song.get(MyPressureService.KEY_PR));
        at.setText(song.get(MyPressureService.KEY_AT));

        return vi;
    }
}
