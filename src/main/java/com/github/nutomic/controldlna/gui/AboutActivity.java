package com.github.nutomic.controldlna.gui;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import java.util.Date;

import com.github.nutomic.controldlna.BuildConfig;
import com.github.nutomic.controldlna.R;

/**
 * Created by aab on 21/05/16.
 */
public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        Date buildDate = new Date(BuildConfig.TIMESTAMP);
        TextView aboutBuild = (TextView) findViewById(R.id.about_build);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            aboutBuild.setText("Version: " + pInfo.versionName + "\nBuilt: " + buildDate.toString());
        }
        catch(PackageManager.NameNotFoundException e) {
            aboutBuild.setText("Version: Unknown\nBuilt: " + buildDate.toString());
        }

    }
}
