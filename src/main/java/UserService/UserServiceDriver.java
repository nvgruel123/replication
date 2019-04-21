package UserService;


import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.json.simple.JSONObject;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

/**
 * Server for user service
 */
public class UserServiceDriver {


    protected static Logger log = Logger.getLogger(UserServiceDriver.class);
    private static int PORT;
    private static String primary;


    public static void main(String args[]) {

        for(int i = 0; i < args.length; i ++) {
            if(args[i].equals("-p")) {
                primary = args[i+1];
            }
            if(args[i].equals("-port")) {
                PORT = Integer.parseInt(args[i+1]);
            }
        }

        Server serv = new Server(PORT);

        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(CreateServlet.class, "/create");
        handler.addServletWithMapping(AbortUserCreateServlet.class, "/create/abort");
        handler.addServletWithMapping(UserServlet.class, "/*");
        handler.addServletWithMapping(RegisterServlet.class, "/register");
        handler.addServletWithMapping(UpdateServlet.class, "/update");
        handler.addServletWithMapping(AbortAddTicketsServlet.class, "/add/abort");
        handler.addServletWithMapping(AbortTransferTicketsServlet.class, "/transfer/abort");
        handler.addServletWithMapping(RemoveServerServlet.class, "/remove");
        handler.addServletWithMapping(HeartBeatServlet.class, "/heartbeat");
        handler.addServletWithMapping(ElectionServlet.class, "/election");
        handler.addServletWithMapping(RegisterFrontEndServlet.class, "/register/frontend");
        serv.setHandler(handler);

        try {
            serv.start();
            UserServiceDatabase db = UserServiceDatabase.getInstance();
            if(primary == null) {
                String host = InetAddress.getLocalHost().getHostAddress();
                primary = "http://" + host + ":" + PORT;
                db.setIsPrimary(true);
            }
            log.info("user server start running on port " + PORT + ".. primary @ " + primary);

            db.setPrimary(primary);
            boolean isPrimary = db.isPrimary();
            log.info("primary: " + isPrimary);
            String local = "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + PORT;
            db.setLocalAddress(local);
            if(isPrimary) {
                Thread t = new Thread(new HeartBeatHandler(1000));
                t.start();
                db.setThread(t);
            }
            else {
                registerServer(primary);
            }

            serv.join();

            log.info("Exiting...");
        }
        catch (Exception ex) {
            log.fatal("Interrupted while running server.", ex);
            System.exit(-1);
        }


    }

    private static boolean registerServer(String uri) {
        try {

            URL url = new URL(uri + "/register");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("POST");
            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            JSONObject obj = new JSONObject();
            String host = InetAddress.getLocalHost().getHostAddress();

            int port = PORT;
            obj.put("host", host);
            obj.put("port", port);
            wr.write(obj.toString());
            wr.flush();
            wr.close();
            int status = connection.getResponseCode();
            if(status == 200) {
                return true;
            }
            else {
                return false;
            }
        }
        catch(Exception ex) {
            log.debug(ex);
        }
        return false;
    }
}
