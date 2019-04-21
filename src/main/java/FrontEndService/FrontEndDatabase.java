package FrontEndService;

/**
 * Database to save the address of primary user server.
 */
public class FrontEndDatabase {

    private static FrontEndDatabase singleton = new FrontEndDatabase();
    private static String primaryAddress;

    private FrontEndDatabase() {}

    public static FrontEndDatabase getInstance() {
        return singleton;
    }

    public void setPrimaryAddress(String address) {
        primaryAddress = address;
    }

    public String getPrimaryAddress() {
        return primaryAddress;
    }


}
