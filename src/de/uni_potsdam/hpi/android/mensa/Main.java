package de.uni_potsdam.hpi.android.mensa;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ZoomControls;

/**
 * Main class
 * 
 * mensa = canteen
 * 
 * @author dominik
 * 
 */
public class Main extends Activity {

	private final String TAG = "PotsdamMensaApp";

	private int mYear;
	private int mMonth;
	private int mDay;

	private String error = "";

	// menu items
	List<Item> items = null;

	// date already initalized?
	private String mensa = "";
	private String url = "";

	private WebView mWebView;

	/* Dialogs */
	private static final int DATE_DIALOG_ID = 0;
	private static final int WARNING_DIALOG = 1;
	private static final int PROGRESS_DIALOG = 2;
	
	ProgressDialog loadingDialog = null;

	DownloadMenuTask fetcher;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate: E");

		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		// requestWindowFeature(Window.FEATURE_PROGRESS);

		setContentView(R.layout.main);

		// set mensa and make it available to compare it later with mensa from
		// settings
		this.mensa = Preferences.getMensa(getApplicationContext());
		// set url
		this.url = String.format(Preferences.getUrl(getApplicationContext()),
				mensa);

		Log.d(TAG, "Url: " + url);
		Log.d(TAG, "Mensa: " + mensa);

		// this is neccessary to see weather we just changed the orientation
		Wrapper lastNonConfigurationInstance = (Wrapper) getLastNonConfigurationInstance();

		if (lastNonConfigurationInstance == null) {
			// set date to today
			// get the current date
			final Calendar c = Calendar.getInstance();
			mYear = c.get(Calendar.YEAR);
			mMonth = c.get(Calendar.MONTH);
			mDay = c.get(Calendar.DAY_OF_MONTH);
		}

		if (lastNonConfigurationInstance != null) {
			items = lastNonConfigurationInstance.getItems();
			mYear = lastNonConfigurationInstance.getmYear();
			mDay = lastNonConfigurationInstance.getmDay();
			mMonth = lastNonConfigurationInstance.getmMonth();
		}

		// ###### Web View
		mWebView = (WebView) findViewById(R.id.webview);

		mWebView.getSettings().setSupportZoom(true);
		mWebView.getSettings().setBuiltInZoomControls(true);

		mWebView.setWebViewClient(new HelloWebViewClient());
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setBackgroundColor(0);

