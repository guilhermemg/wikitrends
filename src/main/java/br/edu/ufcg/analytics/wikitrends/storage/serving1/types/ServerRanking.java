package br.edu.ufcg.analytics.wikitrends.storage.serving1.types;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ServerRanking implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6373451215862698502L;
	
	private int hour;
	private int day;
	private int month;
	private int year;
	private long position;
	private String serverName;
	private int numberOfChanges;

	
	public ServerRanking() {
		
	}

	public ServerRanking(int year, int month, int day, int hour, long position, String serverName, int numberOfChanges) {
		this.hour = hour;
		this.day = day;
		this.month = month;
		this.year = year;
		this.position = position;
		this.serverName = serverName;
		this.numberOfChanges = numberOfChanges;
	}

	public ServerRanking(LocalDateTime time, long position, String serverName, int numberOfAccess) {
		this(time.getYear(), time.getMonthValue(), time.getDayOfMonth(), time.getHour(), position, serverName, numberOfAccess);
	}

	/**
	 * @return the hour
	 */
	public int getHour() {
		return hour;
	}

	/**
	 * @param hour the hour to set
	 */
	public void setHour(int hour) {
		this.hour = hour;
	}

	/**
	 * @return the day
	 */
	public int getDay() {
		return day;
	}

	/**
	 * @param day the day to set
	 */
	public void setDay(int day) {
		this.day = day;
	}

	/**
	 * @return the month
	 */
	public int getMonth() {
		return month;
	}

	/**
	 * @param month the month to set
	 */
	public void setMonth(int month) {
		this.month = month;
	}

	/**
	 * @return the year
	 */
	public int getYear() {
		return year;
	}

	/**
	 * @param year the year to set
	 */
	public void setYear(int year) {
		this.year = year;
	}
	
	/**
	 * @return the position
	 */
	public long getPosition() {
		return position;
	}

	/**
	 * @param position the position to set
	 */
	public void setPosition(long position) {
		this.position = position;
	}

	/**
	 * @return the serverName
	 */
	public String getServerName() {
		return serverName;
	}

	/**
	 * @param serverName the serverName to set
	 */
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	
	/**
	 * @return the numberOfChanges
	 */
	public int getNumberOfChanges() {
		return numberOfChanges;
	}

	/**
	 * @param numberOfChanges the numberOfChanges to set
	 */
	public void setNumberOfChanges(int numberOfChanges) {
		this.numberOfChanges = numberOfChanges;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ServerRanking [hour=" + hour + ", day=" + day + ", month=" + month + ", year=" + year + ", serverName="
				+ serverName + ", numberOfChanges=" + numberOfChanges + "]";
	}

}