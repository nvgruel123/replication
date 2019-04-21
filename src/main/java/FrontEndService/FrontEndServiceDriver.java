package FrontEndService;

import UserService.HeartBeatHandler;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.json.simple.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

/**
 * Server for frontend service
 */
public class FrontEndServiceDriver {

    protected static Logger log = Logger.getLogger(FrontEndServiceDriver.class);
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
        handler.addServletWithMapping(UserCreateServlet.class, "/users/create");
        handler.addServletWithMapping(UserServlet.class, "/users/*");
        handler.addServletWithMapping(EventListServlet.class, "/events");
        handler.addServletWithMapping(EventCreateServlet.class, "/events/create");
        handler.addServletWithMapping(EventDetailAndPurchaseServlet.class, "/events/*");
        handler.addServletWithMapping(UpdateServlet.class, "/update");
        handler.addServletWithMapping(HeartBeatServlet.class, "/heartbeat");
        serv.setHandler(handler);


        try {
            serv.start();

            registerServer(primary);
            FrontEndDatabase db = FrontEndDatabase.getInstance();
            db.setPrimaryAddress(primary);
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

            URL url = new URL(uri + "/register/frontend");

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