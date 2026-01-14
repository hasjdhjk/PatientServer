package Servlet;

import Models.Patient;
import DataAccessObject.PatientDAO;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {"/api/patients"})
public class PatientsApiServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String doctor = req.getParameter("doctor");
        if (doctor == null || doctor.isBlank()) doctor = "demo";

        List<Patient> list = PatientDAO.getPatientsForDoctor(doctor);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Gson gson = new Gson();
        resp.getWriter().write(gson.toJson(list));
    }

    // POST /api/patients?action=clearDemo&doctor=demo
    // clears all demo patients (no password), returns { ok:true, deleted:n }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String action = req.getParameter("action");
        if (action == null) action = "";

        if (!"clearDemo".equalsIgnoreCase(action)) {
            resp.setStatus(400);
            resp.getWriter().write("{\"ok\":false,\"error\":\"Unknown action. Use ?action=clearDemo\"}");
            return;
        }

        String doctor = req.getParameter("doctor");
        if (doctor == null || doctor.isBlank()) doctor = "demo";

        if (!"demo".equalsIgnoreCase(doctor.trim())) {
            resp.setStatus(403);
            resp.getWriter().write("{\"ok\":false,\"error\":\"Forbidden: only demo can be cleared\"}");
            return;
        }

        int deleted = PatientDAO.clearDemoPatients();

        JsonObject out = new JsonObject();
        out.addProperty("ok", true);
        out.addProperty("deleted", deleted);
        resp.getWriter().write(out.toString());
    }
}