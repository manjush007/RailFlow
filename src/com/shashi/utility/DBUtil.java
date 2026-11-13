package com.shashi.utility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ResourceBundle;

import com.shashi.beans.TrainException;
import com.shashi.constant.ResponseCode;

public class DBUtil {
	private static final String DATABASE_URL;
	private static final String DATABASE_USERNAME;
	private static final String DATABASE_PASSWORD;

	static {

		ResourceBundle rb = ResourceBundle.getBundle("application");
		DATABASE_URL = requiredEnvironmentVariable("DATABASE_URL");
		DATABASE_USERNAME = requiredEnvironmentVariable("DATABASE_USERNAME");
		DATABASE_PASSWORD = requiredEnvironmentVariable("DATABASE_PASSWORD");

		try {
			Class.forName(rb.getString("driverName"));
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static String requiredEnvironmentVariable(String name) {
		String value = System.getenv(name);
		if (value == null || value.trim().isEmpty()) {
			throw new ExceptionInInitializerError("Required environment variable is not set: " + name);
		}
		return value;
	}

	public static Connection getConnection() throws TrainException {
		try {
			return DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
		} catch (SQLException e) {
			System.err.println("Database connection failed: " + e.getMessage());
			throw new TrainException(ResponseCode.DATABASE_CONNECTION_FAILURE);
		}
	}
}
