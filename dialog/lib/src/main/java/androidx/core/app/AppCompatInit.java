package androidx.core.app;

import android.app.Activity;

/**
 * Entry facade (online-only build).
 *
 *   androidx.core.app.AppCompatInit.a(this);   // remote-config variant
 *
 * This build contains ONLY the online dialog (remote config). The bundled
 * default prompt and the offline click counter are not included.
 * Keep a() as the method you hand to dex2c / dex2cPro for native protection.
 */
public final class AppCompatInit {

    private AppCompatInit() { }

    /** Session flag: set once the user acts. Resets when the process restarts. */
    public static boolean joined = false;

    /** Shared gate: keep prompting until the user acts this session. */
    public static boolean shouldShow(boolean optional) {
        return !joined;
    }

    /** Variant A — fetches the remote config and shows the prompt. */
    public static void a(Activity act) {
        try {
            new AppCompatSync(act, AppCompatSync.SRC).execute();
        } catch (Exception ignored) { }
    }
}
