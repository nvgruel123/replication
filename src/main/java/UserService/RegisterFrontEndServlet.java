package UserService;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RegisterFrontEndServlet extends BaseServlet {

    protected Logger log = Logger.getLogger(RegisterFrontEndServlet.class);


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            try {
                UserServiceDatabase db = UserServiceDatabase.getInstance();
                boolean primary = db.isPrimary();
                int time = 0;
                if(primary) {
                    JSONObject obj = getJSON(request.getReader());
                    ConcurrentLinkedQueue<String> membership = db.getMembership();

                    String host = (String) obj.get("host");
                    int port = (int)(long) obj.get("port");
                    String uri = "http://" + host + ":" + port;
                    synchronized (this) {

                        db.commitAddFrontEnd(uri, time);
                        time = db.getTimeStamp();
                        obj.put("timestamp", time);
                        for (String server : membership) {

                            URL url = new URL(server + "/register/frontend");
                            SendRequest(url, obj);

                        }

                        response.setStatus(200);
                    }
                }
                else {
                    JSONObject obj = getJSON(request.getReader());
                    String host = (String) obj.get("host");
                    int port = (int)(long) obj.get("port");
                    time = (int)(long) obj.get("timestamp");
                    String uri = "http://" + host + ":" + port;
                    db.commitAddFrontEnd(uri, time);
                    response.setStatus(200);
                }
            }
            catch(Exception ex) {
                log.debug(ex);
                response.setStatus(400);
            }
        }
        catch(Exception ex) {
            log.debug(ex);
        }
        response.getWriter().close();

    }
}
