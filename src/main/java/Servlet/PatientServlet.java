//package Servlet;
//
//import DataAccessObject.PatientDAO;
//
//import Models.Patient;
//import com.google.gson.Gson;
//import jakarta.servlet.annotation.WebServlet;
//import jakarta.servlet.http.*;
//
//import java.io.IOException;
//import java.util.List;
//
//
//@WebServlet("/getPatients")
//public class PatientServlet extends HttpServlet {
//
//    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
//            throws IOException {
//
//        resp.setContentType("application/json");
//
//        List<Patient> patients = PatientDAO.getAllPatients();
//
//        Gson gson = new Gson();
//        String json = gson.toJson(patients);
//
//        resp.getWriter().println(json);
//    }
//}
