package Servlet;

import DataAccessObject.DoctorDAO;
import Models.Doctor;
import com.google.gson.Gson;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {"/api/doctors"})
public class DoctorsApiServlet extends HttpServlet {

    private static final Gson GSON = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            List<Doctor> doctors = DoctorDAO.getAllDoctors();
            resp.getWriter().write(GSON.toJson(doctors));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"ok\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}