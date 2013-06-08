package com.github.nutomic.controldlna;

import java.util.Collection;
import java.util.Comparator;

import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.item.Item;
import org.teleal.cling.support.model.item.MusicTrack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FileArrayAdapter extends ArrayAdapter<DIDLObject> {
	
	public FileArrayAdapter(Context context) {
		super(context, android.R.layout.simple_list_item_1);
		sort(new Comparator<DIDLObject>() {

			@Override
			public int compare(DIDLObject lhs, DIDLObject rhs) {
				if (lhs instanceof MusicTrack && rhs instanceof MusicTrack) 
					return ((MusicTrack) rhs).getOriginalTrackNumber() -
							((MusicTrack) lhs).getOriginalTrackNumber();
				else if (lhs instanceof Item && rhs instanceof Container)
					return 1;
				else if (rhs instanceof Item && lhs instanceof Container)
					return -1;
				else if (lhs instanceof Container && rhs instanceof Container)
					return lhs.getTitle().compareTo(rhs.getTitle());
				else
					return 0;
			}
		});
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
	        LayoutInflater inflater = (LayoutInflater) getContext()
	                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        convertView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
		}
		DIDLObject item = getItem(position);
        TextView title = (TextView) convertView.findViewById(android.R.id.text1);
        TextView artist = (TextView) convertView.findViewById(android.R.id.text2);
        MusicTrack track;
		if (item instanceof MusicTrack) {
        	track = (MusicTrack) item;
        	title.setText(Integer.toString(track.getOriginalTrackNumber()) + 
        			". " + item.getTitle());
        	artist.setText(track.getArtists()[0].getName());
		}
        else
        	title.setText(item.getTitle());
        return convertView;
	}
	
	/**
	 * Not implemented on lower API levels.
	 */
	@Override
	public void addAll(Collection<? extends DIDLObject> collection) {
		for (DIDLObject d : collection)
			add(d);
	}
	
}
