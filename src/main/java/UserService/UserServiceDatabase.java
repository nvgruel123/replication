package UserService;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.omg.PortableInterceptor.INACTIVE;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * database class use to store user info and tickets hold by user.
 */
public class UserServiceDatabase {

    protected final static Logger log = LogManager.getLogger(UserServiceDatabase.class);
    ConcurrentHashMap<Integer,String> userMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> ticketMap = new ConcurrentHashMap<>();
    private static UserServiceDatabase singleton = new UserServiceDatabase();
    private static String primary;
    private static ConcurrentLinkedQueue<String> membership = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<String> frontend = new ConcurrentLinkedQueue<>();
    private static int timeStamp = 0;
    private static boolean isPrimary = false;
    private static String localAddress;
    private static Thread t;
    private static UUID uid;
    private volatile boolean alive = true;

    private UserServiceDatabase () {
        this.uid = UUID.randomUUID();


    }

    public static UserServiceDatabase getInstance() {
        return singleton;
    }

    /**
     * add user to database.
     * @param username
     * @return -1 if user exists, otherwise return user id.
     */
    private synchronized int addUser(String username) {

        if(!userMap.containsValue(username)) {
            Integer size = userMap.size();
            userMap.put(size, username);
            ticketMap.put(size, new ConcurrentHashMap<Integer, Integer>());
            log.info("User " + username + " added, userId = " + size);
            timeStamp ++;
            return size;
        }
        else {
            log.info("user exists.");
            return -1;
        }


    }

    private synchronized int addUser(String username, int time) {
        try {
            while (time != (timeStamp + 1)) {
                wait();
                notifyAll();
            }
            return addUser(username);

        }
        catch(Exception ex) {
            log.debug(ex);
        }
        return -1;
    }

    public synchronized int commitAddUser(String username, int time) {
        int userid;
        if(isPrimary()) {
           userid = addUser(username);
        }
        else {
            userid = addUser(username, time);
            notifyAll();
        }
        return userid;
    }

    private synchronized void removeUser(int userId) {
        userMap.remove(userId);
        ticketMap.remove(userId);
        timeStamp --;
        log.info("user removed");
    }

    private synchronized void removeUser(int userId, int time) {

        try {
            while (time != timeStamp) {
                wait();
                notifyAll();
            }
            removeUser(userId);

        }
        catch(Exception ex) {
            log.debug(ex);
        }
    }

    public synchronized void abortAddUser(int userId, int time) {
        if(isPrimary()) {
            removeUser(userId);
        }
        else {
            removeUser(userId, time);
        }
    }


    /**
     * add tickets to user
     * @param userId
     * @param eventId
     * @param tickets
     * @return return false if user can't be found, otherwise add tickets to user
     */
    private boolean addTickets(int userId, int eventId, int tickets) {
        synchronized (this) {
            if(ticketMap.containsKey(userId)) {
                ConcurrentHashMap<Integer, Integer> map = ticketMap.get(userId);
                int ticket;
                if (map.containsKey(eventId)) {
                    ticket = map.get(eventId) + tickets;
                } else {
                    ticket = 0 + tickets;
                }
                map.put(eventId, ticket);
                log.info("user " + getUsername(userId) + " now has " + map.get(eventId) + " tickets");
                timeStamp++;
                return true;
            }
            else {
                log.info("user not exist");
                return false;
            }

        }
    }

    private synchronized boolean addTickets(int userId, int eventId, int tickets, int time) {
        try {
            while(time != (timeStamp + 1)) {
                wait();
                notifyAll();
            }
            return addTickets(userId, eventId, tickets);


        }
        catch(Exception ex) {
            log.debug(ex);
        }
        return false;
    }

    public synchronized boolean commitAddTickets(int userId, int eventId, int tickets, int time) {
        boolean status;
        if(isPrimary()){
            status = addTickets(userId, eventId, tickets);
        }
        else {
            status = addTickets(userId, eventId, tickets, time);
            notifyAll();
        }
        return status;
    }

    private synchronized void removeAddTickets(int userId, int eventId, int tickets) {


        ConcurrentHashMap<Integer, Integer> map = ticketMap.get(userId);
        int ticket = map.get(eventId);
        map.put(eventId, (ticket - tickets));
        timeStamp --;

    }

