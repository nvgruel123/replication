package UserService;



import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.*;

/**
 * Servlet for create user
 */
public class CreateServlet extends BaseServlet {

    protected Logger log = Logger.getLogger(CreateServlet.class);

    /**
     * Check if username exist in database, if not, add this user.
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();

        try {
            JSONObject obj = getJSON(request.getReader());
            UserServiceDatabase db = UserServiceDatabase.getInstance();
            boolean primary = db.isPrimary();
            String username = (String) obj.get("username");
            int time = 0;
            if(primary) {
                synchronized (this) {
                    int userid = db.commitAddUser(username, time);
                    if(userid != -1) {
                        time = db.getTimeStamp();
                        obj.put("timestamp", time);
                        if(UpdateSecondary(db, obj, userid)) {
                            response.setStatus(200);
                            response.setContentType("application/json");
                            obj = new JSONObject();
                            obj.put("userid", userid);
                            out.println(obj.toString());
                        }
                        else {
                            response.setStatus(400);
                        }
                    }


                }

            }
            else {
                time = (int)(long) obj.get("timestamp");
                if(db.commitAddUser(username, time)!= -1) {
                    response.setStatus(200);

                }
                else {
                    response.setStatus(400);
                }
            }
        }
        catch(Exception ex) {
            log.debug(ex);
            response.setStatus(400);
        }
        out.flush();
        out.close();
    }

    /**
     * send updates to secondaries.
     * @param db
     * @param obj
     * @param userid
     * @return true if all secondaries return 200
     * @throws MalformedURLException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private synchronized boolean UpdateSecondary(UserServiceDatabase db, JSONObject obj, int userid) throws MalformedURLException,
            ExecutionException, InterruptedException {


        boolean status = true;
        ConcurrentLinkedQueue<String> membership = db.getMembership();
        ExecutorService exec = Executors.newFixedThreadPool(4);
        for(String server : membership) {

            URL url = new URL(server + "/create");
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

            db.abortAddUser(userid, time);
            obj = new JSONObject();

            obj.put("timestamp", time);
            obj.put("userid", userid);
            log.info(obj);
            ExecutorService exec1 = Executors.newFixedThreadPool(4);
            for(String server : membership) {
                URL url = new URL(server + "/create/abort");
                Future<Integer> response = exec1.submit(new SendRequest(url, obj));
                response.get();

            }
            exec1.shutdown();
            exec1.awaitTermination(1, TimeUnit.SECONDS);
        }

        return status;

    }






}
