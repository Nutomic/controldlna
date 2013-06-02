package com.github.nutomic.controldlna;

import org.teleal.cling.model.meta.Device;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DeviceArrayAdapter extends ArrayAdapter<Device<?, ?, ?>> {

	public DeviceArrayAdapter(Context context) {
		super(context, android.R.layout.simple_list_item_1);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
	        LayoutInflater inflater = (LayoutInflater) getContext()
	                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
		}
        TextView tv = (TextView) convertView.findViewById(android.R.id.text1);
        tv.setText(getItem(position).getDisplayString());
        return convertView;
	}

}
