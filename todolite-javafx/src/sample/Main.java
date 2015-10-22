package sample;

import com.couchbase.lite.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import sample.model.List;
import sample.view.overview.OverviewController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main extends Application {

    private Stage primaryStage;
    private BorderPane rootLayout;

    /**
     * The data as an observable list of Persons.
     */
    private ObservableList<List> listData = FXCollections.observableArrayList();

    /**
     * Constructor
     */
    public Main() {
    }

    /**
     * Returns the data as an observable list of Persons.
     * @return
     */
    public ObservableList<List> getListData() {
        return listData;
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        this.primaryStage = primaryStage;

        StorageManager.getInstance().startReplications();

        LiveQuery liveQuery = StorageManager.getInstance().database.createAllDocumentsQuery().toLiveQuery();
        QueryEnumerator enumerator = liveQuery.run();
        for(QueryRow row : enumerator) {
            listData.add(new List((String) (row.getDocument().getProperty("title")), "123"));
        }
        liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
            @Override
            public void changed(LiveQuery.ChangeEvent changeEvent) {
                System.out.println("update");
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        QueryEnumerator enumerator = null;
                        try {
                            enumerator = liveQuery.run();
                        } catch (CouchbaseLiteException e) {
                            e.printStackTrace();
                        }
                        listData.clear();
                        for(QueryRow row : enumerator) {
                            listData.add(new List((String) (row.getDocument().getProperty("title")), "123"));
                        }
                    }
                });

            }
        });

        initRootLayout();

        showToDoOverview();

        createUserProfile();
    }

    /**
     * Initializes the root layout
     */
    public void initRootLayout() {
        try {
            // Load root layout from fxml file.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Main.class.getResource("view/RootLayout.fxml"));
            rootLayout = (BorderPane) loader.load();

            // Show the scene containing the root layout
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Show the todo overview inside the root layout
     */
    public void showToDoOverview() {
        try {
            // Load ToDo overview.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Main.class.getResource("view/overview/ToDoOVerview.fxml"));
            AnchorPane toDoOverview = (AnchorPane) loader.load();

            // Set the todo overview into the center of root layout.
            rootLayout.setCenter(toDoOverview);

            // Give the controller access to the main app.
            OverviewController toDoOverviewController = loader.getController();
            toDoOverviewController.setMainApp(this);
            toDoOverviewController.setData(getListData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createUserProfile() {
        Map<String, Object> properties = new HashMap<String, Object>();

        properties.put("name", "Wayne Carter");
        properties.put("type", "profile");
        properties.put("user_id", "wayne");

        Document document = StorageManager.getInstance().database.getDocument("p:wayne");
        try {
            document.putProperties(properties);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
