package UserService;

import org.apache.log4j.Logger;

import org.json.simple.JSONObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.concurrent.*;

/**
 * UserServlet that handle request
 * display user info
 * add tickets to user
 * transfer tickets between users.
 */
public class UserServlet extends BaseServlet {

    protected Logger log = Logger.getLogger(UserServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter out = response.getWriter();
        String path = request.getPathInfo();
        path = path.replace("/", "");
        try {
            int userid = Integer.parseInt(path);
            UserServiceDatabase db = UserServiceDatabase.getInstance();
            String username = db.getUsername(userid);
            JSONObject obj = db.getTicketsInfo(userid);
            if(username != null & obj != null) {
                response.setStatus(200);
                response.setContentType("application/json");
                out.println(obj);
            }

            else {
                response.setStatus(400);
            }
        }
        catch (NumberFormatException ex) {
            log.debug(ex);
            response.setStatus(400);

        }
        catch(NullPointerException ex) {
            log.debug(ex);
            response.setStatus(500);

        }
        out.flush();
        out.close();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        String path = request.getPathInfo();
        if (path.endsWith("add")) {
            doAdd(request, response, path);

        }
        else if (path.endsWith("transfer")) {
            doTransfer(request, response, path);
        }
        else {
            response.setStatus(400);
        }
        out.flush();
        out.close();
    }

    private void doAdd(HttpServletRequest request, HttpServletResponse response, String path) {

        String[] paths = path.split("/");

        try {
            int userid = Integer.parseInt(paths[1]);
            UserServiceDatabase db = UserServiceDatabase.getInstance();
            JSONObject obj = getJSON(request.getReader());
            int eventid = (int) (long) obj.get("eventid");
            int tickets = (int) (long) obj.get("tickets");
            int time = 0;
            boolean primary = db.isPrimary();
            if (primary) {
                synchronized (this) {
                    if(db.commitAddTickets(userid, eventid, tickets, time)) {
                        time = db.getTimeStamp();
                        obj.put("timestamp", time);
                        obj.put("userid", userid);
                        if (UpdateSecondaryForAdd(db, obj, userid)) {
                            response.setStatus(200);
                        } else {
                            response.setStatus(400);
                        }
                    }
                    else {
                        response.setStatus(400);
                    }
                }
            }
            else {

                time = (int)(long) obj.get("timestamp");
                db.commitAddTickets(userid, eventid, tickets, time);
                response.setStatus(200);
            }

        }
        catch(Exception ex) {
            log.debug(ex);
        }
    }

    private void doTransfer(HttpServletRequest request, HttpServletResponse response, String path) {
        String[] paths = path.split("/");

        try {
            int userid = Integer.parseInt(paths[1]);

            UserServiceDatabase db = UserServiceDatabase.getInstance();
            boolean primary = db.isPrimary();
            JSONObject obj = getJSON(request.getReader());
            int eventid = (int)(long) obj.get("eventid");
            int tickets = (int)(long) obj.get("tickets");
            int targetuser = (int)(long) obj.get("targetuser");
            int time = 0;

            if(primary) {
                synchronized (this) {
                    if(db.commitTransferTickets(userid, eventid, tickets, targetuser, time)) {
                        time = db.getTimeStamp();
                        obj.put("timestamp", time);
                        obj.put("userid", userid);

                        if (UpdateSecondaryForTransfer(db, obj, userid)) {
                            response.setStatus(200);
                        }
                        else {
                            response.setStatus(400);
                        }
                    }
                    else {
                        response.setStatus(400);
                    }
                }
            }
            else {
                time = (int)(long) obj.get("timestamp");
                db.commitTransferTickets(userid, eventid, tickets, targetuser, time);
                response.setStatus(200);
            }
        }
        catch (Exception ex) {
            log.debug(ex);
            response.setStatus(400);
        }
    }


    private synchronized boolean UpdateSecondaryForAdd(UserServiceDatabase db, JSONObject obj, int userid) throws MalformedURLException,
            ExecutionException, InterruptedException {

        String primary = db.getPrimary();
        boolean status = true;
        ConcurrentLinkedQueue<String> membership = db.getMembership();
        ExecutorService exec = Executors.newFixedThreadPool(4);
        for(String server : membership) {
            if(!primary.equals(server)) {
                URL url = new URL(server + "/" + userid + "/tickets/add");
                Future<Integer> response = exec.submit(new SendRequest(url, obj));
                if(response.get() != 200) {
                    status = false;
                }
            }

        }
        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.SECONDS);
        if(status == false) {
            log.info("error occurs, send aborting signal to secondaries");
            int time = db.getTimeStamp();
            int eventid = (int) (long) obj.get("eventid");
            int tickets = (int) (long) obj.get("tickets");

            db.abortAddTickets(userid, eventid, tickets, time);
            obj = new JSONObject();

            obj.put("timestamp", time);
            obj.put("userid", userid);
            obj.put("eventid", eventid);
            obj.put("tickets", tickets);

            ExecutorService exec1 = Executors.newFixedThreadPool(4);
            for(String server : membership) {
                if(!primary.equals(server)) {
                    URL url = new URL(server + "/add/abort");
                    Future<Integer> response = exec1.submit(new SendRequest(url, obj));
                    response.get();
                }

            }
            exec1.shutdown();
            exec1.awaitTermination(1, TimeUnit.SECONDS);
        }
        return status;
    }

    private synchronized boolean UpdateSecondaryForTransfer(UserServiceDatabase db, JSONObject obj, int userid) throws MalformedURLException,
            ExecutionException, InterruptedException {

        String primary = db.getPrimary();
        boolean status = true;
        ConcurrentLinkedQueue<String> membership = db.getMembership();
        ExecutorService exec = Executors.newFixedThreadPool(4);
        for(String server : membership) {
            URL url = new URL(server + "/" + userid + "/tickets/transfer");
            Future<Integer> response = exec.submit(new SendRequest(url, obj));
            if(response.get() != 200) {
                status = false;
            }

        }
        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.SECONDS);
        if(status == false) {
            log.info("error occurs, send aborting signal to secondaries");
            int time = db.getTimeStamp();
            int eventid = (int) (long) obj.get("eventid");
            int tickets = (int) (long) obj.get("tickets");
            int target = (int)(long) obj.get("targetuser");

            db.abortTransferTickets(userid, eventid, tickets, target, time);
            obj = new JSONObject();

            obj.put("timestamp", time);
            obj.put("userid", userid);
            obj.put("eventid", eventid);
            obj.put("tickets", tickets);
            obj.put("targetuser", target);

            ExecutorService exec1 = Executors.newFixedThreadPool(4);
            for(String server : membership) {

                URL url = new URL(server + "/transfer/abort");
                Future<Integer> response = exec1.submit(new SendRequest(url, obj));
                response.get();

            }
            exec1.shutdown();
            exec1.awaitTermination(1, TimeUnit.SECONDS);
        }
        return status;
    }

}