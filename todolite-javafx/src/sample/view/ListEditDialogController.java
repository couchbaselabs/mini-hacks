package sample.view;

import com.couchbase.lite.*;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import sample.model.List;
import sample.util.StorageManager;

import javax.jws.Oneway;
import javax.xml.crypto.Data;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Dialog to add a new List.
 */
public class ListEditDialogController {

    @FXML
    private TextField titleField;

    private Stage dialogStage;
    private List list;
    private Boolean okClicked = false;

    /**
     * Initializes the controller class. This method is automatically called after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
    }

    /**
     * Sets the stage of this dialog.
     *
     * @param dialogStage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Sets the list to be edited in the dialog.
     *
     * @param list
     */
    public void setList(List list) {
        this.list = list;

        titleField.setText(list.getTitle());
    }

    /**
     * Returns true if the user clicked OK, false otherwise.
     *
     * @return
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    /**
     * Called when the user clicks ok.
     */
    @FXML private void handleOk() {
        System.out.println("create a new list");

        Map<String, Object> properties = new HashMap<>();
        properties.put("title", titleField.getText());

        Document newDoc = StorageManager.getInstance().database.createDocument();
        try {
            newDoc.putProperties(properties);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

    }

    /**
     * Called when the user clicks cancel.
     */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    /**
     * Validates the user input in the text fields.
     *
     * @return true if the input is valid
     */
    public boolean isInputValid() {
        String errorMessage = "";

        if (titleField.getText() == null || titleField.getText().length() == 0) {
            errorMessage += "No valid title!\n";
        }

        if (errorMessage.length() == 0) {
            return true;
        } else {
            // Show the error message.
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(dialogStage);
            alert.setTitle("Invalid Fields");
            alert.setHeaderText("Please correct invalid fields");
            alert.setContentText(errorMessage);

            alert.showAndWait();

            return false;
        }
    }
}
