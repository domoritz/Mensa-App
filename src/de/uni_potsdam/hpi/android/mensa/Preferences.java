package de.uni_potsdam.hpi.android.mensa;

import java.util.Iterator;

import android.app.backup.RestoreObserver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class Preferences extends PreferenceActivity {
	private ListPreference menuPreference;
	
	private final static String TAG = "PotsdamMensaApp";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		Preference authorPref = (Preference) findPreference("author");

		authorPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://www.dmoritz.bplaced.de"));
				startActivity(intent);
				return false;
			}
		});
		
		Preference feedbackPref = (Preference) findPreference("feedback");
		
		feedbackPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
			    Intent i = new Intent(Intent.ACTION_SEND);
			    //i.setType("text/plain"); //use this line for testing in the emulator
			    i.setType("message/rfc822") ; // use from live device
			    i.putExtra(Intent.EXTRA_EMAIL, new String[]{"domoritz+mensa@gmail.com"});
			    i.putExtra(Intent.EXTRA_SUBJECT,"Mensa App");
			    //i.putExtra(Intent.EXTRA_TEXT,"body goes here");
			    startActivity(Intent.createChooser(i, "Select email application."));
				return false;
			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
		// Toast.makeText(this, "onResume", Toast.LENGTH_LONG).show();
	}

	private static final String URL_OF_RSS = "URL_OF_RSS";
	private static final String URL_OF_RSS_DEFAULT = "http://myhpi.de/~dominik.moritz/mensa.py?display=multiple&mensa=%s";

	public static String getUrl(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString(URL_OF_RSS, URL_OF_RSS_DEFAULT);
	}

	private static final String MENSA = "MENSA";
	private static final String MENSA_DEFAULT = "griebnitzsee";

	public static String getMensa(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString(MENSA, MENSA_DEFAULT);
	}
	
	public static String getMensaString(Context context) {
		String mensa = getMensa(context);
		
		String[] mensas = context.getResources().getStringArray(R.array.mensas_options);
		String[] mensas_keys = context.getResources().getStringArray(R.array.mensas_values);
		
		int i = 0;
		String name = mensas[i];
		//Log.d(TAG, "Compare with: "+mensa);
		
		for (String value : mensas_keys) {
			//Log.d(TAG, "mensa: "+ name + " " + value);
			if (mensa.equalsIgnoreCase(value)) {
				return name;
			}
			i++;
			name = mensas[i];
		}
		return "Empty";
	}
	
	private static final String DEBUG = "DEBUG";
	private static final Boolean DEBUG_DEFAULT = false;

	public static Boolean getDebug(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(DEBUG, DEBUG_DEFAULT);
	}

}
