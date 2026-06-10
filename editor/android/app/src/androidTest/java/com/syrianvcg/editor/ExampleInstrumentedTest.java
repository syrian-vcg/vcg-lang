package com.syrianvcg.editor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation().getTargetContext();
        assertEquals("com.syrianvcg.editor.debug",
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().getTargetContext().getPackageName());
    }
}
