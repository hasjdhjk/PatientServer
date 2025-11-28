package Servlet;

import DataAccessObject.DoctorDAO;
import Models.Doctor;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/verifyEmail")
public class VerifyEmailServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String token = req.getParameter("token");
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        if (token == null || token.isEmpty()) {
            out.println("Invalid verification link.");
            return;
        }

        Doctor d = DoctorDAO.findByVerificationToken(token);
        if (d == null) {
            out.println("Invalid or already used verification token.");
            return;
        }

        DoctorDAO.markVerified(d.getId());
        out.println("Email verified! You can now log in.");
    }
}
