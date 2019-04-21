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
 * Servlet used to buy tickets for user, and get event detail
 */
public class EventDetailAndPurchaseServlet extends HttpServlet {

    protected Logger log = Logger.getLogger(EventDetailAndPurchaseServlet.class);
    private String eventServer = "http://mc02.cs.usfca.edu:2450";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            String url = request.getRequestURI();
            String eventId = url.replaceAll("events/", "").replaceAll("/", "");
            log.info("eventid = " + eventId);
            PrintWriter out = response.getWriter();
            URL redirectUrl = new URL(eventServer + "/" + eventId);

            HttpURLConnection con = setConnection(redirectUrl, "GET");
            int status = con.getResponseCode();
            log.info("status = " + status);
            JSONObject obj = getJSONFromResponse(con);
            response.setStatus(status);
            response.setContentType("application/json");
            out.println(obj);
            out.flush();
        }
        catch(Exception ex) {
            log.debug(ex);
            response.setStatus(400);
        }

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        String url = request.getRequestURI();
        int userId = Integer.parseInt(url.replaceAll("/events/[0-9]*/purchase/", ""));
        int eventId = Integer.parseInt(url.replaceAll("/events/", "")
                .replaceAll("/purchase/[0-9]*", ""));
        JSONObject requestBody = readRequestBody(request);
        PrintWriter out = null;
        try {
            out = response.getWriter();
            URL requestUrl = new URL(eventServer + "/purchase/" + eventId);
            HttpURLConnection connection = setConnection(requestUrl, "POST");

            requestBody.put("userid", userId);
            requestBody.put("eventid", eventId);
            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(requestBody.toString());
            wr.flush();
            int status = connection.getResponseCode();
            response.setStatus(status);
        }
        catch(Exception ex) {
            response.setStatus(400);
            log.debug(ex);

        }
        out.flush();
    }

    /****************************************** Below are helper methods  *********************************************/



    private JSONObject readRequestBody(HttpServletRequest request) {
        StringBuffer requestBody = new StringBuffer();
        String line;
        try {
            BufferedReader in = request.getReader();

            while ((line = in.readLine()) != null) {
                requestBody.append(line);
            }

            JSONParser parser = new JSONParser();
            JSONObject JsonObject = (JSONObject) parser.parse(requestBody.toString());

            return JsonObject;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private HttpURLConnection setConnection(URL url, String method) throws IOException {

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestMethod(method);
        return con;
    }

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