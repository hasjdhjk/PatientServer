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
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet(urlPatterns = {"/api/patient"})
public class PatientApiServlet extends HttpServlet {

    private static final Gson GSON = new Gson();

    // =========================
    // GET /api/patient?id=1&doctor=demo
    // return: { ok:true, id, givenname, familyname, hr, sys, dia, temp }
    // =========================
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String doctor = req.getParameter("doctor");
        if (doctor == null || doctor.isBlank()) doctor = "demo";

        String idStr = req.getParameter("id");
        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (Exception e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"ok\":false,\"error\":\"Missing/invalid id\"}");
            return;
        }

        try {
            Patient p = PatientDAO.getPatientByIdForDoctor(doctor, id);
            if (p == null) {
                resp.setStatus(404);
                resp.getWriter().write("{\"ok\":false,\"error\":\"Patient not found\"}");
                return;
            }

            int sys = 0, dia = 0;
            try {
                String[] parts = p.getBloodPressure().split("/");
                sys = Integer.parseInt(parts[0].trim());
                dia = Integer.parseInt(parts[1].trim());
            } catch (Exception ignore) {}

            JsonObject out = new JsonObject();
            out.addProperty("ok", true);
            out.addProperty("id", p.getId());
            out.addProperty("givenname", p.getGivenName());
            out.addProperty("familyname", p.getFamilyName());

            out.addProperty("gender", p.getGender());
            out.addProperty("sys", sys);
            out.addProperty("dia", dia);
            out.addProperty("age", p.getAge());

            out.addProperty("rr", 0);
            out.addProperty("spo2", 0);

            resp.getWriter().write(out.toString());

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"ok\":false,\"error\":\"" + e.getClass().getSimpleName() + ": " + e.getMessage() + "\"}");
        }
    }

    // =========================
    // POST /api/patient
    // body: {doctor,givenname,familyname,heartrate,temperature,bp}
    // return: { ok:true, id: <newId> }
    // =========================
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String body = readBody(req);

        try {
            JsonObject in = new JsonParser().parse(body).getAsJsonObject();

            String doctor = getStr(in, "doctor");
            String givenname = getStr(in, "givenname");
            String familyname = getStr(in, "familyname");
            String gender = getStr(in, "gender");
            int age = getInt(in, "age");
            String bp = getStr(in, "bp");

            if (doctor == null || givenname == null || familyname == null || bp == null
                    || gender == null || age <= 0) {
                resp.setStatus(400);
                resp.getWriter().write("{\"ok\":false,\"error\":\"Missing/invalid fields. Expect doctor,givenname,familyname,heartrate,temperature,bp\"}");
                return;
            }

            Patient p = new Patient(0, givenname, familyname, gender, age, bp);

            int newId = PatientDAO.insertPatientForDoctor(doctor, p);
            if (newId <= 0) {
                resp.setStatus(500);
                resp.getWriter().write("{\"ok\":false,\"error\":\"Insert failed (returned id=" + newId + ")\"}");
                return;
            }

            JsonObject out = new JsonObject();
            out.addProperty("ok", true);
            out.addProperty("id", newId);
            resp.getWriter().write(out.toString());

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"ok\":false,\"error\":\"" + e.getClass().getSimpleName() + ": " + e.getMessage() + "\"}");
        }
    }

    private static String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = req.getReader()) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static String getStr(JsonObject in, String k) {
        return (in.has(k) && !in.get(k).isJsonNull()) ? in.get(k).getAsString() : null;
    }

    private static int getInt(JsonObject in, String k) {
        try {
            return (in.has(k) && !in.get(k).isJsonNull()) ? in.get(k).getAsInt() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static double getDouble(JsonObject in, String k) {
        try {
            return (in.has(k) && !in.get(k).isJsonNull()) ? in.get(k).getAsDouble() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
}