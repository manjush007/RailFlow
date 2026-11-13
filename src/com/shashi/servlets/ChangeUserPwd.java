package com.shashi.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
//import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.shashi.beans.TrainException;
import com.shashi.beans.UserBean;
import com.shashi.constant.UserRole;
import com.shashi.service.UserService;
import com.shashi.service.impl.UserServiceImpl;
import com.shashi.utility.TrainUtil;

@SuppressWarnings("serial")
@WebServlet("/changeuserpwd")
public class ChangeUserPwd extends HttpServlet {
	private UserService userService = new UserServiceImpl(UserRole.CUSTOMER);

	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();
		TrainUtil.validateUserAuthorization(req, UserRole.CUSTOMER);
		UserBean currentUser = TrainUtil.getCurrentCustomer(req);

		try {
			String u_Name = req.getParameter("username");
			String oldPWord = req.getParameter("oldpassword");
			String newPWord = req.getParameter("newpassword");
			if (currentUser.getMailId().equals(u_Name)) {
				String message = userService.changePassword(u_Name, oldPWord, newPWord);
				if ("SUCCESS".equalsIgnoreCase(message)) {
					TrainUtil.logout(req);
					RequestDispatcher rd = req.getRequestDispatcher("UserLogin.html");
					rd.include(req, res);
					pw.println(
							"<div class='tab'>Your password has been updated successfully. Please log in again.</div>");
				} else {
					RequestDispatcher rd = req.getRequestDispatcher("UserHome.html");
					rd.include(req, res);
					pw.println("<div class='main'><p1 class='menu'><a href='viewuserprofile'>View Profile</a></p1>"
							+ "<p1 class='menu'><a href='edituserprofile'>Edit Profile</a></p1>"
							+ "<p1 class='menu'><a href='changeuserpassword'>Change Password</a></p1>" + "</div>");
					pw.println("<div class='tab'>Password was not changed. Check your current password and use a new password with at least 12 characters.</div>");
				}
			} else {
				RequestDispatcher rd = req.getRequestDispatcher("UserHome.html");
				rd.include(req, res);
				pw.println("<div class='main'><p1 class='menu'><a href='viewuserprofile'>view Profile</a></p1>"
						+ "<p1 class='menu'><a href='edituserprofile'>Edit Profile</a></p1>"
						+ "<p1 class='menu'><a href='changeuserpassword'>Change Password</a></p1>" + "</div>");
				pw.println("<div class='tab'>Invalid username.</div>");
			}

		} catch (Exception e) {
			throw new TrainException(422, this.getClass().getName() + "_FAILED", e.getMessage());
		}

	}

}
