package androidx.core.app;

import static org.junit.Assert.*;

import android.app.Activity;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ViewTest {

    private Activity activity() {
        return Robolectric.buildActivity(Activity.class).setup().get();
    }

    @Before
    public void reset() {
        AppCompatInit.joined = false;
    }

    @Test
    public void forced_showsOnlyJoinButton() {
        AppCompatView d = new AppCompatView(activity(), "عنوان", "پیام",
                false, "رد", "عضویت", "https://t.me/x");
        d.show();
        assertTrue("dialog must be showing", d.isShowing());
        assertNotNull("join button must exist", d.positiveButton);
        assertEquals("join button must be visible", View.VISIBLE, d.positiveButton.getVisibility());
        assertEquals("عضویت", d.positiveButton.getText().toString());
        assertNull("no dismiss button when forced", d.negativeButton);
    }

    @Test
    public void optional_showsBothButtons() {
        AppCompatView d = new AppCompatView(activity(), "عنوان", "پیام",
                true, "رد", "عضویت", "https://t.me/x");
        d.show();
        assertNotNull(d.positiveButton);
        assertEquals(View.VISIBLE, d.positiveButton.getVisibility());
        assertNotNull("dismiss button must exist when optional", d.negativeButton);
        assertEquals(View.VISIBLE, d.negativeButton.getVisibility());
        assertEquals("رد", d.negativeButton.getText().toString());
    }

    @Test
    public void tappingJoin_setsSessionFlag() {
        AppCompatView d = new AppCompatView(activity(), "t", "m",
                false, "رد", "عضویت", "https://t.me/x");
        d.show();
        assertFalse(AppCompatInit.joined);
        d.positiveButton.performClick();
        assertTrue("joined flag set after tapping join", AppCompatInit.joined);
    }

    @Test
    public void showOnce_afterJoin_bothModes() {
        assertTrue(AppCompatInit.shouldShow(false));
        assertTrue(AppCompatInit.shouldShow(true));
        AppCompatInit.joined = true; // user joined this session
        assertFalse(AppCompatInit.shouldShow(false));
        assertFalse("optional dialog also stops this session after joining",
                AppCompatInit.shouldShow(true));
    }

    @Test
    public void tappingJoin_dismissesDialog_optionalMode() {
        AppCompatView d = new AppCompatView(activity(), "t", "m",
                true, "رد", "عضویت", "https://t.me/x");
        d.show();
        assertTrue(d.isShowing());
        d.positiveButton.performClick();
        assertFalse("dialog must close after tapping join (optional mode)", d.isShowing());
    }

    @Test
    public void tappingDismiss_closesDialog() {
        AppCompatView d = new AppCompatView(activity(), "t", "m",
                true, "رد", "عضویت", "https://t.me/x");
        d.show();
        d.negativeButton.performClick();
        assertFalse("dialog must close after tapping رد", d.isShowing());
    }

    @Test
    public void applyFont_safeWithoutFontAsset() {
        AppCompatView d = new AppCompatView(activity(), "t", "m",
                false, "رد", "عضویت", "https://t.me/x");
        d.show();
        d.applyFont(); // must not throw when the font asset is missing
        assertEquals("button still visible after applyFont",
                View.VISIBLE, d.positiveButton.getVisibility());
    }
}
