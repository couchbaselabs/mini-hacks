package sample.view;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import sample.Main;
import sample.model.List;

/**
 * Created by jamesnocentini on 18/10/15.
 */
public class OverviewController {
    @FXML
    private TableView<List> listTableView;
    @FXML
    private TableColumn<List, String> listColumn;

    // Reference the main application.
    private Main mainApp;

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
        listColumn.setCellValueFactory(cellData -> cellData.getValue().titleProperty());


        // Listen for selection changes and show the person details when changed.
//        personTable.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> showPersonDetails(newValue)));
    }

    /**
     * Is called by the main application to give a reference back to itself.
     *
     * @param mainApp
     */
    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;

        listTableView.setItems(mainApp.getListData());
    }

    /**
     * Fills all text fields to show details about the list.
     * If the specified list is null, all text fields are cleared.
     *
     * @param list the list or null
     */
    private void showListDetails(List list) {
        if (list != null) {

        } else {
            // List is null, remove all text
            list.setTitle("");
        }
    }

    /**
     * Called when the user clicks the new button. Opens a dialog to edit details for a new list.
     */
    @FXML
    private void handleNewList() {
        List tempList = new List();
        mainApp.showPersonEditDialog(tempList);
    }

}
