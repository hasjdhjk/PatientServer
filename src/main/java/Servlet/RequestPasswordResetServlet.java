package Servlet;

import DataAccessObject.DoctorDAO;
import Models.Doctor;

import Utils.EmailSender;
import com.google.gson.Gson;
import jakarta.mail.MessagingException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.UUID;


@WebServlet("/requestPasswordReset")
public class RequestPasswordResetServlet extends HttpServlet {

    private static class ResetReq {
        String email;
    }

    private static class SimpleResponse {
        String status;
        String message;
        SimpleResponse(String status, String message) {
            this.status = status; this.message = message;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");
        Gson gson = new Gson();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        ResetReq r = gson.fromJson(sb.toString(), ResetReq.class);

        PrintWriter out = resp.getWriter();

        Doctor d = DoctorDAO.findByEmail(r.email);
        // For security, you usually still respond "ok" even if not found
        if (d == null) {
            out.println(gson.toJson(new SimpleResponse("ok", "If that email exists, a reset link was sent.")));
            return;
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expires = LocalDateTime.now().plusHours(1);
        DoctorDAO.setResetToken(d.getId(), token, expires);

        String appBaseUrl = System.getenv("APP_BASE_URL");
        String link = appBaseUrl + "/resetPasswordForm?token=" + token;

        String subject = "Reset your HealthTrack password";
        String body = "Hi " + d.getGivenName() + ",\n\n" +
                "Use the link below to reset your password (valid for 1 hour):\n" +
                link;

        try {
            EmailSender.sendEmail(d.getEmail(), subject, body);
        } catch (MessagingException e) {
            e.printStackTrace();
            out.println(gson.toJson(new SimpleResponse("error", "Failed to send email")));
            return;
        }

        out.println(gson.toJson(new SimpleResponse("ok", "If that email exists, a reset link was sent.")));
    }
}
