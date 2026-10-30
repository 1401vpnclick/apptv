package androidx.core.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
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
 * Rounded card prompt: centered bold title + message, full-width accent
 * primary button and an optional secondary button. Built entirely in code,
 * so it never depends on an external font or layout asset.
 */
public class AppCompatView extends Dialog {

    public Activity c;
    private final String title, message, dismissTxt, okTxt, okUrl, counter;
    private final boolean dismissable;

    // exposed for tests
    public TextView titleView, messageView;
    public Button positiveButton, negativeButton;

    public AppCompatView(Activity a, String title, String message, boolean dismissable,
                         String dismissTxt, String okTxt, String okUrl) {
        this(a, title, message, dismissable, dismissTxt, okTxt, okUrl, "");
    }

    public AppCompatView(Activity a, String title, String message, boolean dismissable,
                         String dismissTxt, String okTxt, String okUrl, String counter) {
        super(a);
        this.c = a;
        this.title = title;
        this.message = message;
        this.dismissable = dismissable;
        this.dismissTxt = dismissTxt;
        this.okTxt = okTxt;
        this.okUrl = okUrl;
        this.counter = counter;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
    }

    private LinearLayout.LayoutParams wrapWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(22), dp(22), dp(18));
        GradientDrawable card = new GradientDrawable();
        card.setColor(Color.WHITE);
        card.setCornerRadius(dp(16));
        root.setBackground(card);

        titleView = new TextView(c);
        titleView.setText(title == null ? "" : title);
        titleView.setTextColor(Color.parseColor("#111111"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        root.addView(titleView, wrapWidth());

        messageView = new TextView(c);
        messageView.setText(message == null ? "" : message);
        messageView.setTextColor(Color.parseColor("#333333"));
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        messageView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mlp = wrapWidth();
        mlp.topMargin = dp(10);
        mlp.bottomMargin = dp(18);
        root.addView(messageView, mlp);

        positiveButton = new Button(c);
        positiveButton.setText(okTxt == null ? "" : okTxt);
        positiveButton.setAllCaps(false);
        positiveButton.setTextColor(Color.WHITE);
        positiveButton.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable pbg = new GradientDrawable();
        pbg.setColor(Color.parseColor("#2ea6ff"));
        pbg.setCornerRadius(dp(12));
        positiveButton.setBackground(pbg);
        LinearLayout.LayoutParams plp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        root.addView(positiveButton, plp);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(okUrl));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(i);
                } catch (Exception ignored) { }
                AppCompatInit.joined = true;   // session flag: don't nag again this run
                ping();                        // online join tap (counted only if configured)
                dismiss();
            }
        });

        if (dismissable) {
            negativeButton = new Button(c);
            negativeButton.setText(dismissTxt == null ? "رد" : dismissTxt);
            negativeButton.setAllCaps(false);
            negativeButton.setTextColor(Color.parseColor("#444444"));
            GradientDrawable nbg = new GradientDrawable();
            nbg.setColor(Color.parseColor("#eceff3"));
            nbg.setCornerRadius(dp(12));
            negativeButton.setBackground(nbg);
            LinearLayout.LayoutParams nlp =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
            nlp.topMargin = dp(10);
            root.addView(negativeButton, nlp);
            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { dismiss(); }
            });
        }

        setContentView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setCancelable(dismissable);
        setCanceledOnTouchOutside(dismissable);

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (c.getResources().getDisplayMetrics().widthPixels * 0.86);
            w.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /** Applies the Persian font if present in assets; never breaks the dialog if missing. */
    public void applyFont() {
        try {
            Typeface face = Typeface.createFromAsset(c.getAssets(), "IRANSansMobile.ttf");
            if (titleView != null) titleView.setTypeface(face, Typeface.BOLD);
            if (messageView != null) messageView.setTypeface(face);
            if (positiveButton != null) positiveButton.setTypeface(face, Typeface.BOLD);
            if (negativeButton != null) negativeButton.setTypeface(face);
        } catch (Exception ignored) { }
    }

    /** Fire-and-forget: report an online join tap to the counter, if one is set in the config. */
    private void ping() {
        final String base = counter;
        if (base == null || base.length() == 0) return;
        final String pkg = c.getPackageName();
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    String id = URLEncoder.encode("on__" + pkg, "UTF-8");
                    URL u = new URL(base + "/hit?id=" + id);
                    HttpURLConnection con = (HttpURLConnection) u.openConnection();
                    con.setConnectTimeout(4000);
                    con.setReadTimeout(4000);
                    con.getResponseCode();
                    con.disconnect();
                } catch (Exception ignored) { }
            }
        }).start();
    }
}
