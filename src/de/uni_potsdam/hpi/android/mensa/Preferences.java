package de.uni_potsdam.hpi.android.mensa;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
	
	private static final String URL = "URL_OF_RSS";
	private static final String URL_DEFAULT = "http://myhpi.de/~kai.fabian/mensa.py?multiple";
	
	public static String getUrl(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).
			getString(URL, URL_DEFAULT);
	}

}
