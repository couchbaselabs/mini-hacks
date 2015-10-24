package sample.view;

import sample.Main;

/**
 * Created by jamesnocentini on 21/10/15.
 */
public class BaseController {

    // Reference the main application.
    protected Main mainApp;

    /**
     * Is called by the main application to give a reference back to itself.
     *
     * @param mainApp
     */
    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

}
