/*
Copyright (c) 2013, Felix Ableitner
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.github.nutomic.controldlna;

import java.util.Comparator;
import java.util.List;

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
		super(context, R.layout.list_item);
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
	        convertView = inflater.inflate(R.layout.list_item, parent, false);	
		}
		DIDLObject item = getItem(position);
        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView artist = (TextView) convertView.findViewById(R.id.subtitle);	
        RemoteImageView image = (RemoteImageView) convertView.findViewById(R.id.image);
        MusicTrack track;
		if (item instanceof MusicTrack) {
        	track = (MusicTrack) item;
        	title.setText(Integer.toString(track.getOriginalTrackNumber()) + 
        			". " + item.getTitle());
        	artist.setText(track.getArtists()[0].getName());
		}
        else {
        	title.setText(item.getTitle());
        	artist.setText("");
        }
		image.setImageDrawable(null);
		image.setImageUri(item.getFirstPropertyValue(
				DIDLObject.Property.UPNP.ALBUM_ART_URI.class));
        return convertView;
	}

	/**
	 * Replacement for addAll, which is not implemented on lower API levels.
	 */
	public void add(List<Item> playlist) {
		for (DIDLObject d : playlist)
			add(d);
	}
	
}
