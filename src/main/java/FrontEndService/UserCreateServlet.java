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
 * Servlet used to create user
 */
public class UserCreateServlet extends HttpServlet {

    protected Logger log = Logger.getLogger(UserCreateServlet.class);
    /**
     * get username, send it to user service. print out the response get back from user service.
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        StringBuffer sb = new StringBuffer();
        String line;
        PrintWriter out = response.getWriter();
        BufferedReader in = request.getReader();

        while((line = in.readLine()) != null) {
            sb.append(line);
        }

        JSONParser parser = new JSONParser();

        try {
            String userServer = FrontEndDatabase.getInstance().getPrimaryAddress();

            JSONObject obj = (JSONObject) parser.parse(sb.toString());
            //some code snippets are from https://stackoverflow.com/questions/21404252/post-request-send-json-data-java-httpurlconnection
            URL url = new URL(userServer + "/create");

            HttpURLConnection con = setConnection(url, "POST");

            OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
            wr.write(obj.toString());
            wr.flush();
            int status = con.getResponseCode();

            obj = getJSONFromResponse(con);
            if(obj != null) {
                out.println(obj);
            }
            con.disconnect();
            response.setStatus(status);
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
