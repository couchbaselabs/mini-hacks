# Implement Facebook Login a JavaFX application

To authenticate users on Sync Gateway, you can use basic authentication, custom authentication. There is built-in support for Facebook in Sync Gateway which makes it very easy to start replicating documents between users.

The Java application can pass arbitratry scripts to the JavaScript engine of a `WebEngine` object by callend the `WebEngine.executeScript()` method.

## Getting started

Open IntelliJ IDEA and choose the **Create New Project** menu. On the left pane, select the **JavaFX** application template and set the Project SDK to 1.8.

![](assets/starter-template.png)

Name the application **ToDoLite-JavaFX** and click Finish, this will open your newly created project in a new window. Before we begin writing code, there a couple configuration settings to change. Select the **Edit Configurations...** menu in the top right corner which will open a new window. Check the **Single instance only** box to ensure that the IDE doesn't start a new instance of the application every time you click the Run button.

![](assets/single-instance.png)

With that, let's turn our attention to adding Couchbase Lite as a dependency to the project. Couchbase Lite is



Select **File\Project Structure...** from the top menu bar, a new window will open and on the **Modules** tab, add a new Library from Maven:

![](assets/maven-module.png)

In the search field, type **com.couchbase.lite:couchbase-lite-java:1.1.0** and click **OK**. This will download the library and add it to the project. Next, you will add a JAR file that contains the native library for the platform you're running the application on. For OS X users, download this JAR file and add it in a new directory called **libraries** in your project. Return the **Modules** window and add this JAR file from the **Add** menu:

![](assets/jar-dependency.png)

Click **OK** and run the application by click the **Run** button in the top right corner. You should see a blank window with the message **Hello World** in the status bar:

![](assets/hello-world.png)

## Building the UI



## Model classes

The data model is the following:

![](assets/data-model.png)

With JavaFX it's common to use **Properties** for all fields of a model class. A **Property** allows us, for example, to automatically be notified when the **title** or any other variable is changed. This helps us keep the view in sync with the data.