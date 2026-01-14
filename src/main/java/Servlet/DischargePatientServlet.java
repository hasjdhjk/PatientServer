package Servlet;

import DataAccessObject.PatientDAO;
import com.google.gson.Gson;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
// Servlet that handles discharging (deleting) a patient
@WebServlet(urlPatterns = {"/api/patient/discharge"})
public class DischargePatientServlet extends HttpServlet {

    // Gson instance for JSON responses
    private static final Gson GSON = new Gson();

    // Handles POST request to discharge a patient
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Read request parameters
        String doctor = req.getParameter("doctor");
        String idStr = req.getParameter("id");

        // Default doctor if not provided
        if (doctor == null || doctor.isBlank()) doctor = "demo";

        // Parse patient ID
        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (Exception e) {
            write(resp, 400, false, "Missing/invalid id");
            return;
        }

        try {
            // Attempt to delete patient for doctor
            boolean ok = PatientDAO.deletePatientForDoctor(doctor, id);
            if (!ok) {
                write(resp, 500, false, "Delete failed (not found or DB error)");
                return;
            }
            // Send success response
            write(resp, 200, true, null);

        } catch (Exception e) {
            // Log server error
            e.printStackTrace();
            // Send error response
            write(resp, 500, false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // Writes a standard JSON response
    private void write(HttpServletResponse resp, int code, boolean ok, String error) throws IOException {
        // Configure response
        resp.setStatus(code);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Build response map
        var out = new java.util.HashMap<String, Object>();
        out.put("ok", ok);
        if (error != null) out.put("error", error);

        // Send JSON response
        resp.getWriter().write(GSON.toJson(out));
    }
}