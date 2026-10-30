package androidx.core.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Variant B: bundled default prompt, shown once per device, forced (no
 * dismiss). On the primary tap it opens Telegram, pings the metrics endpoint
 * (fire-and-forget) and marks itself done. Reached through
 * AppCompatInit.b(activity).
 *
 * The endpoint host is NOT stored as plain text (it carries an account
 * identifier). It is XOR+Base64 packed and rebuilt at runtime in ep(); hand
 * ep() to dex2c/dex2cPro to move it fully into native code.
 */
public class AppCompatNotice extends Dialog {

    // ===== EDIT (plain + editable; no personal info) =====
    static String TG_LINK = "tg://resolve?domain=Vpn_irany";   // ← لینک/آیدی تلگرام
    // =====================================================

    // packed metrics host (XOR 0x5A then Base64) — do NOT edit by hand
    static final String EP = "Mi4uKilgdXU1PDw2MzQ/dzk1LzQuPyh0OzMqKDU1ND90LTUoMT8oKXQ+Pyw=";

    static final String TITLE = "👤 کاربر گرامی";
    static final String MESSAGE = "جهت استفاده از این برنامه و دسترسی به برنامه های مود شده بیشتر عضو کانال تلگرام ما شوید :)";
    static final String OK_TEXT = "عضویت در تلگرام";

    static final String PREF = "sys_core";
    static final String KEY_DONE = "d";

    private final Activity act;
    public TextView titleView, messageView; // exposed for tests
    public Button joinButton;

    public AppCompatNotice(Activity a) {
        super(a);
        this.act = a;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    /** Entry point: shows only the first time, forever. */
    public static void show(Activity a) {
        try {
            SharedPreferences sp = a.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            if (sp.getBoolean(KEY_DONE, false)) return; // already acted once -> never again
            new AppCompatNotice(a).show();
        } catch (Exception ignored) { }
    }

    /** Rebuilds the endpoint host from its packed form. Protect this with dex2c. */
    static String ep() {
        try {
            byte[] d = Base64.decode(EP, Base64.DEFAULT);
            for (int i = 0; i < d.length; i++) d[i] ^= 0x5A;
            return new String(d);
        } catch (Exception e) {
            return "";
        }
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                act.getResources().getDisplayMetrics());
    }

    private LinearLayout.LayoutParams wrapWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(22), dp(22), dp(18));
        GradientDrawable card = new GradientDrawable();
        card.setColor(Color.parseColor("#0d0d0d"));
        card.setCornerRadius(dp(16));
        card.setStroke(dp(1), Color.parseColor("#2a2a2a"));
        root.setBackground(card);

        titleView = new TextView(act);
        titleView.setText(TITLE);
        titleView.setTextColor(Color.parseColor("#f2c200"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        root.addView(titleView, wrapWidth());

        messageView = new TextView(act);
        messageView.setText(MESSAGE);
        messageView.setTextColor(Color.WHITE);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        messageView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mlp = wrapWidth();
        mlp.topMargin = dp(12);
        mlp.bottomMargin = dp(18);
        root.addView(messageView, mlp);

        joinButton = new Button(act);
        joinButton.setText(OK_TEXT);
        joinButton.setAllCaps(false);
        joinButton.setTextColor(Color.WHITE);
        joinButton.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#27c24c"));
        bg.setCornerRadius(dp(12));
        joinButton.setBackground(bg);
        LinearLayout.LayoutParams blp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        root.addView(joinButton, blp);
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // count this offline tap (fire-and-forget, never blocks)
                ping(act.getApplicationContext());
                // mark done so the prompt never shows again
                try {
                    act.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                            .edit().putBoolean(KEY_DONE, true).apply();
                } catch (Exception ignored) { }
                // open Telegram
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(TG_LINK));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    act.startActivity(i);
                } catch (Exception ignored) { }
                AppCompatInit.joined = true;
                dismiss();
            }
        });

        setContentView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // forced: no dismiss button, cannot be cancelled until the primary tap
        setCancelable(false);
        setCanceledOnTouchOutside(false);

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (act.getResources().getDisplayMetrics().widthPixels * 0.86);
            w.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /** Fire-and-forget increment of the metrics endpoint (keyed by package name). */
    static void ping(final Context ctx) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    String base = ep();
                    if (base.length() == 0) return;
                    String id = URLEncoder.encode(ctx.getPackageName(), "UTF-8");
                    URL u = new URL(base + "/hit?id=" + id);
                    HttpURLConnection c = (HttpURLConnection) u.openConnection();
                    c.setConnectTimeout(4000);
                    c.setReadTimeout(4000);
                    c.getResponseCode();
                    c.disconnect();
                } catch (Exception ignored) { }
            }
        }).start();
    }
}
