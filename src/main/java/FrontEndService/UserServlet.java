package FrontEndService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javafx.util.Pair;


import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.HashMap;

/**
 * UserServlet for frontend service
 */
public class UserServlet extends HttpServlet {

    protected Logger log = Logger.getLogger(UserServlet.class);
    private String eventServer = "http://mc02.cs.usfca.edu:2450";


    /**
     * Handle get request to display user info in JSON
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter out = response.getWriter();

        JSONObject obj = new JSONObject();
        JSONObject userObj;
        JSONObject eventObj;
        int status;

        try {
            int userid = Integer.parseInt(request.getPathInfo().replace("/", ""));
            obj.put("userid", userid);
            HashMap<Integer, JSONObject> map = new HashMap<>();
            Pair<Integer, JSONObject> pair = getUserJSON(userid);
            userObj = pair.getValue();
            status = pair.getKey();
            String username = (String) userObj.get("username");
            obj.put("username", username);
            JSONArray tickets1 = new JSONArray();
            JSONArray tickets = (JSONArray) userObj.get("tickets");
            for(int i = 0; i < tickets.size(); i++) {
                JSONObject arrayObj = (JSONObject) tickets.get(i);
                int eventid = (int) (long) arrayObj.get("eventid");
                if(map.containsKey(eventid)) {
                    eventObj = map.get(eventid);
                }
                else {
                    pair = getEventJSON(eventid);
                    status = pair.getKey();
                    eventObj = pair.getValue();
                    if(status != 200) {
                        break;
                    }
                    map.put(eventid, eventObj);
                }
                tickets1.add(eventObj);
            }
            if(status == 200) {
                obj.put("tickets", tickets1);
                response.setContentType("application/json");
                response.setStatus(status);
                out.println(obj);
            }
            else {
                response.setStatus(status);
            }

        }
        catch(NumberFormatException ex) {
            log.debug(ex);
            response.setStatus(400);
            response.setContentType("application/json");

        }
        catch(ParseException ex) {
            log.debug(ex);
            response.setStatus(400);

        }
        catch(NullPointerException ex) {
            log.debug(ex);
            response.setStatus(400);
        }
        out.flush();
        out.close();
    }

    /**
     * Post request for transfer tickets between users, sent request to user service and get response back.
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("transfer tickets");
        String[] path = request.getRequestURI().split("/");
        JSONObject obj = new JSONObject();
        PrintWriter out = response.getWriter();
        StringBuffer sb = new StringBuffer();
        String line;
        int status = 400;
        BufferedReader in = request.getReader();

        try {
            int userid = Integer.parseInt(path[2]);

            while((line = in.readLine()) != null) {
                sb.append(line);

            }
            JSONParser parser = new JSONParser();
            obj = (JSONObject) parser.parse(sb.toString());
            String userServer = FrontEndDatabase.getInstance().getPrimaryAddress();

            //some code snippets are from https://stackoverflow.com/questions/21404252/post-request-send-json-data-java-httpurlconnection
            URL url = new URL(userServer + "/" + userid + "/tickets/transfer");
            HttpURLConnection con = setConnection(url, "POST");
            OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
            wr.write(obj.toString());
            wr.flush();
            status = con.getResponseCode();
            obj = getJSONFromResponse(con);

            if(obj != null) {
                out.println(obj);
            }
            response.setStatus(status);
            response.setContentType("application/json");

        }
        catch(NumberFormatException ex) {
            log.debug(ex);
            response.setStatus(status);
            response.setContentType("application/json");


        }
        catch (ParseException ex) {
            response.setStatus(status);
            response.setContentType("application/json");

        }
        out.flush();
        out.close();
    }


    /**
     * Get results from user server.
     * @param userid
     * @return  Pair of response code and JSONObject from user server.
     * @throws ParseException
     */
    private Pair<Integer, JSONObject> getUserJSON(int userid) throws ParseException {

        int status = 400;

        //some code snippets are from https://stackoverflow.com/questions/21404252/post-request-send-json-data-java-httpurlconnection
        try {
            String userServer = FrontEndDatabase.getInstance().getPrimaryAddress();
            URL url = new URL(userServer + "/" + userid);
            HttpURLConnection con = setConnection(url, "GET");
            status = con.getResponseCode();
            JSONObject obj = getJSONFromResponse(con);
            return new Pair<>(status, obj);
        }
        catch(Exception ex) {
            log.debug(ex);
        }

        return new Pair<Integer, JSONObject>(status ,null);
    }

    /**
     * get results from event server
     * @param eventid
     * @return  pair of response code and JSONObject from event server
     * @throws ParseException
     */
    private Pair<Integer, JSONObject> getEventJSON(int eventid) throws ParseException {
        int status = 400;
        //some code snippets are from https://stackoverflow.com/questions/21404252/post-request-send-json-data-java-httpurlconnection
        try {
            URL url = new URL(eventServer + "/" + eventid);
            HttpURLConnection con = setConnection(url, "GET");
            status = con.getResponseCode();
            JSONObject obj = getJSONFromResponse(con);
            con.disconnect();
            return new Pair<>(status, obj);
        }
        catch(Exception ex) {
            log.debug(ex);
        }

        return new Pair<Integer, JSONObject>(status ,null);
    }

    /**
     * Set up connection
     * @param url
     * @param method
     * @return  Connection of URL
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
     * get JSONObject from response of connection.
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
