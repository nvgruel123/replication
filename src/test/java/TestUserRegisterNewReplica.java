import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class TestUserRegisterNewReplica {

    public static void main(String args[]) {
        try {
            String FrontEndServer = "http://localhost:2400";
            int index = 10;
            int eventId = 0;
            ConcurrentTest(FrontEndServer, index, eventId);

            //NonConcurrentTest(FrontEndServer, index);
        }
        catch(Exception ex) {
            System.out.println(ex);
        }



    }

    private static void NonConcurrentTest(String userServer, int index) throws IOException {
        for(int i = 0; i < index; i ++) {
            URL url = new URL(userServer + "/create");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("POST");
            JSONObject obj = new JSONObject();
            obj.put("username", "testuser" + i);

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            System.out.println(obj.toString());
            wr.write(obj.toString());
            wr.flush();

            wr.close();
            System.out.println(connection.getResponseCode());
        }
        for(int i = 0; i < index; i ++) {
            URL url = new URL(userServer + "/" + i + "/tickets/add");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("POST");
            JSONObject obj = new JSONObject();
            obj.put("eventid", i);
            obj.put("tickets", i + 10);
            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(obj.toString());
            wr.flush();
            wr.close();
            System.out.println(connection.getResponseCode());
        }
        for(int i = 0; i < index; i ++) {
            URL url = new URL(userServer + "/" + i + "/tickets/transfer");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("POST");
            JSONObject obj = new JSONObject();
            obj.put("eventid", i);
            obj.put("tickets", 1);
            obj.put("targetuser", 0);
            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(obj.toString());
            wr.flush();
            wr.close();
            System.out.println(connection.getResponseCode());

        }
    }

    private static void ConcurrentTest(String userServer, int index, int eventId) throws InterruptedException {
        ExecutorService exec = Executors.newFixedThreadPool(10);
        for(int i = 0; i < index; i ++) {

           exec.submit(new UserAdder(i, userServer));
        }
        for(int i = 0; i < index; i ++) {
          //exec.submit(new TicketAdder(i, userServer, eventId));
        }

        for(int i = 0; i < index; i ++) {
            //exec.submit(new TicketTransfer(i, userServer, eventId));
        }
        exec.shutdown();
        exec.awaitTermination(10, TimeUnit.SECONDS);
    }

    public static class UserAdder implements Runnable {

        int i;
        String Server;

        public UserAdder(int i, String user) {
            this.i = i;
            Server = user;
        }
        @Override
        public void run() {
            try {
                URL url = new URL(Server + "/users/create");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestMethod("POST");
                JSONObject obj = new JSONObject();
                obj.put("username", "concurrent testuser" + i);

                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());

                wr.write(obj.toString());
                wr.flush();

                wr.close();
                System.out.println("add user concurrent testuser" + i + "\n" + connection.getResponseCode());

            }
            catch(Exception ex) {

            }
        }

    }

    public static class TicketAdder implements Runnable {

        int i;
        String Server;
        int eventId;

        public TicketAdder(int i, String user, int eventId) {
            this.i = i;
            Server = user;
            this.eventId = eventId;
        }
        @Override
        public void run() {
            try {

                URL url = new URL(Server + "/" + i + "/tickets/add");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestMethod("POST");
                JSONObject obj = new JSONObject();
                obj.put("eventid", eventId);
                obj.put("tickets", 5);
                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                wr.write(obj.toString());
                wr.flush();
                wr.close();
                System.out.println("add 5 tickets for userid = " + i + "\n" + connection.getResponseCode());

            }
            catch(Exception ex) {

            }
        }



    }
    public static class TicketTransfer implements Runnable {

        int i;
        String userServer;
        int eventId;

        public TicketTransfer(int i, String user, int eventId) {
            this.i = i;
            userServer = user;
            this.eventId = eventId;
        }

        @Override
        public void run() {
            try {

                URL url = new URL(userServer + "/" + 10 + "/tickets/transfer");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestMethod("POST");
                JSONObject obj = new JSONObject();
                obj.put("eventid", eventId);
                obj.put("tickets", 1);
                obj.put("targetuser", 0);
                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                wr.write(obj.toString());
                wr.flush();
                wr.close();
                System.out.println("transfer tickets\n" + connection.getResponseCode());

            } catch (Exception ex) {

            }
        }
    }

}
