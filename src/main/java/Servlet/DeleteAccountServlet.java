package Servlet;

import com.google.gson.Gson;
import DataAccessObject.DoctorDAO;
import Models.Doctor;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
// Servlet that handles permanent doctor account deletion
@WebServlet("/deleteAccount")
public class DeleteAccountServlet extends HttpServlet {

    // DTO for delete account request body
    private static class DeleteAccountRequest {
        String email;
        String password;

        // optional: require typing "DELETE" to confirm (client can send it)
        String confirm;
    }

    // Simple JSON response wrapper
    private static class SimpleResponse {
        String status;
        String message;
        // Constructs a response with status and message
        SimpleResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    // Handles POST request for deleting an account
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // Set response content type
        resp.setContentType("application/json");

        // Initialise Gson and output writer
        Gson gson = new Gson();
        PrintWriter out = resp.getWriter();

        // Read JSON request body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while((line = reader.readLine()) != null) sb.append(line);
        }

        // Deserialize request JSON into object
        DeleteAccountRequest delReq = gson.fromJson(sb.toString(), DeleteAccountRequest.class);

        // Validate required fields
        if (delReq == null || delReq.email == null || delReq.password == null ||
                delReq.email.isBlank() || delReq.password.isBlank()) {
            resp.setStatus(400);
            out.println(gson.toJson(new SimpleResponse("error", "Missing email or password")));
            return;
        }

        // Require explicit DELETE confirmation
        if (delReq.confirm == null || !delReq.confirm.equals("DELETE")) {
            resp.setStatus(400);
            out.println(gson.toJson(new SimpleResponse("error", "Confirmation required (confirm=DELETE)")));
            return;
        }

        // Look up doctor by email
        Doctor d = DoctorDAO.findByEmail(delReq.email.trim());
        if (d == null) {
            resp.setStatus(404);
            out.println(gson.toJson(new SimpleResponse("error", "Email not found")));
            return;
        }

        // Hash and verify password
        String passwordHash = Integer.toHexString(delReq.password.hashCode());
        if (!passwordHash.equals(d.getPasswordHash())) {
            resp.setStatus(401);
            out.println(gson.toJson(new SimpleResponse("error", "Wrong password")));
            return;
        }

        // Permanently delete doctor account
        boolean ok = DoctorDAO.hardDeleteByEmail(d.getEmail());
        if (!ok) {
            resp.setStatus(500);
            out.println(gson.toJson(new SimpleResponse("error", "Delete failed")));
            return;
        }

        out.println(gson.toJson(new SimpleResponse("ok", "Account deleted")));
    }
}