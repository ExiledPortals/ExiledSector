package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EscapeKeyTest {

    private final EscapeKey escapeKey = new EscapeKey();

    private static InputEventAPI escapeDown() {
        InputEventAPI event = mock(InputEventAPI.class);
        when(event.isKeyDownEvent()).thenReturn(true);
        when(event.getEventValue()).thenReturn(Keyboard.KEY_ESCAPE);
        return event;
    }

    private static InputEventAPI escapeUp() {
        InputEventAPI event = mock(InputEventAPI.class);
        when(event.isKeyUpEvent()).thenReturn(true);
        when(event.getEventValue()).thenReturn(Keyboard.KEY_ESCAPE);
        return event;
    }

    @Test
    void aPressAndReleaseCountsAsEscape() {
        assertFalse(escapeKey.released(escapeDown()));
        assertTrue(escapeKey.released(escapeUp()));
    }

    @Test
    void aReleaseWhosePressWentElsewhereIsIgnored() {
        assertFalse(escapeKey.released(escapeUp()));
    }

    @Test
    void eachPressReleasesOnlyOnce() {
        escapeKey.released(escapeDown());
        escapeKey.released(escapeUp());
        assertFalse(escapeKey.released(escapeUp()));
    }

    @Test
    void aMouseButtonSharingTheEscapeValueIsIgnored() {
        InputEventAPI mouseDown = mock(InputEventAPI.class);
        when(mouseDown.isMouseDownEvent()).thenReturn(true);
        when(mouseDown.getEventValue()).thenReturn(Keyboard.KEY_ESCAPE);
        InputEventAPI mouseUp = mock(InputEventAPI.class);
        when(mouseUp.isMouseUpEvent()).thenReturn(true);
        when(mouseUp.getEventValue()).thenReturn(Keyboard.KEY_ESCAPE);
        assertFalse(escapeKey.released(mouseDown));
        assertFalse(escapeKey.released(mouseUp));
    }
}
