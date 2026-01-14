package Servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
// Servlet that serves static Digital Twin dashboard resources
@WebServlet(urlPatterns = {
        "/digital_twin/dashboard.html",
        "/digital_twin/body.png"
})
public class DigitalTwinDashboardServlet extends HttpServlet {

    // Streams a classpath resource to the HTTP response
    private void serveResource(HttpServletResponse resp, String resourcePath, String contentType)
            throws IOException {

        // Set response content type
        resp.setContentType(contentType);

        // Load resource from classpath and stream to client
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("Not found: " + resourcePath);
                return;
            }
            in.transferTo(resp.getOutputStream());
        }
    }

    // Handles GET requests for Digital Twin resources
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Get full request URI
        String uri = req.getRequestURI(); // e.g. /PatientServer/digital_twin/dashboard.html

        // Serve dashboard HTML
        if (uri.endsWith("/digital_twin/dashboard.html")) {
            serveResource(resp, "/digital_twin/dashboard.html", "text/html; charset=UTF-8");
            return;
        }

        // Serve body image
        if (uri.endsWith("/digital_twin/body.png")) {
            serveResource(resp, "/digital_twin/body.png", "image/png");
            return;
        }

        // Handle unknown resource requests
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.getWriter().write("Unknown resource.");
    }
}