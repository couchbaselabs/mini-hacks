package sample.view.overview;

import com.couchbase.lite.*;
import com.couchbase.lite.util.Log;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import sample.Main;
import sample.StorageManager;
import sample.model.List;
import sample.model.Profile;
import sample.model.Task;
import sample.util.ModelHelper;
import sample.view.BaseController;
import sample.view.ListEditDialogController;

import javax.management.Query;
import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Observable;

/**
 * Created by jamesnocentini on 18/10/15.
 */
public class OverviewController extends BaseController {

    /**
     * The list of lists
     */
    @FXML
    private TableView<List> listTableView;
    @FXML
    private TableColumn<List, String> listColumn;

    /**
     * The list of tasks
     */
    @FXML
    private TableView<Task> taskTableView;
    @FXML
    private TableColumn<Task, String> taskColumn;

    /**
     * The list of profiles
     */
    @FXML
    private TableView<Profile> profileTableView;
    @FXML
    private TableColumn<Profile, String> profileColumn;


    /**
     * The data as an observable list of Tasks.
     */
    private ObservableList<Task> taskData = FXCollections.observableArrayList();

    private ObservableList<Profile> profileData = FXCollections.observableArrayList();
    /**
     * Returns the data as an observable list of Tasks.
     */
    public ObservableList<Task> getTaskData() {
        return taskData;
    }

    public ObservableList<Profile> getProfileData() {
        return profileData;
    }

    private LiveQuery taskQuery;
    /**
     * The constructor.
     * The constructor is called before the initialize() method.
     */
    public OverviewController() {
    }

    /**
     * Initializes the controller class. This method is automatically called after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
        // Initialize the person table with the two columns.
        listColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));

        // Listen for selection changes and show the list details.
        listTableView.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> showListDetails(newValue)));

        taskColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getChecked()) {
                return new SimpleStringProperty("x    " + cellData.getValue().getTitle());
            }
            return new SimpleStringProperty("      " + cellData.getValue().getTitle());
        });

        taskColumn.setCellFactory(new Callback<TableColumn<Task, String>, TableCell<Task, String>>() {
            @Override
            public TableCell<Task, String> call(TableColumn<Task, String> param) {
                return new TableCell<Task, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!isEmpty()) {
                            setText(item);
                        }
                    }
                };
            }
        });

        taskTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> changeCheck(newValue));

        profileColumn.setCellValueFactory(cellData -> {
            List list = listTableView.getSelectionModel().getSelectedItem();
            Boolean isUserInList = list.getMembers().contains(cellData.getValue().getDocumentId());
            if (isUserInList) {
                return new SimpleStringProperty("x    " + cellData.getValue().getName());
            }
            return new SimpleStringProperty("      " + cellData.getValue().getName());
        });

        profileTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> shareWithUser(newValue));

    }

    public void setData(ObservableList<List> data) {
        listTableView.setItems(data);
    }

    private List selectedList;

    /**
     * Fills all text fields to show details about the list.
     * If the specified list is null, all text fields are cleared.
     *
     * @param list the list or null
     */
    private void showListDetails(List list) {
        if (list != null) {
            selectedList = list;
            String listId = list.getDocumentId();

            taskTableView.refresh();
            taskData.clear();
            taskTableView.setItems(getTaskData());
            startTaskLiveQueryForList(listId);
            profileTableView.refresh();
            profileData.clear();
            profileTableView.setItems(getProfileData());
            startProfilesLiveQuery();

            taskColumn.setText(list.getTitle());
        } else {
            // List is null, remove all text
        }
    }

    private void startProfilesLiveQuery() {
        LiveQuery liveQuery = Profile.getQuery(StorageManager.getInstance().database, "wayne").toLiveQuery();
        QueryEnumerator enumerator = null;
        try {
            enumerator = liveQuery.run();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        for (QueryRow row : enumerator) {
            Document document = row.getDocument();
            Profile profile = ModelHelper.modelForDocument(document, Profile.class);
            profileData.add(profile);
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
                        profileData.clear();
                        for (QueryRow row : enumerator) {
                            Document document = row.getDocument();
                            Profile profile = ModelHelper.modelForDocument(document, Profile.class);
                            profileData.add(profile);
                        }
                    }
                });

            }
        });
    }

    /**
     * Change the status of the cell
     * @param task
     */
    private void changeCheck(Task task) {
        task.setChecked(!task.getChecked());
        ModelHelper.save(StorageManager.getInstance().database, task);
    }

    private void startTaskLiveQueryForList(String listId) {
        if (taskQuery != null) {
            taskQuery = null;
        }
        LiveQuery liveQuery = Task.getQuery(StorageManager.getInstance().database, listId).toLiveQuery();
        QueryEnumerator enumerator = null;
        try {
            enumerator = liveQuery.run();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        for(QueryRow row : enumerator) {
            Document document = row.getDocument();
            Task task = ModelHelper.modelForDocument(document, Task.class);
            taskData.add(task);
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
                        taskData.clear();
                        for(QueryRow row : enumerator) {
                            Document document = row.getDocument();
                            Task task = ModelHelper.modelForDocument(document, Task.class);
                            taskData.add(task);
                        }
                    }
                });

            }
        });
    }

    /**
     * Called when the user clicks the new button. Opens a dialog to edit details for a new list.
     */
    @FXML
    private void handleNewList() {
        List tempList = new List();
        showListEditDialog(tempList);
    }

    private void shareWithUser(Profile profile) {
        List list = listTableView.getSelectionModel().getSelectedItem();
        Boolean isUserSelectedAlreadyMember = list.getMembers().contains(profile.getDocumentId());
        if (isUserSelectedAlreadyMember) {
            selectedList.getMembers().remove(profile.getDocumentId());
        } else {
            selectedList.getMembers().add(profile.getDocumentId());
        }
        ModelHelper.save(StorageManager.getInstance().database, selectedList);
    }

    /**
     * Opens a dialog to edit details for the specified person. If the user
     * clicks OK, the changes are saved into the provided person object and true
     * is returned.
     *
     * @param list the person object to be edited
     * @return true if the user clicked OK, false otherwise.
     */
    public boolean showListEditDialog(List list) {
        try {
            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Main.class.getResource("view/ListEditDialog.fxml"));
            AnchorPane page = (AnchorPane) loader.load();

            // Create the dialog Stage.
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit List");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(mainApp.getPrimaryStage());
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


}
