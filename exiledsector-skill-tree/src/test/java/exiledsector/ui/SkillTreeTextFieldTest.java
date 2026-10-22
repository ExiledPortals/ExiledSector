package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillTreeTextFieldTest {

    private static InputEventAPI keyDown(int keyCode, char character) {
        InputEventAPI event = mock(InputEventAPI.class);
        when(event.isKeyDownEvent()).thenReturn(true);
        when(event.getEventValue()).thenReturn(keyCode);
        when(event.getEventChar()).thenReturn(character);
        return event;
    }

    private static SkillTreeTextField focusedField() {
        SkillTreeTextField field = new SkillTreeTextField(5, 20f, Color.WHITE);
        field.focus(true);
        return field;
    }

    @Test
    void anUnfocusedFieldIgnoresKeysAndAFocusedOneConsumesKeyUps() {
        SkillTreeTextField field = new SkillTreeTextField(5, 20f, Color.WHITE);
        assertEquals(SkillTreeTextField.KeyResult.IGNORED, field.handleKey(keyDown(Keyboard.KEY_NONE, 'a')));

        field.focus(true);
        assertEquals(SkillTreeTextField.KeyResult.CONSUMED, field.handleKey(mock(InputEventAPI.class)));
    }

    @Test
    void typingEditsUpToTheLimitAndControlCharactersAreIgnored() {
        SkillTreeTextField field = focusedField();

        assertEquals(SkillTreeTextField.KeyResult.EDITED, field.handleKey(keyDown(Keyboard.KEY_NONE, '装')));
        assertEquals(SkillTreeTextField.KeyResult.CONSUMED, field.handleKey(keyDown(Keyboard.KEY_NONE, '\u0001')));
        for (char c : "abcdef".toCharArray()) {
            field.handleKey(keyDown(Keyboard.KEY_NONE, c));
        }

        assertEquals("装abcd", field.text());
    }

    @Test
    void backspaceRemovesAWholeCharacterIncludingSurrogatePairs() {
        SkillTreeTextField field = focusedField();
        field.setText("a🚀");

        assertEquals(SkillTreeTextField.KeyResult.EDITED, field.handleKey(keyDown(Keyboard.KEY_BACK, '\b')));
        assertEquals("a", field.text());
        field.handleKey(keyDown(Keyboard.KEY_BACK, '\b'));
        assertEquals(SkillTreeTextField.KeyResult.CONSUMED, field.handleKey(keyDown(Keyboard.KEY_BACK, '\b')));
    }

    private static InputEventAPI keyRepeat(int keyCode, char character) {
        InputEventAPI event = mock(InputEventAPI.class);
        when(event.isRepeat()).thenReturn(true);
        when(event.getEventValue()).thenReturn(keyCode);
        when(event.getEventChar()).thenReturn(character);
        return event;
    }

    @Test
    void heldKeysRepeatEditsButNeverRepeatSubmitOrCancel() {
        SkillTreeTextField field = focusedField();
        field.setText("abc");

        assertEquals(SkillTreeTextField.KeyResult.EDITED, field.handleKey(keyRepeat(Keyboard.KEY_BACK, '\b')));
        assertEquals(SkillTreeTextField.KeyResult.EDITED, field.handleKey(keyRepeat(Keyboard.KEY_BACK, '\b')));
        assertEquals(SkillTreeTextField.KeyResult.EDITED, field.handleKey(keyRepeat(Keyboard.KEY_NONE, 'z')));
        assertEquals(SkillTreeTextField.KeyResult.CONSUMED, field.handleKey(keyRepeat(Keyboard.KEY_RETURN, '\r')));
        assertEquals(SkillTreeTextField.KeyResult.CONSUMED, field.handleKey(keyRepeat(Keyboard.KEY_ESCAPE, '\u001b')));
        assertEquals("az", field.text());
    }

    @Test
    void enterSubmitsAndEscapeCancelsWithoutChangingTheText() {
        SkillTreeTextField field = focusedField();
        field.setText("Line");

        assertEquals(SkillTreeTextField.KeyResult.SUBMIT, field.handleKey(keyDown(Keyboard.KEY_RETURN, '\r')));
        assertEquals(SkillTreeTextField.KeyResult.SUBMIT, field.handleKey(keyDown(Keyboard.KEY_NUMPADENTER, '\r')));
        assertEquals(SkillTreeTextField.KeyResult.CANCEL, field.handleKey(keyDown(Keyboard.KEY_ESCAPE, '\u001b')));
        assertEquals("Line", field.text());
    }

    @Test
    void fittingTrimsWholeCharactersFromTheRequestedEnd() {
        assertEquals("cdef", SkillTreeTextField.fitEnd("abcdef", 40, text -> text.length() * 10.0));
        assertEquals("abcd", SkillTreeTextField.fitStart("abcdef", 40, text -> text.length() * 10.0));
    }
}
