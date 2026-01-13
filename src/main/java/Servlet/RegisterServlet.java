package Servlet;

import com.google.gson.Gson;
import DataAccessObject.DoctorDAO;
import Utils.EmailSender;
import jakarta.mail.MessagingException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static class RegisterRequest {
        String email;
        String password;
        String givenName;
        String familyName;
    }

    private static class SimpleResponse {
        String status;
        String message;
        SimpleResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");
        Gson gson = new Gson();

        // 1. Read JSON body (same pattern as Tutorial 7) :contentReference[oaicite:5]{index=5}
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        RegisterRequest registerReq = gson.fromJson(sb.toString(), RegisterRequest.class);

        PrintWriter out = resp.getWriter();

        // Basic validation to avoid NPEs / blank input
        if (registerReq == null || registerReq.email == null || registerReq.password == null ||
                registerReq.givenName == null || registerReq.familyName == null ||
                registerReq.email.isBlank() || registerReq.password.isBlank() ||
                registerReq.givenName.isBlank() || registerReq.familyName.isBlank()) {
            resp.setStatus(400);
            resp.getWriter().println(gson.toJson(new SimpleResponse("error", "Missing required fields")));
            return;
        }

        // 2. Check if email already exists
        var existing = DoctorDAO.findByEmail(registerReq.email);
        if (existing != null) {
            out.println(gson.toJson(new SimpleResponse("error", "Email already registered")));
            return;
        }

        // 3. Hash password (for a uni project you could keep it simple, but in real apps use a proper password hasher)
        String passwordHash = Integer.toHexString(registerReq.password.hashCode());

        // 4. Generate verification token
        String token = UUID.randomUUID().toString();

        // 5. Insert doctor (throws if insert fails)
        try {
            int newDoctorId = DoctorDAO.insertDoctor(
                    registerReq.email,
                    registerReq.givenName,
                    registerReq.familyName,
                    passwordHash,
                    token
            );

            // TEMPORARY: auto-verify user (skip email verification)
            if (newDoctorId > 0) {
                DoctorDAO.markVerified(newDoctorId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            out.println(gson.toJson(new SimpleResponse("error", "Registration failed: " + e.getMessage())));
            return;
        }

        // 6. Send verification email
        String appBaseUrl = System.getenv("APP_BASE_URL"); // tsuru website
        // testing url
        if (appBaseUrl == null || appBaseUrl.isEmpty()) {
            System.out.println(appBaseUrl);
            appBaseUrl = "http://localhost:8080/PatientServer";
        }
        String verifyLink = appBaseUrl + "/verifyEmail?token=" + token;

        String subject = "Verify your HealthTrack account";
        String body = "Hi " + registerReq.givenName + ",\n\n" +
                "Please click the link below to verify your account:\n" +
                verifyLink + "\n\n" +
                "If you did not register, ignore this email.";

//        try {
//            EmailSender.sendEmail(registerReq.email, subject, body);
//        } catch (MessagingException e) {
//            e.printStackTrace();
//            out.println(gson.toJson(new SimpleResponse("error", "Failed to send verification email")));
//            return;
//        }
        // DISABLE EMAIL FOR DEBUG
        System.out.println("EMAIL DISABLED - WOULD SEND TO: " + registerReq.email);


        // 7. Respond OK
        out.println(gson.toJson(new SimpleResponse("ok", "Registration successful. Check your email.")));
    }

    // testing
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/plain");
        resp.getWriter().write("Hello from /register on Tsuru!");
    }
}
