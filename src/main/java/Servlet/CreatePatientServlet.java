package Servlet;

import DataAccessObject.PatientDAO;
import Models.Patient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
// Servlet that handles creating a new patient via POST request
@WebServlet(urlPatterns = {"/api/patient/create"})
public class CreatePatientServlet extends HttpServlet {

    // Gson instance for JSON parsing and serialization
    private static final Gson GSON = new Gson();

    // Handles POST requests for creating a patient
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // Configure response as JSON
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            // Read request body as UTF-8 string
            String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Parse JSON body into object
            JsonObject in = new JsonParser().parse(body).getAsJsonObject();

            // Extract doctor (default to "demo" if not provided)
            String doctor = in.has("doctor") ? in.get("doctor").getAsString() : "demo";

            // Extract patient details from request
            String given = in.get("givenname").getAsString();
            String family = in.get("familyname").getAsString();
            String gender = in.get("gender").getAsString();
            int age = in.get("age").getAsInt();
            String bp = in.get("bp").getAsString();

            // Create patient model (ID assigned by database)
            Patient p = new Patient(
                    0,
                    given,
                    family,
                    gender,
                    age,
                    bp
            );

            // Insert patient and retrieve generated ID
            int id = PatientDAO.insertPatientForDoctor(doctor, p);

            // Build success response
            JsonObject out = new JsonObject();
            out.addProperty("ok", true);
            out.addProperty("id", id);

            // Send success response
            resp.setStatus(200);
            resp.getWriter().write(GSON.toJson(out));

        } catch (Exception e) {
            // Log error to server console
            e.printStackTrace();

            // Build error response
            JsonObject out = new JsonObject();
            out.addProperty("ok", false);
            out.addProperty("error", e.getClass().getSimpleName() + ": " + e.getMessage());

            // Send error response
            resp.setStatus(500);
            resp.getWriter().write(GSON.toJson(out));
        }
    }
}