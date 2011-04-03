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
import java.util.Locale;

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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class ShowMenu extends Activity {

	private TextView mDateDisplay;
	private Button mPickDate;
	private int mYear;
	private int mMonth;
	private int mDay;

	private static final String TAG = "MyActivity";

	private String error = "";

	// menu items
	List<Item> items;

	// date already initalized?
	private boolean initalized = false;

	private WebView mWebView;

	private static final int DATE_DIALOG_ID = 0;
	private static final int WARNING_DIALOG = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		// requestWindowFeature(Window.FEATURE_PROGRESS);

		setContentView(R.layout.main);

		if (!initalized) {
			// get the current date
			final Calendar c = Calendar.getInstance();
			mYear = c.get(Calendar.YEAR);
			mMonth = c.get(Calendar.MONTH);
			mDay = c.get(Calendar.DAY_OF_MONTH);
			initalized = true;
		}

		// ###### Web View
		mWebView = (WebView) findViewById(R.id.webview);
		mWebView.setWebViewClient(new HelloWebViewClient());
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setBackgroundColor(0);

		final View zc = mWebView.getZoomControls();
		FrameLayout mContentView = (FrameLayout) getWindow().getDecorView()
				.findViewById(android.R.id.content);
		final FrameLayout.LayoutParams ZOOM_PARAMS = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
		mContentView.addView(zc, ZOOM_PARAMS);
		mWebView.getSettings().setBuiltInZoomControls(false);
		mWebView.getSettings().setSupportZoom(true);
		
		mWebView.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				updateMenu();
				return true;
			}
		});

		zc.getTouchables().get(0)
				.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						mWebView.zoomOut();
					}
				});

		zc.getTouchables().get(1)
				.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						mWebView.zoomIn();
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
		setProgressBarIndeterminate(true);
		setProgressBarIndeterminateVisibility(false);

		// display the current date
		updateMenu();
		
		Context context = this.getApplicationContext();
		String locale = context.getResources().getConfiguration().locale.getDisplayName();
	}

	/**
	 * overwrites standard webviewclient to avoid loading of urls
	 * 
	 * @author dominik
	 * 
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
		case R.id.chdate:
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

		// options: perDay, multiple, multipleComplete; http://myhpi.de/~kai.fabian/mensa.py?multiple
		DownloadMenuTask fetcher = new DownloadMenuTask();
		fetcher.execute(Preferences.getUrl(this.getApplicationContext()));
	}

	/**
	 * fetches the file and processes it
	 */
	private class DownloadMenuTask extends AsyncTask<String, Integer, Boolean> {
		private String url = "";
		
		protected Boolean doInBackground(String... urls) {
			this.url = urls[0];
			String response = "";

			try {
				// options: perDay, multiple, multipleComplete
				response = doGet(urls[0]);

				// showDebug("response: " + response.substring(0, 200));
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
			// showDebug("start");
			setProgressBarIndeterminateVisibility(true);
		}

		protected void onPostExecute(Boolean result) {
			// showDebug("ende "+result);
			if (result) {
				// shows menu in web view
				showMenu();
				
				//show toast
				Context context = getApplicationContext();
				int duration = Toast.LENGTH_LONG;
				Resources res = getResources();
				Toast toast = Toast.makeText(context, String.format(res.getString(R.string.fetched_successfully), this.url), duration);
				toast.show();
				
			} else {
				showError();
			}
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

		for (Item item : items) {
			tmpdate = item.getDate();

			// sb.append("%%%%"+(date.getYear()+1900)+"-"+item.getDate().getMonth()+"-"+item.getDate().getDate()+"\n");
			// sb.append("%##%"+mYear+"-"+mMonth+"-"+mDay+"\n");

			isRight = tmpdate.getMonth() == mMonth;
			isRight = isRight && tmpdate.getDate() == mDay;
			isRight = isRight && (tmpdate.getYear() + 1900) == mYear;

			if (isRight) {
				// richtiges datum gefunden
				sb.append("<li>" + item.getDescription() + "</li>");
				empty = false;
			}
		}

		if (empty) {
			sb.append("<html><body>"+getResources().getString(R.string.no_menu)+"</body></html>");
		}

		String html = "<html><body style=\"color: lightgrey\"><meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />"
				+ "<ol>" + sb.toString() + "</ol>" + "</body></html>";
		mWebView.loadData(html, "text/html", "UTF-8");

		Date date = new Date(mYear - 1900, mMonth, mDay);
		SimpleDateFormat formatter1 = new SimpleDateFormat(
				"EEEE, dd MMMM yyyy", Locale.ENGLISH);
		SimpleDateFormat formatter2 = new SimpleDateFormat("EEE, dd MMM",
				Locale.ENGLISH);

		CharSequence timestring = "";

		GregorianCalendar calendar = new GregorianCalendar();
		Date now = calendar.getTime();
		calendar.add(calendar.DATE, 1);
		Date tomorrow = calendar.getTime();
		calendar.add(calendar.DATE, -2);
		Date yesterday = calendar.getTime();
		
		if (now.getDate() == date.getDate() && now.getMonth() == date.getMonth() && now.getYear() == date.getYear()) {
			timestring = getResources().getText(R.string.today);
		} else if (tomorrow.getDate() == date.getDate() && tomorrow.getMonth() == date.getMonth()
				&& tomorrow.getYear() == date.getYear()) {
			timestring = getResources().getText(R.string.tomorrow);
		} else if (yesterday.getDate() == date.getDate() && yesterday.getMonth() == date.getMonth()
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
		/*Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, "Menu for " + timestring,
				duration);
		toast.show();*/
	}

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
				+ "	<author>MensaRSS generator, kai.fabian+mensarss+noreply@myhpi.de</author>"
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
	}

	public static String doGet(String url) throws ClientProtocolException,
			IOException {
		HttpGet getRequest = new HttpGet(url);
		HttpClient client = new DefaultHttpClient();
		HttpResponse response = client.execute(getRequest);

		return responseToString(response);
	}

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
			updateMenu();
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			return new DatePickerDialog(this, mDateSetListener, mYear, mMonth,
					mDay);
		case WARNING_DIALOG: {
			return new AlertDialog.Builder(this)
					.setMessage(Html.fromHtml("<b>" + getResources().getString(R.string.error) + "</b>") + "\n" + error)
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
		}
		return null;
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

	private void showDebug(String string) {
		//TextView text = (TextView) findViewById(R.id.head);
		//text.setText(string);
		
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, "Debug: " + string, duration);
		toast.show();
	}
}