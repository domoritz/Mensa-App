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
	
	@Override
	protected void onResume() {
		 super.onResume();
		 //Toast.makeText(this, "onResume", Toast.LENGTH_LONG).show();
	}
	
	private static final String URL_OF_RSS = "URL_OF_RSS";
	private static final String URL_OF_RSS_DEFAULT = "http://myhpi.de/~dominik.moritz/mensa.py?multiple";
	
	public static String getUrl(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).
			getString(URL_OF_RSS, URL_OF_RSS_DEFAULT);
	}
	
	private static final String MENSA = "MENSA";
	private static final String MENSA_DEFAULT = "griebnitzsee";
	
	public static String getMensa(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).
			getString(MENSA, MENSA_DEFAULT);
	}

}
