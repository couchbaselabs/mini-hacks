package com.couchbase.dbdownloadexample;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.couchbase.lite.util.ZipUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class DatabaseDownloader extends AsyncTask {

    private String stringURL = "https://cl.ly/3W0l0R1G0P11/todo.cblite2.zip";
    private Context context;
    private DownloaderListener downloaderListener;

    public DatabaseDownloader(Context context) {
        this.context = context;
    }

    public void setDownloaderListener(DownloaderListener downloaderListener) {
        this.downloaderListener = downloaderListener;
    }

    @Override
    protected Object doInBackground(Object[] params) {

        int count;
        try {
            URL url = new URL(stringURL);
            URLConnection conection = url.openConnection();
            conection.connect();

            // Download the file
            InputStream input = new BufferedInputStream(url.openStream(), 8192);

            // Output stream
            File dataDir = getDataFolder(context);
            File dataFile = new File(dataDir, "todo.cblite2.zip");
            OutputStream output = new FileOutputStream(dataFile);

            byte data[] = new byte[1024];

            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();

            // Unzip the database in the Couchbase Lite app directory (i.e files)
            ZipUtils.unzip(new FileInputStream(dataFile), this.context.getFilesDir());

        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
        }

        downloaderListener.onCompleted();

        return null;
    }

    private File getDataFolder(Context context) {
        File dataDir = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            dataDir = new File(Environment.getExternalStorageDirectory(), "myappdata");
            if (!dataDir.isDirectory()) {
                dataDir.mkdirs();
            }
        }

        if (!dataDir.isDirectory()) {
            dataDir = context.getFilesDir();
        }

        return dataDir;
    }

}
