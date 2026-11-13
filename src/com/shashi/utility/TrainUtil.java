package com.shashi.utility;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.shashi.beans.TrainException;
import com.shashi.beans.UserBean;
import com.shashi.constant.ResponseCode;
import com.shashi.constant.UserRole;
import com.shashi.service.UserService;
import com.shashi.service.impl.UserServiceImpl;

public class TrainUtil {

	public static String login(HttpServletRequest request, UserRole userRole, String username, String password) {
		UserService userService = new UserServiceImpl(userRole);
		String responseCode = ResponseCode.UNAUTHORIZED.toString();
		try {
			UserBean user = userService.loginUser(username, password);

			HttpSession oldSession = request.getSession(false);
			if (oldSession != null) {
				oldSession.invalidate();
			}
			HttpSession session = request.getSession(true);
			session.setMaxInactiveInterval(600);
			session.setAttribute(userRole.toString(), user);
			session.setAttribute("uName", user.getFName());
			session.setAttribute("mailid", user.getMailId());

			// set the responseCode to success
			responseCode = ResponseCode.SUCCESS.toString();

		} catch (TrainException e) {
			responseCode += " : " + e.getMessage();
		}

		return responseCode;
	}

	public static boolean isLoggedIn(HttpServletRequest request, UserRole userRole) {
		HttpSession session = request.getSession(false);
		return session != null && session.getAttribute(userRole.toString()) instanceof UserBean;
	}

	public static void validateUserAuthorization(HttpServletRequest request, UserRole userRole) throws TrainException {
		if (!isLoggedIn(request, userRole)) {
			throw new TrainException(ResponseCode.SESSION_EXPIRED);
		}
	}

	public static boolean logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return true;
	}

	public static String getCurrentUserName(HttpServletRequest req) {
		return (String) req.getSession().getAttribute("uName");
	}

	public static String getCurrentUserEmail(HttpServletRequest req) {
		return (String) req.getSession().getAttribute("mailid");
	}

	public static UserBean getCurrentCustomer(HttpServletRequest req) {
		HttpSession session = req.getSession(false);
		return session == null ? null : (UserBean) session.getAttribute(UserRole.CUSTOMER.toString());
	}
}
