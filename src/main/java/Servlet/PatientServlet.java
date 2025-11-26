package Servlet;

@WebServlet("/getPatients")
public class PatientServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");

        List<Patient> patients = PatientDAO.getAllPatients();

        Gson gson = new Gson();
        String json = gson.toJson(patients);

        resp.getWriter().println(json);
    }
}
