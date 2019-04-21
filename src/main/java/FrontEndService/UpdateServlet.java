package FrontEndService;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Servlet for updating primary user server
 */
public class UpdateServlet extends HttpServlet {

    protected Logger log = Logger.getLogger(UpdateServlet.class);


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            JSONObject obj = getJSON(request.getReader());
            String primary = (String) obj.get("primary");
            FrontEndDatabase.getInstance().setPrimaryAddress(primary);
            log.info("update primary address to " + primary);

        }
        catch(Exception ex) {
            log.debug(ex);
        }
        response.setStatus(200);
        response.getWriter().close();
    }

    private JSONObject getJSON(BufferedReader reader) throws ParseException, IOException {

        StringBuffer sb = new StringBuffer();
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        JSONParser parser = new JSONParser();

        JSONObject obj = (JSONObject) parser.parse(sb.toString());

        return obj;
    }

}