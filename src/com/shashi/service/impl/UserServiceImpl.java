package com.shashi.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import com.shashi.beans.TrainException;
import com.shashi.beans.UserBean;
import com.shashi.constant.ResponseCode;
import com.shashi.constant.UserRole;
import com.shashi.service.UserService;
import com.shashi.utility.DBUtil;
import com.shashi.utility.PasswordUtil;

public class UserServiceImpl implements UserService {

	private final String TABLE_NAME;

	public UserServiceImpl(UserRole userRole) {
		if (userRole == null) {
			userRole = UserRole.CUSTOMER;
		}
		this.TABLE_NAME = userRole.toString();
	}

	@Override
	public UserBean getUserByEmailId(String customerEmailId) throws TrainException {
		UserBean customer = null;
		String query = "SELECT * FROM " + TABLE_NAME + " WHERE MAILID=?";
		try {
			try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
				ps.setString(1, customerEmailId);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						customer = new UserBean();
						customer.setFName(rs.getString("fname"));
						customer.setLName(rs.getString("lname"));
						customer.setAddr(rs.getString("addr"));
						customer.setMailId(rs.getString("mailid"));
						customer.setPhNo(rs.getLong("phno"));
					} else {
						throw new TrainException(ResponseCode.NO_CONTENT);
					}
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new TrainException(e.getMessage());
		}
		return customer;
	}

	@Override
	public List<UserBean> getAllUsers() throws TrainException {
		List<UserBean> customers = null;
		String query = "SELECT * FROM  " + TABLE_NAME;
		try {
			try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(query);
					ResultSet rs = ps.executeQuery()) {
				customers = new ArrayList<UserBean>();
				while (rs.next()) {
					UserBean customer = new UserBean();
					customer.setFName(rs.getString("fname"));
					customer.setLName(rs.getString("lname"));
					customer.setAddr(rs.getString("addr"));
					customer.setMailId(rs.getString("mailid"));
					customer.setPhNo(rs.getLong("phno"));
					customers.add(customer);
				}

				if (customers.isEmpty()) {
					throw new TrainException(ResponseCode.NO_CONTENT);
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new TrainException(e.getMessage());
		}
		return customers;
	}

	@Override
	public String updateUser(UserBean customer) {
		String responseCode = ResponseCode.FAILURE.toString();
		String query = "UPDATE  " + TABLE_NAME + " SET FNAME=?,LNAME=?,ADDR=?,PHNO=? WHERE MAILID=?";
		try {
			try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
				ps.setString(1, customer.getFName());
				ps.setString(2, customer.getLName());
				ps.setString(3, customer.getAddr());
				ps.setLong(4, customer.getPhNo());
				ps.setString(5, customer.getMailId());
				int response = ps.executeUpdate();
				if (response > 0) {
					responseCode = ResponseCode.SUCCESS.toString();
				}
			}
		} catch (SQLException | TrainException e) {
			responseCode += " : " + e.getMessage();
		}
		return responseCode;
	}

	@Override
	public String deleteUser(UserBean customer) {
		String responseCode = ResponseCode.FAILURE.toString();
		String query = "DELETE FROM " + TABLE_NAME + " WHERE MAILID=?";
		try {
			try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
				ps.setString(1, customer.getMailId());
				int response = ps.executeUpdate();
				if (response > 0) {
					responseCode = ResponseCode.SUCCESS.toString();
				}
			}
		} catch (SQLException | TrainException e) {
			responseCode += " : " + e.getMessage();
		}
		return responseCode;
	}

	@Override
	public String registerUser(UserBean customer) {
		String responseCode = ResponseCode.FAILURE.toString();
		if (!PasswordUtil.isValidNewPassword(customer.getPWord())) {
			return responseCode + " : Password must be between 12 and 128 characters.";
		}
		String query = "INSERT INTO " + TABLE_NAME + " VALUES(?,?,?,?,?,?)";
		try {
			String hashedPassword = PasswordUtil.hash(customer.getPWord());
			try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
				ps.setString(1, customer.getMailId());
				ps.setString(2, hashedPassword);
				ps.setString(3, customer.getFName());
				ps.setString(4, customer.getLName());
				ps.setString(5, customer.getAddr());
				ps.setLong(6, customer.getPhNo());
				if (ps.executeUpdate() > 0) {
					responseCode = ResponseCode.SUCCESS.toString();
				}
			}
		} catch (SQLException | TrainException | GeneralSecurityException e) {
			if (e.getMessage().toUpperCase().contains("ORA-00001")) {
				responseCode += " : " + "User With Id: " + customer.getMailId() + " is already registered ";
			} else {
				responseCode += " : " + e.getMessage();
			}
		}
		return responseCode;
	}

	@Override
	public UserBean loginUser(String username, String password) throws TrainException {
		UserBean customer = null;
		String query = "SELECT * FROM " + TABLE_NAME + " WHERE MAILID=?";
		try {
			try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
				ps.setString(1, username);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next() || !PasswordUtil.verify(password, rs.getString("pword"))) {
						throw new TrainException(ResponseCode.UNAUTHORIZED);
					}

					String storedPassword = rs.getString("pword");
					if (!PasswordUtil.isHashed(storedPassword)) {
						updateLegacyPassword(con, username, storedPassword, password);
					}

					customer = new UserBean();
					customer.setFName(rs.getString("fname"));
					customer.setLName(rs.getString("lname"));
					customer.setAddr(rs.getString("addr"));
					customer.setMailId(rs.getString("mailid"));
					customer.setPhNo(rs.getLong("phno"));
				}
			}
		} catch (SQLException | GeneralSecurityException e) {
			System.out.println(e.getMessage());
			throw new TrainException(e.getMessage());
		}
		return customer;
	}

	@Override
	public String changePassword(String username, String oldPassword, String newPassword) {
		if (!PasswordUtil.isValidNewPassword(newPassword)) {
			return ResponseCode.FAILURE + " : Password must be between 12 and 128 characters.";
		}

		String responseCode = ResponseCode.UNAUTHORIZED.toString();
		String selectQuery = "SELECT PWORD FROM " + TABLE_NAME + " WHERE MAILID=?";
		String updateQuery = "UPDATE " + TABLE_NAME + " SET PWORD=? WHERE MAILID=? AND PWORD=?";
		try (Connection con = DBUtil.getConnection(); PreparedStatement select = con.prepareStatement(selectQuery)) {
			select.setString(1, username);
			try (ResultSet rs = select.executeQuery()) {
				if (rs.next()) {
					String storedPassword = rs.getString("pword");
					if (!PasswordUtil.verify(oldPassword, storedPassword)) {
						return responseCode;
					}

					try (PreparedStatement update = con.prepareStatement(updateQuery)) {
						update.setString(1, PasswordUtil.hash(newPassword));
						update.setString(2, username);
						update.setString(3, storedPassword);
						if (update.executeUpdate() == 1) {
							responseCode = ResponseCode.SUCCESS.toString();
						}
					}
				}
			}
		} catch (SQLException | TrainException | GeneralSecurityException e) {
			responseCode = ResponseCode.FAILURE + " : " + e.getMessage();
		}
		return responseCode;
	}

	private void updateLegacyPassword(Connection con, String username, String storedPassword, String password)
			throws SQLException, GeneralSecurityException, TrainException {
		String query = "UPDATE " + TABLE_NAME + " SET PWORD=? WHERE MAILID=? AND PWORD=?";
		try (PreparedStatement ps = con.prepareStatement(query)) {
			ps.setString(1, PasswordUtil.hash(password));
			ps.setString(2, username);
			ps.setString(3, storedPassword);
			if (ps.executeUpdate() != 1) {
				throw new TrainException(ResponseCode.UNAUTHORIZED);
			}
		}
	}

}
