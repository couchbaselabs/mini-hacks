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
import javafx.stage.Modality;
import javafx.stage.Stage;
import sample.model.List;
import sample.util.StorageManager;
import sample.view.ListEditDialogController;
import sample.view.OverviewController;

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

        // set views and live query
        StorageManager storageManager = new StorageManager();

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
                        listData.removeAll();
                        for(QueryRow row : enumerator) {
                            listData.add(new List((String) (row.getDocument().getProperty("title")), "123"));
                        }
                    }
                });

            }
        });

        initRootLayout();

        showToDoOverview();
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
     * Opens a dialog to edit details for the specified person. If the user
     * clicks OK, the changes are saved into the provided person object and true
     * is returned.
     *
     * @param list the person object to be edited
     * @return true if the user clicked OK, false otherwise.
     */
    public boolean showPersonEditDialog(List list) {
        try {
            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Main.class.getResource("view/ListEditDialog.fxml"));
            AnchorPane page = (AnchorPane) loader.load();

            // Create the dialog Stage.
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit List");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            // Set the person into the controller.
            ListEditDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setList(list);

            // Show the dialog and wait until the user closes it
            dialogStage.showAndWait();

            return controller.isOkClicked();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Show the todo overview inside the root layout
     */
    public void showToDoOverview() {
        try {
            // Load ToDo overview.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Main.class.getResource("view/ToDoOVerview.fxml"));
            AnchorPane toDoOverview = (AnchorPane) loader.load();

            // Set the todo overview into the center of root layout.
            rootLayout.setCenter(toDoOverview);

            // Give the controller access to the main app.
            OverviewController toDoOverviewController = loader.getController();
            toDoOverviewController.setMainApp(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
