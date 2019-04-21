package UserService;


import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class ElectionServlet extends BaseServlet {

    protected Logger log = Logger.getLogger(ElectionServlet.class);


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int status;
        try {
            UserServiceDatabase db = UserServiceDatabase.getInstance();
            Thread t = db.getThread();
            if(db.getAlive() == true) {
                t.interrupt();
            }
            JSONObject obj = getJSON(request.getReader());
            int timestamp = (int)(long) obj.get("timestamp");
            int uid = (int)(long) obj.get("uid");
            String server = (String) obj.get("server");
            log.info("get election request from server @ " + server);

            int mytime = db.getTimeStamp();
            int myuid = db.getUID();
            if(mytime < timestamp) {
                status = 400;
            }
            else {
                if(mytime == timestamp) {
                    if(uid < myuid) {
                        status = 400;

                    }
                    else {
                        status = 200;
                    }
                }
                else {
                    status = 200;
                }
            }
        }
        catch(Exception ex) {
            log.debug(ex);
            status = 400;
        }
        if(status == 400) {
            ElectionHandler handler = new ElectionHandler();
            int code = handler.startElection();
            if(code == 200) {
                handler.becomePrimary();
            }
        }

        response.setStatus(status);
        response.getWriter().close();

    }

}