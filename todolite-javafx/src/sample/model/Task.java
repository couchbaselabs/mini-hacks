package sample.model;

import com.couchbase.lite.*;
import com.couchbase.lite.util.Log;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.application.Application;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jamesnocentini on 18/10/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {

    @JsonProperty(value = "_id")
    private String documentId;

    private String type;

    @JsonProperty(value = "list_id")
    private String listId;

    private String title;

    private Boolean checked;

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    private static final String VIEW_NAME = "tasks";
    private static final String DOC_TYPE = "task";

    public static Query getQuery(Database database, String listDocId) {
        com.couchbase.lite.View view = database.getView(VIEW_NAME);
        if (view.getMap() == null) {
            Mapper map = new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    if (DOC_TYPE.equals(document.get("type"))) {
                        java.util.List<Object> keys = new ArrayList<Object>();
                        keys.add(document.get("list_id"));
                        keys.add(document.get("created_at"));
                        emitter.emit(keys, document);
                    }
                }
            };
            view.setMap(map, "1");
        }

        Query query = view.createQuery();
        query.setDescending(true);

        java.util.List<Object> startKeys = new ArrayList<Object>();
        startKeys.add(listDocId);
        startKeys.add(new HashMap<String, Object>());

        java.util.List<Object> endKeys = new ArrayList<Object>();
        endKeys.add(listDocId);

        query.setStartKey(startKeys);
        query.setEndKey(endKeys);

        return query;
    }

    public static Document createTask(Database database,
                                      String title,
                                      String listId) throws CouchbaseLiteException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Calendar calendar = GregorianCalendar.getInstance();
        String currentTimeString = dateFormatter.format(calendar.getTime());

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("type", DOC_TYPE);
        properties.put("title", title);
        properties.put("checked", Boolean.FALSE);
        properties.put("created_at", currentTimeString);
        properties.put("list_id", listId);

        Document document = database.createDocument();

        UnsavedRevision revision = document.createRevision();
        revision.setUserProperties(properties);

        revision.save();

        Log.d("app", "Created doc: %s", document.getId());

        return document;
    }

    public static void updateCheckedStatus(Document task, boolean checked)
            throws CouchbaseLiteException {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.putAll(task.getProperties());
        properties.put("checked", checked);
        task.putProperties(properties);
    }

    public static void deleteTask(Document task) throws CouchbaseLiteException {
        task.delete();
        Log.d("app", "Deleted doc: %s", task.getId());
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
