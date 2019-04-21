package FrontEndService;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The servlet for create events.
 */
//todo important: stress test
public class EventCreateServlet extends HttpServlet {

    private String eventServer = "http://mc02.cs.usfca.edu:2450";

    protected Logger log = Logger.getLogger(EventCreateServlet.class);


    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        PrintWriter out = null;

        try {
            out = response.getWriter();
            StringBuffer sb = new StringBuffer();
            String line;
            BufferedReader in = request.getReader();
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse(sb.toString());
            Integer userId = (int)(long) obj.get("userid");
            log.info(obj);
            if (!isUserExist(userId)) {
                response.setStatus(400);
                log.info("user not exist");
            }
            else {
                log.info("send request to event server");
                //redirect
                URL redirectUrl = new URL(eventServer + "/create");

                HttpURLConnection con = setConnection(redirectUrl, "POST");
                OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
                wr.write(obj.toString());
                wr.flush();

                // handle input
                int status = con.getResponseCode();
                response.setStatus(status);
                response.setContentType("application/json");
                obj = getJSONFromResponse(con);
                log.info("response from event server " + obj);
                out.println(obj);

            }

        }
        catch(Exception ex) {
            log.debug(ex);
            response.setStatus(400);
        }
        out.flush();
    }

    /**
     * check if user exists.
     * @param userId
     * @return  true if user exists
     * @throws IOException
     */
    private boolean isUserExist(int userId) throws IOException {
        String userServer = FrontEndDatabase.getInstance().getPrimaryAddress();

        URL redirectUrl = new URL(userServer + "/" + userId);

        HttpURLConnection con = (HttpURLConnection) redirectUrl.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestMethod("GET");

        int status = con.getResponseCode();
        if (status == 200)
            return true;
        else
            return false;
    }

    /**
     * set up connection
     * @param url
     * @param method
     * @return  connection to URL
     * @throws IOException
     */
    private HttpURLConnection setConnection(URL url, String method) throws IOException {

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestMethod(method);
        return con;
    }

    /**
     * get JSON from post request
     * @param connection
     * @return  JSONObject
     * @throws IOException
     * @throws ParseException
     */
    private JSONObject getJSONFromResponse(HttpURLConnection connection) throws IOException, ParseException {
        StringBuffer sb = new StringBuffer();
        String line;
        BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject) parser.parse(sb.toString());

        return obj;
    }
}