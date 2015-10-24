package sample;

import com.couchbase.lite.*;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.auth.Authorizer;
import com.couchbase.lite.replicator.Replication;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class StorageManager {



    static private String localDatabase = "todolite";
//    static private String remoteDatabase = "http://localhost:4984/todos";
    static private String remoteDatabase = "http://d929c39c-jamiltz.node.tutum.io:4984/todos/";
    private Manager manager;
    public Database database;

    private static StorageManager instance = new StorageManager();

    private StorageManager() {

        try {
            Context context = new JavaContext();
            manager = new Manager(context, Manager.DEFAULT_OPTIONS);
            database = manager.getDatabase(localDatabase);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

    }

    public void startReplications() {

        URL remoteURL = null;
        try {
            remoteURL = new URL(remoteDatabase);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Authenticator authenticator = AuthenticatorFactory.createBasicAuthenticator("wayne", "letmein");

        Replication pull = database.createPullReplication(remoteURL);
        Replication push = database.createPushReplication(remoteURL);

        pull.setAuthenticator(authenticator);
        push.setAuthenticator(authenticator);

        pull.setContinuous(true);
        push.setContinuous(true);

        pull.start();
        push.start();

    }

    public static StorageManager getInstance() {
        return instance;
    }

}
