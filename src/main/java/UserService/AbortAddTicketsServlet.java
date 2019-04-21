package UserService;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Servlet for aborting add tickets progress.
 */
public class AbortAddTicketsServlet extends BaseServlet {

    protected Logger log = Logger.getLogger(AbortAddTicketsServlet.class);

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {

            JSONObject obj = getJSON(request.getReader());
            UserServiceDatabase db = UserServiceDatabase.getInstance();
            int userid = (int)(long) obj.get("userid");
            int timestamp = (int)(long) obj.get("timestamp");
            int eventid = (int) (long) obj.get("eventid");
            int tickets = (int) (long) obj.get("tickets");
            db.abortAddTickets(userid, eventid, tickets, timestamp);
            response.setStatus(200);

        }
        catch(Exception ex) {
            log.info(ex);
            response.setStatus(400);
        }
        response.getWriter().close();
    }



}
