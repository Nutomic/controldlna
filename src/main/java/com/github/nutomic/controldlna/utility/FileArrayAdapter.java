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

package com.github.nutomic.controldlna.utility;

import java.net.URI;
import java.util.List;

import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.item.AudioItem;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.PlaylistItem;
import org.fourthline.cling.support.model.item.VideoItem;
import org.fourthline.cling.support.model.item.MusicTrack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.util.Log;

import com.github.nutomic.controldlna.R;

/**
 * ArrayAdapter specialization for UPNP server directory contents.
 *
 * @author Felix Ableitner
 *
 */
public class FileArrayAdapter extends ArrayAdapter<DIDLObject> {

	public FileArrayAdapter(Context context) {
		super(context, R.layout.list_item);
	}

	/**
	 * Returns a view with folder/media title, and artist name (for audio only).
	 */
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
		artist.setText("");
		if (item instanceof MusicTrack) {
			MusicTrack track = (MusicTrack) item;
			String trackNumber = (track.getOriginalTrackNumber() != null)
					? Integer.toString(track.getOriginalTrackNumber()) + ". "
					: "";
			title.setText(trackNumber + item.getTitle());
			if (track.getArtists().length > 0) {
				artist.setText(track.getArtists()[0].getName());
			}
		}
		else {
			title.setText(item.getTitle());
		}

		RemoteImageView image = (RemoteImageView) convertView.findViewById(R.id.image);
		URI icon = item.getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM_ART_URI.class);
		if (icon != null) {
			image.setImageUri(icon);
		}
		else {
			int resId;
			if (item instanceof AudioItem) {
				resId = R.drawable.ic_doc_audio_am;
			}
			else if (item instanceof VideoItem) {
				resId = R.drawable.ic_doc_video_am;
			}
			else if (item instanceof ImageItem) {
				resId = R.drawable.ic_doc_image;
			}
			else if (item instanceof PlaylistItem) {
				resId = R.drawable.ic_doc_album;
			}
			else {
				resId = R.drawable.ic_root_folder_am;
			}
			image.setImageResource(resId);
		}

		return convertView;
	}

	/**
	 * Replacement for addAll, which is not implemented on lower API levels.
	 */
	public void add(List<Item> playlist) {
		for (DIDLObject d : playlist) {
			add(d);
		}
	}

}
