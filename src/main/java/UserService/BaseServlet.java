package UserService;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * Base servlet include base functions used by servlets
 */
public class BaseServlet extends HttpServlet {
    /**
     * get JSONObject from request
     * @param reader
     * @return
     * @throws ParseException
     * @throws IOException
     */
    protected JSONObject getJSON(BufferedReader reader) throws ParseException, IOException {

        StringBuffer sb = new StringBuffer();
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        JSONParser parser = new JSONParser();

        JSONObject obj = (JSONObject) parser.parse(sb.toString());

        return obj;
    }

    /**
     * Inner class for sending request by thread
     */
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

    /**
     * Callable example from CS601 Instructor's Examples
     */
    protected int SendRequest(URL url, JSONObject obj) throws IOException {
        int status;
        HttpURLConnection connection = setConnection(url, "POST");
        OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
        wr.write(obj.toString());
        wr.flush();
        wr.close();
        status = connection.getResponseCode();
        return status;
    }


    protected HttpURLConnection setConnection(URL url, String method) throws IOException {

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestMethod(method);
        return con;
    }
}
