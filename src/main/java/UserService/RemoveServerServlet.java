package UserService;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet for remove a secondary.
 */
public class RemoveServerServlet extends BaseServlet {

    protected Logger log = Logger.getLogger(RemoveServerServlet.class);


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JSONObject obj = getJSON(request.getReader());
            String server = (String) obj.get("server");
            int timestamp = (int)(long) obj.get("timestamp");

            UserServiceDatabase db = UserServiceDatabase.getInstance();
            db.removeServer(server, timestamp);
            response.setStatus(200);
        }
        catch(Exception ex) {
            log.debug(ex);
        }
        response.getWriter().close();

    }

}