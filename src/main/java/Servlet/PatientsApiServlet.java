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

// Handles requests to /api/patients
@WebServlet(urlPatterns = {"/api/patients"})
// rest api for listing patients (and clearing demo data)
public class PatientsApiServlet extends HttpServlet {
    // List patients for a doctor
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // doctor email (default demo)
        String doctor = req.getParameter("doctor");
        if (doctor == null || doctor.isBlank()) doctor = "demo";

        // load list from database
        List<Patient> list = PatientDAO.getPatientsForDoctor(doctor);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Gson gson = new Gson();
        resp.getWriter().write(gson.toJson(list));
    }

    // clear demo patients (POST /api/patients?action=clearDemo&doctor=demo)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // action=clearDemo
        String action = req.getParameter("action");
        if (action == null) action = "";

        // only allow clearDemo
        if (!"clearDemo".equalsIgnoreCase(action)) {
            resp.setStatus(400);
            resp.getWriter().write("{\"ok\":false,\"error\":\"Unknown action. Use ?action=clearDemo\"}");
            return;
        }

        // only demo is allowed (no password)
        String doctor = req.getParameter("doctor");
        if (doctor == null || doctor.isBlank()) doctor = "demo";

        if (!"demo".equalsIgnoreCase(doctor.trim())) {
            resp.setStatus(403);
            resp.getWriter().write("{\"ok\":false,\"error\":\"Forbidden: only demo can be cleared\"}");
            return;
        }

        // delete rows in database
        int deleted = PatientDAO.clearDemoPatients();

        // build response JSON
        JsonObject out = new JsonObject();
        out.addProperty("ok", true);
        out.addProperty("deleted", deleted);
        resp.getWriter().write(out.toString());
    }
}