    private synchronized void removeAddTickets(int userId, int eventId, int tickets, int time) {
        try {
            while (time != timeStamp) {
                wait();
                notifyAll();
            }
            removeAddTickets(userId, eventId, tickets);

        }
        catch(Exception ex) {
            log.debug(ex);
        }

    }

    public synchronized void abortAddTickets(int userId, int eventId, int tickets, int time) {

        if(isPrimary()) {
            removeAddTickets(userId, eventId, tickets);
        }
        else {
            removeAddTickets(userId, eventId, tickets, time);
        }

    }

    private synchronized boolean transferTickets(int userId, int eventId, int tickets, int TargetUser) {


        if(userId == TargetUser) {
            return false;
        }
        if(ticketMap.containsKey(userId) && ticketMap.containsKey(TargetUser)) {
            ConcurrentHashMap<Integer,Integer> map = ticketMap.get(userId);
            ConcurrentHashMap<Integer, Integer> targetmap = ticketMap.get(TargetUser);
            if(map.containsKey(eventId)) {
                int ticketsHold =  map.get(eventId);
                if(ticketsHold >= tickets) {
                    int targetTicket = 0;
                    if(targetmap.containsKey(eventId)) {
                        targetTicket = targetmap.get(eventId);
                    }
                    targetmap.put(eventId, (targetTicket + tickets));
                    map.put(eventId, (ticketsHold - tickets));
                    timeStamp ++;
                    log.info("ticket transferred");
                    return true;

                }
                else {
                    log.info("Not enough tickets to transfer.");
                }

            }
            else {
                log.info("no tickets found for this event.");
            }

        }
        else {
            log.info("Either user or target user not exist.");
        }
        return false;
    }

    private synchronized boolean transferTickets(int userId, int eventId, int tickets, int TargetUser, int time) {
        try {
            while(time != (timeStamp + 1)) {
                wait();
                notifyAll();
            }

            return transferTickets(userId, eventId, tickets, TargetUser);


        }
        catch(Exception ex) {
            log.debug(ex);
        }
        return false;
    }

    public synchronized boolean commitTransferTickets(int userId, int eventId, int tickets, int TargetUser, int time) {

        boolean status;

        if(isPrimary()) {
            status = transferTickets(userId, eventId, tickets, TargetUser);
        }
        else {
            status = transferTickets(userId, eventId, tickets, TargetUser, time);
            notifyAll();
        }
        return status;

    }

    private synchronized void removeTransferTickets(int userId, int eventId, int tickets, int TargetUser) {
        ConcurrentHashMap<Integer, Integer> usermap = ticketMap.get(userId);
        ConcurrentHashMap<Integer, Integer> targetmap = ticketMap.get(TargetUser);

        int userTicket = usermap.get(eventId) + tickets;
        int targetTicket = targetmap.get(eventId) - tickets;
        usermap.put(eventId, userTicket);
        targetmap.put(eventId, targetTicket);
        timeStamp --;
        log.info("transferred tickets returned");
    }

    private synchronized void removeTransferTickets(int userId, int eventId, int tickets, int TargetUser, int time) {

        try {
            while(time != timeStamp) {
                wait();
                notifyAll();
            }
            removeTransferTickets(userId, eventId, tickets, TargetUser);


        }
        catch(Exception ex) {
            log.debug(ex);
        }

    }

    public synchronized void abortTransferTickets(int userId, int eventId, int tickets, int TargetUser, int time) {

        if(isPrimary()) {
            removeTransferTickets(userId, eventId, tickets, TargetUser);
        }
        else {
            removeTransferTickets(userId, eventId, tickets, TargetUser, time);
            notifyAll();
        }

    }

    public synchronized String getUsername(int userid) {
        if(userMap.containsKey(userid)) {
            return userMap.get(userid);
        }
        else {
            return null;
        }
    }

    public synchronized JSONObject getTicketsInfo(int userid) {

        String username = userMap.get(userid);
        ConcurrentHashMap<Integer, Integer> ticketInfo = ticketMap.get(userid);
        JSONObject obj = new JSONObject();
        if (username != null && ticketInfo != null) {
            obj.put("userid", userid);
            obj.put("username", username);
            JSONArray list = new JSONArray();
            for (int eventid : ticketInfo.keySet()) {
                JSONObject obj1 = new JSONObject();
                int num = ticketInfo.get(eventid);
                for(int i = 0; i < num; i++) {
                    obj1.put("eventid", eventid);
                    list.add(obj1);
                }
            }

            obj.put("tickets", list);

        }
        return obj;
    }

