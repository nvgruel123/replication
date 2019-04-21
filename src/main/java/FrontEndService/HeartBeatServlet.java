package FrontEndService;

import UserService.BaseServlet;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet for getting heart beat message from primary user server
 */
public class HeartBeatServlet extends BaseServlet {

    protected Logger log = Logger.getLogger(UserService.HeartBeatServlet.class);


    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(200);
        response.getWriter().close();
    }
}
