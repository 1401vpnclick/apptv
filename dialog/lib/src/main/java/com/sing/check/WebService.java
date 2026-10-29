package com.sing.check;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Fetches the remote JSON config and shows the join dialog.
 * Called from a host app's MainActivity via:  WebService.Heh(this)
 */
public class WebService extends AsyncTask<String, Void, String> {

    /** Session flag: set when the user taps "join". Resets when the app process restarts. */
    public static boolean joined = false;

    Activity activity;
    private String urlStr = "";

    public WebService(Activity activity, String url) {
        this.activity = activity;
        this.urlStr = url;
    }

    public static void Heh(Activity activity) {
        new WebService(activity,
                "https://raw.githubusercontent.com/1401vpnclick/Vpn-solv/refs/heads/main/CloverVPN%201.2.3%20.txt")
                .execute();
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            URL mUrl = new URL(urlStr);
            HttpURLConnection con = (HttpURLConnection) mUrl.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-length", "0");
            con.addRequestProperty("pkgName", activity.getPackageName());
            con.setUseCaches(false);
            con.setAllowUserInteraction(false);
            con.setConnectTimeout(100000);
            con.setReadTimeout(100000);
            con.connect();
            if (con.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Shared decision used by onPostExecute (and exercised by tests). */
    public static boolean shouldShow(boolean dismissable) {
        // Show on every launch until the user taps "join" this session.
        // Tapping "رد" (dismiss) does NOT set joined, so the dialog keeps
        // appearing on later launches until the user actually joins.
        return !joined;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (s == null) return;
        try {
            JSONObject o = new JSONObject(s);
            if (!o.has("status") || !o.getString("status").equals("ok")) return;
            String title = o.getString("title");
            String message = o.getString("message");
            boolean dismissable = o.getBoolean("dismissable");
            String dismissTxt = o.getString("dismissTxt");
            String okTxt = o.getString("okTxt");
            String okUrl = o.getString("okUrl");
            if (!shouldShow(dismissable)) return;
            CustomDialogClass d = new CustomDialogClass(activity, title, message,
                    dismissable, dismissTxt, okTxt, okUrl);
            d.show();
            d.setTypeeee();
        } catch (Exception e) {
            Log.i("pooya", e.toString());
        }
    }
}