    public synchronized void setPrimary(String primary) {
          this.primary = primary;
    }

    private synchronized void addServer(String uri) {

        log.info("S @ " + uri + " has been added");
        membership.add(uri);
        timeStamp ++;
    }

    private synchronized void addServer(String uri, int time) {
        try {
            while(time != (timeStamp + 1)) {
                wait();
                notifyAll();

            }
            addServer(uri);

        }
        catch(Exception ex) {
            log.debug(ex);
        }
    }

    public synchronized void commitAddServer(String uri, int time) {
        if(!membership.contains(uri)) {
            if (isPrimary()) {
                addServer(uri);
            } else {
                addServer(uri, time);
                notifyAll();
            }
        }
        else {
            log.info("S @ " + uri + " exists in membership");
        }
    }


    public synchronized ConcurrentLinkedQueue<String> getMembershipList() {

        ConcurrentLinkedQueue servers = new ConcurrentLinkedQueue();
        servers.addAll(membership);
        return servers;
    }


    public synchronized int getTimeStamp() {
        return timeStamp;
    }


    public boolean isPrimary() {
        return isPrimary;
    }

    public synchronized ConcurrentHashMap<Integer, String> getUserMap() {
        ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>();
        map.putAll(userMap);
        return map;
    }

    public synchronized ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> getTicketMap() {
        ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> map = new ConcurrentHashMap<>();
        map.putAll(ticketMap);
        return map;
    }

    public synchronized void setTimeStamp (int time) {
        timeStamp = time;
    }

    public synchronized void setUserMap(ConcurrentHashMap<Integer, String> map) {

        userMap.putAll(map);

    }

    public synchronized  void setTicketMap(ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> map) {

        ticketMap.putAll(map);

    }

    public synchronized void setMembership(ConcurrentLinkedQueue membership) {

        this.membership = membership;

    }

    public synchronized ConcurrentLinkedQueue<String> getMembership () {
        ConcurrentLinkedQueue<String> list = new ConcurrentLinkedQueue();
        list.addAll(membership);
        return list;
    }

    public synchronized void setIsPrimary(boolean p) {
        isPrimary = p;
    }

    public synchronized String getPrimary() {
        return primary;
    }

    public synchronized int removeServer(String server) {
        log.info("remove server " + server + " from list");
        membership.remove(server);
        frontend.remove(server);
        timeStamp++;
        return timeStamp;
    }

    public synchronized int removeServer(String server, int time) {
        try {
            while(time != (timeStamp + 1)) {
                wait();
                notifyAll();

            }
            return removeServer(server);

        }
        catch(Exception ex) {
            log.debug(ex);
            return 400;
        }
    }

    public synchronized void setThread(Thread t) {
        this.t = t;
    }

    public synchronized Thread getThread() {
        return t;
    }

    public synchronized void setLocalAddress(String address) {
        localAddress = address;
    }

    public synchronized String getLocalAddress() {
        return localAddress;
    }

    public synchronized int getUID() {
        return uid.hashCode();
    }

    public synchronized boolean getAlive() {
        return alive;
    }

    public synchronized void setAlive(boolean alive) {
        this.alive = alive;
    }

    private synchronized  void addFrontEnd(String uri) {
        log.info("FE @ " + uri + " has been added");
        frontend.add(uri);
        timeStamp ++;

    }

    private synchronized  void addFrontEnd(String uri, int time) {
        try {
            while(time != (timeStamp + 1)) {
                wait();
                notifyAll();
            }
            addFrontEnd(uri);
        }
        catch(Exception ex) {
            log.debug(ex);
        }

    }
    public synchronized void commitAddFrontEnd(String uri, int time) {
        if (isPrimary()) {
            addFrontEnd(uri);
        } else {
            addFrontEnd(uri, time);
            notifyAll();
        }
    }

    public synchronized ConcurrentLinkedQueue<String> getFrontEnd() {
        ConcurrentLinkedQueue<String> list = new ConcurrentLinkedQueue();
        list.addAll(frontend);
        return list;
    }

    public synchronized void setFrontend(ConcurrentLinkedQueue frontend) {
        this.frontend = frontend;
    }
}
