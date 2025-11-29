package Servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

// only for testing, it says hello
// https://bioeng-fguys-app.impaas.uk/hello
@WebServlet(urlPatterns = {"/hello"})
public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/plain");
//        resp.getWriter().write("Hi! Server is running on Tsuru!");
        resp.getWriter().write("任建坤大傻逼!");
    }
}
