@WebServlet("/resetPassword")
public class ResetPasswordServlet extends HttpServlet {

    private static class ResetPasswordReq {
        String token;
        String newPassword;
    }

    private static class SimpleResponse {
        String status;
        String message;
        SimpleResponse(String status, String message) {
            this.status = status; this.message = message;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");
        Gson gson = new Gson();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        ResetPasswordReq r = gson.fromJson(sb.toString(), ResetPasswordReq.class);

        PrintWriter out = resp.getWriter();

        Doctor d = DoctorDAO.findByResetToken(r.token);
        if (d == null) {
            out.println(gson.toJson(new SimpleResponse("error", "Invalid or expired token")));
            return;
        }

        String newHash = Integer.toHexString(r.newPassword.hashCode());
        DoctorDAO.updatePassword(d.getId(), newHash);

        out.println(gson.toJson(new SimpleResponse("ok", "Password updated")));
    }
}
