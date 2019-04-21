package UserService;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Servlet for aborting user create progress
 */
public class AbortUserCreateServlet extends BaseServlet {


    protected Logger log = Logger.getLogger(AbortUserCreateServlet.class);

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {

            JSONObject obj = getJSON(request.getReader());
            UserServiceDatabase db = UserServiceDatabase.getInstance();
            int userid = (int)(long) obj.get("userid");
            int timestamp = (int)(long) obj.get("timestamp");
            db.abortAddUser(userid, timestamp);
            response.setStatus(200);

        }
        catch(Exception ex) {
            log.info(ex);
        }
        response.getWriter().close();

    }



}
