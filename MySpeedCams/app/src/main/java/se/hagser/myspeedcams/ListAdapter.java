package se.hagser.myspeedcams;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ListAdapter extends BaseAdapter {

    private Activity activity;
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
        if(convertView==null)
            vi = inflater.inflate(R.layout.listitems, null);
        
        
        TextView name = (TextView)vi.findViewById(R.id.name);
        TextView dist = (TextView)vi.findViewById(R.id.dist);
        ImageView bear = (ImageView)vi.findViewById(R.id.bear);
 
        HashMap<String, String> song = new HashMap<String, String>();
        song = data.get(position);
 
        name.setText(song.get(MainActivity.KEY_NAME));
        dist.setText(song.get(MainActivity.KEY_DIST));
        bear.setRotation(Float.parseFloat(song.get(MainActivity.KEY_BEARING)));

        float f = Float.parseFloat(song.get(MainActivity.KEY_DIST));
        
        int seekR=100;
		int seekG=0;
		int seekB=0;
		int ired = 0xff000000 + seekR * 0x10000 + seekG * 0x100 + seekB;
		seekR=100;seekG=100;seekB=0;
		int iyellow = 0xff000000 + seekR * 0x10000 + seekG * 0x100 + seekB;

		seekR=0;seekG=100;seekB=0;
		int igreen = 0xff000000 + seekR * 0x10000 + seekG * 0x100 + seekB;
        
        int color = (f<1000?ired:(f<3000?iyellow:igreen));
		vi.setBackgroundColor(color);
		
        return vi;
    }
}
