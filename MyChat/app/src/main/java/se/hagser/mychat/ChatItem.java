package se.hagser.mychat;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Johan on 2015-11-11.
 */
public class ChatItem implements Parcelable {

		List<Item> itemSet;

		public ChatItem(Parcel in) {
			this.itemSet = new ArrayList<>();
			in.readTypedList(itemSet, Item.CREATOR);
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeTypedList(itemSet);
		}

		public int describeContents() {
			return 0;
		}


		public static final Parcelable.Creator<ChatItem> CREATOR
				= new Parcelable.Creator<ChatItem>() {

			public ChatItem createFromParcel(Parcel in) {
				return new ChatItem(in);
			}

			public ChatItem[] newArray(int size) {
				return new ChatItem[size];
			}
		};

		public static class Item implements Parcelable {
			HashMap<String,String> map;

			public Item(Parcel in) {
				this.map=in.readHashMap(ClassLoader.getSystemClassLoader());
			}

			@Override
			public void writeToParcel(Parcel dest, int flags) {
				dest.writeMap(map);
			}

			public static final Parcelable.Creator<Item> CREATOR
					= new Parcelable.Creator<Item>() {

				public Item createFromParcel(Parcel in) {
					return new Item(in);
				}

				public Item[] newArray(int size) {
					return new Item[size];
				}
			};

			@Override
			public int describeContents() {
				return 0;
			}
		}
	}