package Servlet;

import DataAccessObject.PatientDAO;
import com.google.gson.Gson;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = {"/api/patient/discharge"})
public class DischargePatientServlet extends HttpServlet {

    private static final Gson GSON = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String doctor = req.getParameter("doctor");
        String idStr = req.getParameter("id");

        if (doctor == null || doctor.isBlank()) doctor = "demo";

        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (Exception e) {
            write(resp, 400, false, "Missing/invalid id");
            return;
        }

        try {
            boolean ok = PatientDAO.deletePatientForDoctor(doctor, id);
            if (!ok) {
                write(resp, 500, false, "Delete failed (not found or DB error)");
                return;
            }
            write(resp, 200, true, null);

        } catch (Exception e) {
            e.printStackTrace();
            write(resp, 500, false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void write(HttpServletResponse resp, int code, boolean ok, String error) throws IOException {
        resp.setStatus(code);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        var out = new java.util.HashMap<String, Object>();
        out.put("ok", ok);
        if (error != null) out.put("error", error);

        resp.getWriter().write(GSON.toJson(out));
    }
}