package FrontEndService;



import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Servlet used for display event list
 */
public class EventListServlet extends HttpServlet {

    private String eventServer = "http://mc02.cs.usfca.edu:2450";

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        //redirect
        URL url = new URL(eventServer + "/list");
        HttpURLConnection connection = setConnection(url, "GET");
        //handle input
        int status = connection.getResponseCode();
        response.setStatus(status);
        StringBuffer sb = new StringBuffer();
        String line;
        BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        while((line = br.readLine()) != null) {
            sb.append(line);
        }
        if (!sb.toString().isEmpty()) {
            out.println(sb.toString());
        }
        response.setContentType("application/json");
        out.flush();
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
}