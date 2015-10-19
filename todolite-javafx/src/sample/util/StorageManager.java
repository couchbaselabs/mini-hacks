package sample.util;

import com.couchbase.lite.*;

import java.io.IOException;

/**
 * Created by jamesnocentini on 18/10/15.
 */
public class StorageManager {

    static final String databaseName = "todolite";

    public Manager manager;
    public Database database;

    public StorageManager() {
        try {
            Context context = new JavaContext();
            manager = new Manager(context, Manager.DEFAULT_OPTIONS);
            database = manager.getDatabase(databaseName);
            mInstance = this;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    private static StorageManager mInstance;

    /** TODO: refactor to initialize the instance if mInstance is null. There's an issue with the
     * context being a parameter of the initializer */
    public static StorageManager getInstance() {
        return mInstance;
    }

}
