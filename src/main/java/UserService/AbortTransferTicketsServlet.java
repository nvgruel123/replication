package UserService;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet for aborting transfer tickets progress.
 */
public class AbortTransferTicketsServlet extends BaseServlet {

    protected Logger log = Logger.getLogger(AbortTransferTicketsServlet.class);

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {

            JSONObject obj = getJSON(request.getReader());
            UserServiceDatabase db = UserServiceDatabase.getInstance();
            int userid = (int)(long) obj.get("userid");
            int timestamp = (int)(long) obj.get("timestamp");
            int eventid = (int) (long) obj.get("eventid");
            int target = (int)(long) obj.get("targetuser");
            int tickets = (int) (long) obj.get("tickets");
            db.abortTransferTickets(userid, eventid, tickets, target, timestamp);
            response.setStatus(200);

        }
        catch(Exception ex) {
            log.info(ex);
        }
        response.getWriter().close();

    }
}
