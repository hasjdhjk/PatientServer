package Servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;

@WebServlet(urlPatterns = {
        "/digital_twin/dashboard.html",
        "/digital_twin/body.png"
})
public class DigitalTwinDashboardServlet extends HttpServlet {

    private void serveResource(HttpServletResponse resp, String resourcePath, String contentType)
            throws IOException {

        resp.setContentType(contentType);

        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("Not found: " + resourcePath);
                return;
            }
            in.transferTo(resp.getOutputStream());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String uri = req.getRequestURI(); // e.g. /PatientServer/digital_twin/dashboard.html

        if (uri.endsWith("/digital_twin/dashboard.html")) {
            serveResource(resp, "/digital_twin/dashboard.html", "text/html; charset=UTF-8");
            return;
        }

        if (uri.endsWith("/digital_twin/body.png")) {
            serveResource(resp, "/digital_twin/body.png", "image/png");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.getWriter().write("Unknown resource.");
    }
}