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

    private static final Gson GSON = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            JsonObject in = new JsonParser().parse(body).getAsJsonObject();

            String doctor = in.has("doctor") ? in.get("doctor").getAsString() : "demo";
            String given = in.get("givenname").getAsString();
            String family = in.get("familyname").getAsString();
            String gender = in.get("gender").getAsString();
            int age = in.get("age").getAsInt();
            String bp = in.get("bp").getAsString();

            Patient p = new Patient(
                    0,
                    given,
                    family,
                    gender,
                    age,
                    bp
            );

            int id = PatientDAO.insertPatientForDoctor(doctor, p);

            JsonObject out = new JsonObject();
            out.addProperty("ok", true);
            out.addProperty("id", id);

            resp.setStatus(200);
            resp.getWriter().write(GSON.toJson(out));

        } catch (Exception e) {
            e.printStackTrace();

            JsonObject out = new JsonObject();
            out.addProperty("ok", false);
            out.addProperty("error", e.getClass().getSimpleName() + ": " + e.getMessage());

            resp.setStatus(500);
            resp.getWriter().write(GSON.toJson(out));
        }
    }
}