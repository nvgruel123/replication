package UserService;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.*;

/**
 * Class for handling elections
 */
public class ElectionHandler {

    protected Logger log = Logger.getLogger(ElectionHandler.class);

    public ElectionHandler() {

    }

    public synchronized int startElection() {
        int status = 200;
        try {
            ExecutorService exec = Executors.newFixedThreadPool(4);
            UserServiceDatabase db = UserServiceDatabase.getInstance();
            ConcurrentLinkedQueue<String> membership = db.getMembership();
            if(db.getAlive() == true) {
                db.getThread().interrupt();
            }
            JSONObject obj = new JSONObject();
            int timestamp = db.getTimeStamp();
            String localAddress = db.getLocalAddress();
            int uid = db.getUID();
            obj.put("timestamp", timestamp);
            obj.put("uid", uid);
            obj.put("server", localAddress);
            log.info(membership);
            for (String server : membership) {

                    if (!server.equals(localAddress)) {
                        log.info("send election request to " + server);
                        URL url = new URL(server + "/election");
                        Future<Integer> responses = exec.submit(new SendRequest(url, obj));
                        int code = responses.get();
                        log.info("response from server " + server + " is " + code);
                        if(code != 200) {
                            status = 400;
                        }
                    }

            }
            exec.shutdown();
            exec.awaitTermination(3, TimeUnit.SECONDS);
        }
        catch(Exception ex) {

        }
        return status;
    }

    public void becomePrimary() {

        try {
            UserServiceDatabase db = UserServiceDatabase.getInstance();
            String localAddress = db.getLocalAddress();
            db.setPrimary(localAddress);
            db.setIsPrimary(true);
            db.removeServer(localAddress);
            ConcurrentLinkedQueue<String> queue = db.getMembership();
            ConcurrentLinkedQueue<String> frontendlist = db.getFrontEnd();
            ExecutorService exec = Executors.newFixedThreadPool(4);
            JSONObject obj = PrepareJSON(db);
            for (String server : queue) {
                URL url = new URL(server + "/update");
                Future<Integer> response = exec.submit(new SendRequest(url, obj));
                response.get();
            }
            for(String server : frontendlist) {
                obj = new JSONObject();
                obj.put("primary", localAddress);
                URL url = new URL(server + "/update");
                Future<Integer> response = exec.submit(new SendRequest(url, obj));
                response.get();
            }
            exec.shutdown();
            exec.awaitTermination(3, TimeUnit.SECONDS);
            log.info("Server now become Primary");
            Thread t = new Thread(new HeartBeatHandler(1000));
            t.start();
            db.setThread(t);
            db.setAlive(true);
        }
        catch(Exception ex) {
            log.debug(ex);
        }
    }

    protected static class SendRequest implements Callable {

        private URL url;
        private JSONObject obj;
        public SendRequest(URL url, JSONObject obj) {
            this.url = url;
            this.obj = obj;
        }
        @Override
        public Object call() throws IOException {
            return sendRequest(url, obj);
        }
        private int sendRequest(URL url, JSONObject obj) {
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(100);
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestMethod("POST");
                connection.connect();
                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                wr.write(obj.toString());
                wr.flush();
                wr.close();
                connection.disconnect();
                return connection.getResponseCode();
            }
            catch (SocketTimeoutException ex) {
                return 400;
            }
            catch (IOException ex) {
                return 400;
            }
        }

    }

    private JSONObject PrepareJSON(UserServiceDatabase db) {
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
        obj.put("membership", array);
        obj.put("userinfo", user);
        obj.put("ticketinfo", ticket);
        obj.put("timestamp", db.getTimeStamp());
        obj.put("primary", db.getPrimary());
        return obj;
    }
}
