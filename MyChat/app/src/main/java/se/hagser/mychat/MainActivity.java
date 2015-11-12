package se.hagser.mychat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {
	BroadcastReceiver broadcastReceiver=null;
	protected ListAdapter mAdapter;
	ListView spinner=null;
	EditText msg=null;
	ArrayList<HashMap<String, String>> rows=new ArrayList<>();
	ArrayList<String> onlinelist = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	protected void onResume() {
		super.onResume();
		broadcastReceiver = getBR();
		registerReceiver(broadcastReceiver, new IntentFilter(MyChatService.RESULT_OK));

		Intent intent = new Intent(this, MyChatService.class);
		startService(intent);

		msg = (EditText)findViewById(R.id.msg);
		msg.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == 66) {
					if((msg.getText()+"").length()>1) {
						sendChat(msg.getText() + "");
						msg.setText("");
					}
				}
				return false;
			}
		});
		spinner = (ListView) findViewById(R.id.list_chat);
		spinner.setAdapter(this.mAdapter);

	}

	@Override
	protected void onPause()
	{
		try {
			unregisterReceiver(broadcastReceiver);
		}
		catch (Exception ex)
		{}
		super.onPause();
	}

	private void sendChat(String message) {
		Intent intentChat = new Intent(MyChatService.RESULT_CH);
		intentChat.putExtra("msg",message);
		sendBroadcast(intentChat);
	}
	private BroadcastReceiver getBR() {
		return new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				//Log.i("intent.getAction()",intent.getAction()+"");
				if(intent.getAction().equals(MyChatService.RESULT_OK))
				{
					if(intent.hasExtra(MyChatService.RESULT_OL))
					{
						if(intent.hasExtra("onlineList")) {
							onlinelist=intent.getStringArrayListExtra("onlineList");
						}
					}
					else {
						int rcnt=rows.size();
						if (intent.hasExtra("chatItem")) {
							ChatItem chatItem = intent.getParcelableExtra("chatItem");

							for (int ic = Math.max((chatItem.itemSet.size() - 20), 0); ic < chatItem.itemSet.size(); ic++) {
								boolean bAdd=true;
								for (int ir = 0; ir < rows.size(); ir++) {
									if(rows.get(ir).get("id").equals(chatItem.itemSet.get(ic).map.get("id")))
									{
										bAdd=false;
										break;
									}
								}
								if(bAdd)
									rows.add(chatItem.itemSet.get(ic).map);
							}
							for(int ir=0;ir<rows.size();ir++) {
								for (int io = 0; io < onlinelist.size(); io++) {
									if (rows.get(ir).get("ip").toString().equals(onlinelist.get(io))) {
										rows.get(ir).put("ol", "1");
										break;
									}
								}
							}
							Log.i("rows", rows.size() + "_" + rcnt);
							if(rcnt!=rows.size()) {
								List<HashMap<String,String>> list = rows.subList( Math.max(rows.size() - 20, 0),rows.size());
								Log.i("list", list.size()+"");

								mAdapter = new ListAdapter(MainActivity.this, list);
								spinner.setAdapter(MainActivity.this.mAdapter);
								spinner.setSelection(list.size());
							}
						}
					}
				}
				unregisterReceiver(this);
				broadcastReceiver=getBR();
				registerReceiver(broadcastReceiver, new IntentFilter(MyChatService.RESULT_OK));
			}
		};
	}

}
