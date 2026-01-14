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


// Servlet that returns all doctors as JSON
@WebServlet(urlPatterns = {"/api/doctors"})
public class DoctorsApiServlet extends HttpServlet {

    // Gson instance for JSON serialization
    private static final Gson GSON = new Gson();

    // Handles GET request to fetch all doctors
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Configure response as JSON
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            // Fetch all doctors from database
            List<Doctor> doctors = DoctorDAO.getAllDoctors();
            // Write doctors list as JSON
            resp.getWriter().write(GSON.toJson(doctors));
        } catch (Exception e) {
            // Log server error
            e.printStackTrace();
            // Send error response
            resp.setStatus(500);
            resp.getWriter().write("{\"ok\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}