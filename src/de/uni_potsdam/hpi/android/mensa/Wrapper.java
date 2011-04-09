package de.uni_potsdam.hpi.android.mensa;

import java.util.List;

public class Wrapper {
	private int mYear;
	private int mMonth;
	private int mDay;

	private List<Item> items = null;

	public void setmYear(int mYear) {
		this.mYear = mYear;
	}

	public int getmYear() {
		return mYear;
	}

	public void setmMonth(int mMonth) {
		this.mMonth = mMonth;
	}

	public int getmMonth() {
		return mMonth;
	}

	public void setmDay(int mDay) {
		this.mDay = mDay;
	}

	public int getmDay() {
		return mDay;
	}

	void setItems(List<Item> items) {
		this.items = items;
	}

	List<Item> getItems() {
		return items;
	}
}
