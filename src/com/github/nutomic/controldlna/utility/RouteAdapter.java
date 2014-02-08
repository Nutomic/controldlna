package com.github.nutomic.controldlna.utility;

import java.util.List;

import android.content.Context;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.nutomic.controldlna.R;

public class RouteAdapter extends ArrayAdapter<RouteInfo> {

	public RouteAdapter(Context context) {
		super(context, R.layout.list_item);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.list_item, parent, false);
		}

		TextView title = (TextView) convertView.findViewById(R.id.title);
		title.setText(getItem(position).getName());

		TextView subtitle = (TextView) convertView.findViewById(R.id.subtitle);
		subtitle.setText(getItem(position).getDescription());

		return convertView;
	}

	/**
	 * Replacement for addAll, which is not implemented on lower API levels.
	 */
	public void add(List<RouteInfo> routes) {
		for (RouteInfo r : routes)
			add(r);
	}

}
