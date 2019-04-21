package UserService;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Servlet for updating primary info and database.
 */
public class UpdateServlet extends BaseServlet {

    protected Logger log = Logger.getLogger(UpdateServlet.class);


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            BufferedReader in = request.getReader();
            JSONObject obj = getJSON(in);

            String primary = (String) obj.get("primary");

            log.info("update primary to " + primary);
            setMaps(obj);
            log.info("database update");
            response.setStatus(200);
        }
        catch(Exception ex) {
            log.debug(ex);
            response.setStatus(400);
        }
        response.getWriter().close();

    }

    private synchronized void setMaps(JSONObject obj) {

        UserServiceDatabase db = UserServiceDatabase.getInstance();
        String primary = (String) obj.get("primary");
        int timestamp = (int)(long)obj.get("timestamp");
        JSONArray user = (JSONArray) obj.get("userinfo");
        JSONArray ticket = (JSONArray) obj.get("ticketinfo");
        JSONArray membership = (JSONArray) obj.get("membership");
        JSONArray frontend = (JSONArray) obj.get("frontend");
        db.setPrimary(primary);
        db.setTimeStamp(timestamp);
        db.setPrimary(primary);
        ConcurrentHashMap<Integer, String> userMap = new ConcurrentHashMap<>();
        for(int i = 0; i < user.size(); i ++) {
            JSONObject tmp = (JSONObject) user.get(i);
            int userid = (int)(long) tmp.get("userid");
            String username = (String) tmp.get("username");
            userMap.put(userid, username);
        }
        db.setUserMap(userMap);
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> ticketMap = new ConcurrentHashMap<>();
        for(int i = 0; i < ticket.size(); i ++) {
            JSONObject tmp = (JSONObject) ticket.get(i);
            int userid = (int)(long) tmp.get("userid");
            JSONArray array = (JSONArray) tmp.get("eventmap");
            ConcurrentHashMap<Integer, Integer> eventMap = new ConcurrentHashMap<>();
            for(int j = 0; j < array.size(); j++) {
                JSONObject tmp1 = (JSONObject) array.get(j);
                int eventid = (int)(long)tmp1.get("eventid");
                int tickets = (int)(long)tmp1.get("tickets");
                eventMap.put(eventid, tickets);
            }
            ticketMap.put(userid, eventMap);
        }
        db.setTicketMap(ticketMap);
        ConcurrentLinkedQueue<String> list = new ConcurrentLinkedQueue();
        list.addAll(membership);
        db.setMembership(list);
        list = new ConcurrentLinkedQueue<>();
        list.addAll(frontend);
        db.setFrontend(list);

        Thread t = new Thread(new HeartBeatHandler(1000));
        t.start();
        db.setThread(t);
        db.setAlive(true);
    }



}