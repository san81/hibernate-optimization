package com.base;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Rating implements Serializable {
	private String name;
	public static final Rating EXCELLENT = new Rating("Excellent");
	public static final Rating OK = new Rating("OK");
	public static final Rating LOW = new Rating("Low");
	private static final Map<String, Rating> INSTANCES = new HashMap<String, Rating>();
	static {
		INSTANCES.put(EXCELLENT.toString(), EXCELLENT);
		INSTANCES.put(OK.toString(), OK);
		INSTANCES.put(LOW.toString(), LOW);
	}
	private Rating(String name) {
		this.name=name;
	}
	public String toString() {
		return name;
	}
	Object readResolve() {
		return getInstance(name);
	}
	public static Rating getInstance(String name) {
		return (Rating) INSTANCES.get(name);
	}
}