		mWebView.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				updateMenu();
				return true;
			}
		});

		// ###### Buttons
		ImageButton next = (ImageButton) findViewById(R.id.next);
		next.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mDay += 1;
				showMenu();
			}
		});

		ImageButton last = (ImageButton) findViewById(R.id.last);
		last.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mDay -= 1;
				showMenu();
			}
		});

		Button dateChooserButton = (Button) findViewById(R.id.today);
		dateChooserButton
				.setOnLongClickListener(new View.OnLongClickListener() {

					public boolean onLongClick(View v) {
						// get the current date
						final Calendar c = Calendar.getInstance();
						mYear = c.get(Calendar.YEAR);
						mMonth = c.get(Calendar.MONTH);
						mDay = c.get(Calendar.DAY_OF_MONTH);

						showMenu();
						return true;
					}
				});

		dateChooserButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DATE_DIALOG_ID);
			}
		});

		// ressBarVisibility(true);
		//setProgressBarIndeterminate(true);
		//setProgressBarIndeterminateVisibility(false);

		// display the menu
		if (lastNonConfigurationInstance == null) {
			updateMenu();
		} else {
			showMenu();
		}

		Context context = this.getApplicationContext();
		String locale = context.getResources().getConfiguration().locale
				.getDisplayName();
		Log.d(TAG, "Locale: " + locale);

		Log.v(TAG, "onCreate: X");
	}

	/**
	 * prepares data to pass it to iteself
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		Wrapper wrapper = new Wrapper();
		wrapper.setmDay(mDay);
		wrapper.setmMonth(mMonth);
		wrapper.setmYear(mYear);
		wrapper.setItems(items);

		return wrapper;
	}

	/**
	 * Called when the activity gets focus.
	 **/
	@Override
	public void onRestart() {
		super.onRestart();
		// updates if options were changed
		if (this.mensa != Preferences.getMensa(getApplicationContext())
				|| url.compareTo(String.format(
						Preferences.getUrl(getApplicationContext()), mensa)) != 0) {
			this.mensa = Preferences.getMensa(getApplicationContext());
			this.url = String.format(
					Preferences.getUrl(getApplicationContext()), mensa);

			updateMenu();
		}
	}

	/**
	 * overwrites standard webviewclient to avoid loading of urls
	 */
	private class HelloWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}

	/**
	 * initalizes menu (xml->java)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mensa_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.refresh:
			updateMenu();
			return true;
		case R.id.today:
			// get the current date
			final Calendar c = Calendar.getInstance();
			mYear = c.get(Calendar.YEAR);
			mMonth = c.get(Calendar.MONTH);
			mDay = c.get(Calendar.DAY_OF_MONTH);
			showMenu();
			return true;
		case R.id.pick:
			showDialog(DATE_DIALOG_ID);
			return true;
		case R.id.quit:
			finish();
			return true;
		case R.id.preferences:
			startActivity(new Intent(this, Preferences.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * fetches and shows the menu for a given day
	 * 
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	private void updateMenu() {
		Log.v(TAG, "updateMenu");

		// options: perDay, multiple, multipleComplete;
		// http://myhpi.de/~dominik.moritz/mensa.py?multiple
		fetcher = new DownloadMenuTask();
		fetcher.execute(url);
	}

	/**
	 * fetches the file and processes it
	 */
	private class DownloadMenuTask extends AsyncTask<String, Integer, Boolean> {
		private String currenturl = "";
		private String response;

		protected Boolean doInBackground(String... urls) {
			currenturl = urls[0];
			response = "";

			try {
				// options: perDay, multiple, multipleComplete
				response = doGet(currenturl);
				
				Log.v(TAG, "Response: " + response);

				parse(response);

			} catch (ClientProtocolException e) {
				e.printStackTrace();
				setError("Exception: ClientProtocolException " + e.toString());
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				setError("Exception: IOException "
						+ e.getStackTrace()[0].toString() + e.toString());
				return false;
			} catch (ParserConfigurationException e) {
				setError("Exception: ParserConfigurationException "
						+ e.toString());
				e.printStackTrace();
				return false;
			} catch (SAXException e) {
				setError("Exception: SAXException " + e.toString());
				e.printStackTrace();
				return false;
			}

			return true;
		}

		protected void onPreExecute() {
			showDialog(PROGRESS_DIALOG);
			setProgressBarIndeterminateVisibility(true);
		}

		protected void onPostExecute(Boolean result) {
			setProgressBarIndeterminateVisibility(false);
			dismissDialog(PROGRESS_DIALOG);
			
			if (result) {
				// shows menu in web view
				showMenu();
				
				showDebug("\nresponse length: " + response.length());
				showDebug("\nresponse: " + response);
				showDebug("\nitems: " + items);

				Log.d(TAG, "Debug" + String.valueOf(Preferences.getDebug(getApplicationContext())));
				if (Preferences.getDebug(getApplicationContext())) {
					// show toast
					Context context = getApplicationContext();
					int duration = Toast.LENGTH_LONG;
					Resources res = getResources();
					Toast toast = Toast.makeText(context, String.format(
							res.getString(R.string.fetched_successfully),
							currenturl), duration);
					toast.show();
				}
			} else {
				showDebug("no response");
				showError();
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			setProgressBarIndeterminateVisibility(false);
		}
	}

	/**
	 * shows the menu for a given date
	 */
	private void showMenu() {
		// TextView content = (TextView) findViewById(R.id.content);
		StringBuilder sb = new StringBuilder();
		Date tmpdate;
		boolean isRight = false;

		boolean empty = true;

		String name = "";
		
		showDebug("\ndate: " + mDay + " " + mMonth + " " + mYear);
		
		showDebug("\ndate2: " + items.get(0).getDate().toGMTString());

		for (Item item : items) {
			tmpdate = item.getDate();

			// sb.append("%%%%"+(date.getYear()+1900)+"-"+item.getDate().getMonth()+"-"+item.getDate().getDate()+"\n");
			// sb.append("%##%"+mYear+"-"+mMonth+"-"+mDay+"\n");

			isRight = tmpdate.getMonth() == mMonth;
			isRight = isRight && tmpdate.getDate() == mDay;
			isRight = isRight && (tmpdate.getYear() + 1900) == mYear;

			if (isRight) {
				showDebug("\ntmp date" + tmpdate.toString());
				
				// richtiges datum gefunden
				name = item.getTitle();
				name = name.substring(name.length() - 1);

				if (name.equalsIgnoreCase("4")) {
					name = getResources().getString(R.string.alternative);
				} else {
					name = String.format(getResources()
							.getString(R.string.menu), name);
				}

				sb.append("<dt>" + name + "</dt><dd>" + item.getDescription()
						+ "</dd>");
				empty = false;
			}
		}
		
		showDebug("\nhtml: " + sb.toString());

		if (empty) {
			sb.append("<html><body>"
					+ getResources().getString(R.string.no_menu)
					+ "</body></html>");
		}

		String html = "<html>"
				+ "<head><style>dt { font-weight: bold } dd {margin-bottom: 10px;}</style></head>"
				+ "<body style=\"color: lightgrey\"><meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />"
				+ "<dl>" + sb.toString() + "</dl>" + "</body>" + "</html>";
		mWebView.loadData(html, "text/html", "UTF-8");

		Date date = new Date(mYear - 1900, mMonth, mDay);
		SimpleDateFormat formatter1 = new SimpleDateFormat(
				"EEEE, dd MMMM yyyy", getApplicationContext().getResources()
						.getConfiguration().locale);
		SimpleDateFormat formatter2 = new SimpleDateFormat(
				"EEE, dd MMM",
				getApplicationContext().getResources().getConfiguration().locale);

		CharSequence timestring = "";

		GregorianCalendar calendar = new GregorianCalendar();
		Date now = calendar.getTime();
		calendar.add(calendar.DATE, 1);
		Date tomorrow = calendar.getTime();
		calendar.add(calendar.DATE, -2);
		Date yesterday = calendar.getTime();

		if (now.getDate() == date.getDate()
				&& now.getMonth() == date.getMonth()
				&& now.getYear() == date.getYear()) {
			timestring = getResources().getText(R.string.today);
		} else if (tomorrow.getDate() == date.getDate()
				&& tomorrow.getMonth() == date.getMonth()
				&& tomorrow.getYear() == date.getYear()) {
			timestring = getResources().getText(R.string.tomorrow);
		} else if (yesterday.getDate() == date.getDate()
				&& yesterday.getMonth() == date.getMonth()
				&& yesterday.getYear() == date.getYear()) {
			timestring = getResources().getText(R.string.yesterday);
		} else {
			// out of range

			timestring = new StringBuilder()
			// Month is 0 based so add 1
			// .append(mMonth + 1).append("-").append(mDay).append("-")
			// .append(mYear);
					.append(formatter2.format(date));
		}

		setTitle(new StringBuilder().append(formatter1.format(date)));

		Button dateChooserButton = (Button) findViewById(R.id.today);
		dateChooserButton.setText(timestring);

		// show toast
		/*
		 * Context context = getApplicationContext(); int duration =
		 * Toast.LENGTH_SHORT;
		 * 
		 * Toast toast = Toast.makeText(context, "Menu for " + timestring,
		 * duration); toast.show();
		 */
	}

	/**
	 * xml -> list
	 * 
	 * @param response
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	private void parse(String response) throws ParserConfigurationException,
			SAXException, IOException {

		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<rss version=\"2.0\">"
				+ "<channel>"
				+ "<title>Mensa Uni Potsdam</title>"
				+ "<link>http://www.studentenwerk-potsdam.de/speiseplan.html</link>"
				+ "<description>Essensliste der naechsten Woche der Mensen des Studentenwerks Potsdam</description>"
				+ "<language>de-DE</language>"
				+ "<pubDate>Mon, 21 Feb 2011 19:56:22 GMT</pubDate>"
				+ "<item>"
				+ "	<title>Di, 22. Februar 2011</title>"
				+ "	<description><![CDATA[#1: Spaghetti \"Carbonara\" mit Reibekäse #2: Schweinenackenbraten mit Rotkohl und Klößen #3: Hähnchenbrust nach Siam Art mit Wokgemüse, dazu Bratnudeln oder Bio-Reis #4: Gnocchi Pomodore auf Ratatouillegemüse, dazu Salat mit Bio-Joghurtdressing 			]]></description>"
				+ "	<pubDate>Tue, 22 Feb 2011 10:00:00 GMT</pubDate>"
				+ "	<link>http://www.studentenwerk-potsdam.de/speiseplan.html</link>"
				+ "	<author>MensaRSS generator, dominik.moritz+mensarss+noreply@myhpi.de</author>"
				+ "	<guid>{97d6c2a0cb1ec0ac2046011b14b71014a6c2e8cf}</guid>"
				+ "</item>" + "</channel>" + "</rss>";

		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		RSSHandler handler = new RSSHandler();
		xr.setContentHandler(handler);

		ByteArrayInputStream responsestream = new ByteArrayInputStream(
				response.getBytes());
		InputSource is = new InputSource(responsestream);
		xr.parse(is);

		items = handler.getItems();

		Log.d(TAG, "items" + items);
	}

	/**
	 * gets website content
	 * 
	 * @param url
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static String doGet(String url) throws ClientProtocolException,
			IOException {
		HttpGet getRequest = new HttpGet(url);
		HttpClient client = new DefaultHttpClient();
		HttpResponse response = client.execute(getRequest);

		return responseToString(response);
	}

	/**
	 * turns httpresponse into a string representation
	 * 
	 * @param httpResponse
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	private static String responseToString(HttpResponse httpResponse)
			throws IllegalStateException, IOException {
		StringBuilder response = new StringBuilder();
		String aLine = new String();

		// InputStream to String conversion
		InputStream is = httpResponse.getEntity().getContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));

		while ((aLine = reader.readLine()) != null) {
			response.append(aLine);
		}
		reader.close();

		return response.toString();
	}

	/**
	 * the callback received when the user "sets" the date in the dialog
	 */
	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(android.widget.DatePicker view, int year,
				int monthOfYear, int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;
			showMenu();
		}
	};

	/**
	 * precesses showDialog(x)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			return new DatePickerDialog(this, mDateSetListener, mYear, mMonth,
					mDay);
		case WARNING_DIALOG: {
			return new AlertDialog.Builder(this)
					.setMessage(
							Html.fromHtml("<b>"
									+ getResources().getString(R.string.error)
									+ "</b>")
									+ "\n" + error)
					.setCancelable(false)
					.setNeutralButton(R.string.dialogOK,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
									// dismissDialog(WARNING_DIALOG) should also
									// work!
								}
							}).create();
		}
		case PROGRESS_DIALOG: {
			if (loadingDialog == null) {
				loadingDialog = new ProgressDialog(this);
			}
			// loadingDialog.setTitle(getResources().getString(R.string.fetching));
			loadingDialog.setMessage(String.format(
					getResources().getString(R.string.fetching_desc), url));
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(true);
			loadingDialog.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					fetcher.cancel(true);
				}
			});
			return loadingDialog;
		}

		}
		return null;
	}


	public void callIntent(View view) {
		Toast.makeText(view.getContext(), "test", Toast.LENGTH_LONG).show();
		Intent intent = new Intent(Intent.ACTION_VIEW,
				Uri.parse("http://www.vogella.de"));
		startActivity(intent);
	}

	private void setError(String string) {
		error = string;
	}

	private void showError() {
		showDialog(WARNING_DIALOG);
	}

	private void showError(String string) {
		error = string;
		showDialog(WARNING_DIALOG);
	}
	
	private void showDebug(String s) {
		Log.d(TAG, "Debug" + String.valueOf(Preferences.getDebug(getApplicationContext())));
		if (Preferences.getDebug(getApplicationContext())) {
			// show toast
			Context context = getApplicationContext();
			Toast.makeText(context, "Debug: " + s, Toast.LENGTH_LONG).show();
		}
	}
}