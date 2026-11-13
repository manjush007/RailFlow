package com.shashi.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.shashi.beans.TrainBean;
import com.shashi.beans.TrainException;
import com.shashi.constant.UserRole;
import com.shashi.service.TrainService;
import com.shashi.service.impl.TrainServiceImpl;
import com.shashi.utility.TrainUtil;

@SuppressWarnings("serial")
@WebServlet("/trainbwstn")
public class TrainBwStn extends HttpServlet {
	private TrainService trainService = new TrainServiceImpl();

	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();
		TrainUtil.validateUserAuthorization(req, UserRole.CUSTOMER);
		try {
			String fromStation = req.getParameter("fromstation");
			String toStation = req.getParameter("tostation");
			List<TrainBean> trains = trainService.getTrainsBetweenStations(fromStation, toStation);
			if (trains != null && !trains.isEmpty()) {
				RequestDispatcher rd = req.getRequestDispatcher("UserHome.html");
				rd.include(req, res);
				pw.println("<div class='main'><p1 class='menu'>Trains BetWeen Station "
						+ req.getParameter("fromstation") + " and " + req.getParameter("tostation") + "</p1></div>");
				TrainBean cheapestTrain = null;
				TrainBean mostSeatsTrain = null;
				for (TrainBean train : trains) {
					if (train.getSeats() <= 0) {
						continue;
					}
					if (cheapestTrain == null || train.getFare() < cheapestTrain.getFare()) {
						cheapestTrain = train;
					}
					if (mostSeatsTrain == null || train.getSeats() > mostSeatsTrain.getSeats()) {
						mostSeatsTrain = train;
					}
				}
				pw.println("<section class='trip-suggestions'><h2>Smart trip suggestions</h2>"
						+ "<p>Choose the lowest fare or the train with the most seats available. "
						+ "Schedule times are not available in this app yet.</p>");
				if (cheapestTrain == null) {
					pw.println("<p class='suggestion-empty'>No seats are currently available on this route.</p>");
				} else if (mostSeatsTrain == cheapestTrain) {
					printSuggestion(pw, cheapestTrain, "Best overall", "Lowest fare and most seats available");
				} else {
					printSuggestion(pw, cheapestTrain, "Lowest fare", "Lowest fare among trains with seats");
					printSuggestion(pw, mostSeatsTrain, "Most seats", "Most seats currently available");
				}
				pw.println("</section>");
				pw.println("<div class='tab'><table><tr><th>Train Name</th><th>Train No</th>"
						+ "<th>From Stn</th><th>To Stn</th><th>Seats</th><th>Fare (INR)</th><th>Action</th></tr>");
				for (TrainBean train : trains) {
					pw.println("" + "<tr><td>" + train.getTr_name() + "</td>" + "<td>" + train.getTr_no() + "</td>"
							+ "<td>" + train.getFrom_stn() + "</td>" + "<td>" + train.getTo_stn() + "</td>"
							+ "<td>" + train.getSeats() + "</td>" + "<td>" + train.getFare()
							+ " RS</td><td><a href='booktrainbyref?trainNo=" + train.getTr_no() + "&fromStn="
							+ train.getFrom_stn() + "&toStn=" + train.getTo_stn()
							+ "'><div class='red'>Book Now</div></a></td>" + "</tr>");
				}
				pw.println("</table></div>");
			} else {
				RequestDispatcher rd = req.getRequestDispatcher("TrainBwStn.html");
				rd.include(req, res);
				pw.println("<div class='tab'><p1 class='menu'>There are no trains Between "
						+ req.getParameter("fromstation") + " and " + req.getParameter("tostation") + "</p1></div>");
			}
		} catch (Exception e) {
			throw new TrainException(422, this.getClass().getName() + "_FAILED", e.getMessage());
		}

	}

	private void printSuggestion(PrintWriter pw, TrainBean train, String title, String detail) {
		pw.println("<article class='suggestion-card'><span class='suggestion-label'>" + title
				+ "</span><h3>" + train.getTr_name() + "</h3><p>" + train.getFrom_stn() + " to "
				+ train.getTo_stn() + "</p><p>" + detail + "</p><p><strong>INR " + train.getFare()
				+ "</strong> &middot; " + train.getSeats() + " seats available</p><a class='suggestion-book' href='booktrainbyref?trainNo="
				+ train.getTr_no() + "&fromStn=" + train.getFrom_stn() + "&toStn=" + train.getTo_stn()
				+ "'>Book this train</a></article>");
	}
}
