package UserService;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.*;

/**
 * class for handling heart beat
 */
public class HeartBeatHandler implements Runnable {

    private int ms = 50;


    protected Logger log = Logger.getLogger(HeartBeatHandler.class);


    public HeartBeatHandler(int ms) {
        this.ms = ms;

    }

    @Override
    public void run() {


        ExecutorService exec = Executors.newFixedThreadPool(4);
        try {
            UserServiceDatabase db = UserServiceDatabase.getInstance();

            boolean isPrimary = db.isPrimary();
            while(db.getAlive()) {
                if(!isPrimary) {
                    String primary = db.getPrimary();
                    Future<Integer> response = exec.submit(new Worker(primary));
                    int status = response.get();
                    if(status != 200) {
                        // start election
                        log.info("detecting primary failed");
                        synchronized (this) {
                            db.setAlive(false);
                            ElectionHandler handler = new ElectionHandler();
                            int code = handler.startElection();
                            if(code == 200) {
                                handler.becomePrimary();
                            }
                        }
                    }
                }
                else {
                    detectFailure(exec, db);
                }
                Thread.sleep(ms);
            }
        }
        catch(Exception ex) {
            log.debug(ex);
        }
    }

    private void detectFailure(ExecutorService exec, UserServiceDatabase db) throws InterruptedException, ExecutionException {
        ConcurrentLinkedQueue<String> membership = db.getMembership();
        ConcurrentLinkedQueue<String> frontendlist = db.getFrontEnd();
        for(String server : membership) {

            synchronized (this) {
                Future<Integer> response = exec.submit(new Worker(server));
                int status = response.get();
                if (status != 200) {
                    removeServer(server, db);
                }
            }

        }
        for(String server : frontendlist) {
            synchronized (this) {
                Future<Integer> response = exec.submit(new Worker(server));
                int status = response.get();
                if (status != 200) {
                    removeServer(server, db);
                }
            }
        }
    }

    private synchronized void removeServer(String server, UserServiceDatabase db) {
        try {

            log.info("detected failed server @ " + server + ", remove from list");
            int time = db.removeServer(server);
            JSONObject obj = new JSONObject();
            obj.put("timestamp", time);
            obj.put("server", server);
            ConcurrentLinkedQueue<String> membership = db.getMembershipList();
            ExecutorService exec = Executors.newFixedThreadPool(4);
            for (String uri : membership) {
                URL url = new URL(uri + "/remove");
                exec.submit(new SendRequest(url, obj)).get();
            }
            exec.shutdown();
            exec.awaitTermination(10, TimeUnit.SECONDS);
        }
        catch(Exception ex) {
            log.debug(ex);
        }

    }

    private class Worker implements Callable {

        private String uri;
        public Worker(String uri) {
            this.uri = uri;
        }
        @Override
        public Object call() {
            int status = 400;
            try {
                URL url = new URL(uri + "/heartbeat");
                return getResponse(url);
            }
            catch(Exception ex) {

            }
            return status;
        }

        protected int getResponse(URL url) throws IOException {

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestMethod("GET");
            con.connect();
            con.disconnect();
            return con.getResponseCode();

        }

    }

    private static class SendRequest implements Callable {

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
}
