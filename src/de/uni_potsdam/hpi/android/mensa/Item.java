package de.uni_potsdam.hpi.android.mensa;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Item {
	static SimpleDateFormat FORMATTER = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z",Locale.ENGLISH);
	private String title;
	private URL link;
	private String description;
	private Date date;
	private String datestring;

	// sort by date
	public int compareTo(Item another) {
		if (another == null)
			return 1;
		// sort descending, most recent first
		return another.date.compareTo(date);
	}

	@Override
	public boolean equals(Object obj) {
		return false;
		// omitted for brevity
	}

	public Date getDate() {
		return date;
		//return FORMATTER.format(this.date);
	}
	public String getDatestring() {
		return datestring;
	}
	public String getDescription() {
		return description;
	}
	public URL getLink() {
		return link;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public int hashCode() {
		return 0;
		// omitted for brevity
	}

	public void setDate(String dateString) {
		// pad the date if necessary
		this.datestring = dateString;
		
		/*while (!dateString.endsWith("00")) {
			dateString += "0";
		}*/
		try {
			date = FORMATTER.parse(dateString.trim());
		} catch (java.text.ParseException e) {
			//TODO throw exception
			this.date = new Date(2000,1,1);
			e.printStackTrace();
		}
	}

	public void setDatestring(String datestring) {
		this.datestring = datestring;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	// getters and setters omitted for brevity
	public void setLink(String link) {
		try {
			this.link = new URL(link);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public void setLink(URL link) {
		this.link = link;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return "";
		// omitted for brevity
	}
}
