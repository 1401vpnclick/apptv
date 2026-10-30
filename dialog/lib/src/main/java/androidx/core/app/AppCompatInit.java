package androidx.core.app;

import android.app.Activity;

/**
 * Entry facade. Two call sites (both take the host Activity):
 *
 *   androidx.core.app.AppCompatInit.a(this);   // remote-config variant
 *   androidx.core.app.AppCompatInit.b(this);   // bundled-default variant
 *
 * Neither name reveals its role. Keep a() and b() as the methods you hand to
 * dex2c / dex2cPro for native protection — they are small and self-contained.
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

    /** Variant B — shows the bundled default prompt once per device. */
    public static void b(Activity act) {
        try {
            AppCompatNotice.show(act);
        } catch (Exception ignored) { }
    }
}
