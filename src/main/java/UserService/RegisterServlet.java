package UserService;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.*;

/**
 * Servlet for register new secondary to primary
 */
public class RegisterServlet extends BaseServlet {


    protected Logger log = Logger.getLogger(RegisterServlet.class);


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter out = response.getWriter();
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

                    db.commitAddServer(uri, time);
                    time = db.getTimeStamp();
                    obj.put("timestamp", time);
                    for (String server : membership) {

                        URL url = new URL(server + "/register");
                        SendRequest(url, obj);

                    }

                    //update new server's map
                    obj = PrepareJSON(db, time);
                    URL url = new URL(uri + "/update");
                    SendRequest(url, obj);
                    response.setStatus(200);
                }
            }
            else {
                JSONObject obj = getJSON(request.getReader());
                String host = (String) obj.get("host");
                int port = (int)(long) obj.get("port");
                time = (int)(long) obj.get("timestamp");
                String uri = "http://" + host + ":" + port;
                db.commitAddServer(uri, time);
                response.setStatus(200);
            }
        }
        catch(Exception ex) {
            log.debug(ex);
            response.setStatus(400);
        }
        out.flush();
        out.close();
    }




    private JSONObject PrepareJSON(UserServiceDatabase db, int timestamp) {
        JSONObject obj;
        ConcurrentHashMap<Integer, String> userMap = db.getUserMap();
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> ticketMap = db.getTicketMap();
        obj = new JSONObject();
        JSONArray user = new JSONArray();
        JSONArray ticket = new JSONArray();
        for(int userid : userMap.keySet()) {
            JSONObject tmp = new JSONObject();
            tmp.put("userid", userid);
            String username = userMap.get(userid);
            tmp.put("username", username);
            user.add(tmp);
        }
        for(int userid : ticketMap.keySet()){
            JSONObject tmp = new JSONObject();
            ConcurrentHashMap<Integer, Integer> map = ticketMap.get(userid);
            tmp.put("userid", userid);
            JSONArray array = new JSONArray();
            for(int eventid : map.keySet()) {
                JSONObject tmp1 = new JSONObject();
                tmp1.put("eventid", eventid);
                int tickets = map.get(eventid);
                tmp1.put("tickets", tickets);
                array.add(tmp1);
            }
            tmp.put("eventmap", array);
            ticket.add(tmp);
        }

        ConcurrentLinkedQueue membership = db.getMembership();
        JSONArray array = new JSONArray();
        array.addAll(membership);
        ConcurrentLinkedQueue frontend = db.getFrontEnd();
        JSONArray array1 = new JSONArray();
        array1.addAll(frontend);
        obj.put("frontend", array1);
        obj.put("membership", array);
        obj.put("userinfo", user);
        obj.put("ticketinfo", ticket);
        obj.put("timestamp", timestamp);
        obj.put("primary", db.getPrimary());
        return obj;
    }






}
