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

@WebServlet("/deleteAccount")
public class DeleteAccountServlet extends HttpServlet {

    private static class DeleteAccountRequest {
        String email;
        String password;

        // optional: require typing "DELETE" to confirm (client can send it)
        String confirm;
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
        PrintWriter out = resp.getWriter();

        // Read JSON body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while((line = reader.readLine()) != null) sb.append(line);
        }

        DeleteAccountRequest delReq = gson.fromJson(sb.toString(), DeleteAccountRequest.class);

        // Basic validation
        if (delReq == null || delReq.email == null || delReq.password == null ||
                delReq.email.isBlank() || delReq.password.isBlank()) {
            resp.setStatus(400);
            out.println(gson.toJson(new SimpleResponse("error", "Missing email or password")));
            return;
        }

        // Optional hard confirmation
        // If you don't want this, delete this block.
        if (delReq.confirm == null || !delReq.confirm.equals("DELETE")) {
            resp.setStatus(400);
            out.println(gson.toJson(new SimpleResponse("error", "Confirmation required (confirm=DELETE)")));
            return;
        }

        // Verify doctor exists
        Doctor d = DoctorDAO.findByEmail(delReq.email.trim());
        if (d == null) {
            resp.setStatus(404);
            out.println(gson.toJson(new SimpleResponse("error", "Email not found")));
            return;
        }

        // Verify password (same hash as register/login)
        String passwordHash = Integer.toHexString(delReq.password.hashCode());
        if (!passwordHash.equals(d.getPasswordHash())) {
            resp.setStatus(401);
            out.println(gson.toJson(new SimpleResponse("error", "Wrong password")));
            return;
        }

        // Perform hard delete
        boolean ok = DoctorDAO.hardDeleteByEmail(d.getEmail());
        if (!ok) {
            resp.setStatus(500);
            out.println(gson.toJson(new SimpleResponse("error", "Delete failed")));
            return;
        }

        out.println(gson.toJson(new SimpleResponse("ok", "Account deleted")));
    }
}