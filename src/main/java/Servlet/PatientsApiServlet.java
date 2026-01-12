package Servlet;

import Models.Patient;
import DataAccessObject.PatientDAO;
import com.google.gson.Gson;

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
}