package se.hagser.mychat;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ListAdapter extends BaseAdapter {

    private Activity activity;
    private List<HashMap<String, String>> data;
    private static LayoutInflater inflater=null;
 
    public ListAdapter(Activity a, List<HashMap<String, String>> d) {
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
        
        
        TextView at = (TextView)vi.findViewById(R.id.at);
        TextView ms = (TextView)vi.findViewById(R.id.ms);
        TextView ip = (TextView)vi.findViewById(R.id.ip);
        RadioButton ol = (RadioButton)vi.findViewById(R.id.ol);

        HashMap<String, String> song;
        song = data.get(position);
 
        at.setText(song.get("at"));
        ms.setText(song.get("ms"));
        ip.setText(song.get("ip"));
        ol.setChecked((song.containsKey("ol")));

        /*
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
		*/
        return vi;
    }
}
