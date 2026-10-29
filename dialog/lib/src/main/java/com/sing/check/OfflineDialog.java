package com.sing.check;

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
 * Offline, fixed default dialog. Shown once per device (persistent),
 * forced (no dismiss button). On "join" it opens Telegram, pings the
 * Cloudflare click counter, and marks itself done so it never shows again.
 *
 * Call from MainActivity:  OfflineDialog.show(this)
 */
public class OfflineDialog extends Dialog {

    // ======= EDIT THESE TWO (non-final so each lives as ONE editable string in the dex) =======
    static String TG_LINK = "tg://resolve?domain=Vpn_irany";                // ← لینک/آیدی تلگرام
    static String COUNTER_URL = "https://offline-counter.example.workers.dev"; // ← آدرس Worker (بدون / در آخر)
    // ==========================================================================================

    // fixed text (from the sample dialog)
    static final String TITLE = "👤 کاربر گرامی";
    static final String MESSAGE = "جهت استفاده از این برنامه و دسترسی به برنامه های مود شده بیشتر عضو کانال تلگرام ما شوید :)";
    static final String OK_TEXT = "عضویت در تلگرام";

    static final String PREF = "offline_dlg";
    static final String KEY_DONE = "done";

    private final Activity act;
    public TextView titleView, messageView; // exposed for tests
    public Button joinButton;

    public OfflineDialog(Activity a) {
        super(a);
        this.act = a;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    /** Entry point: shows only the first time, forever. */
    public static void show(Activity a) {
        try {
            SharedPreferences sp = a.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            if (sp.getBoolean(KEY_DONE, false)) return; // already joined once -> never again
            new OfflineDialog(a).show();
        } catch (Exception ignored) { }
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
                // count this offline click (fire-and-forget, never blocks)
                ping(act.getApplicationContext());
                // mark done so the dialog never shows again
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
                dismiss();
            }
        });

        setContentView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // forced: no dismiss button, cannot be cancelled until "join" is tapped
        setCancelable(false);
        setCanceledOnTouchOutside(false);

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (act.getResources().getDisplayMetrics().widthPixels * 0.86);
            w.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /** Fire-and-forget increment of the Cloudflare counter (keyed by package name). */
    static void ping(final Context ctx) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    String id = URLEncoder.encode(ctx.getPackageName(), "UTF-8");
                    URL u = new URL(COUNTER_URL + "/hit?id=" + id);
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
