package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.ui.node.NodeSearch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;

import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillTreeSearchBarTest {

    private static final ToDoubleFunction<String> TEN_PIXELS_PER_CHARACTER = text -> text.codePointCount(0, text.length()) * 10.0;

    private NodeSearch search;
    private SkillTreeSearchBar bar;
    private PositionAPI position;
    private int submits;

    @BeforeEach
    void setUp() {
        search = new NodeSearch();
        bar = new SkillTreeSearchBar(search, () -> submits++);
        position = mock(PositionAPI.class);
        when(position.getX()).thenReturn(0f);
        when(position.getY()).thenReturn(0f);
        when(position.getWidth()).thenReturn(1000f);
        when(position.getHeight()).thenReturn(800f);
    }

    private static InputEventAPI keyDown(int keyCode, char character) {
        InputEventAPI event = mock(InputEventAPI.class);
        when(event.isKeyDownEvent()).thenReturn(true);
        when(event.getEventValue()).thenReturn(keyCode);
        when(event.getEventChar()).thenReturn(character);
        return event;
    }

    private void focus() {
        assertTrue(bar.handleClick(position, 500f, 760f));
    }

    private void type(String text) {
        for (char c : text.toCharArray()) {
            assertTrue(bar.handleKey(keyDown(Keyboard.KEY_NONE, c)));
        }
    }

    @Test
    void clickingTheBarFocusesItAndClickingElsewhereUnfocusesIt() {
        focus();
        type("a");
        assertFalse(bar.handleClick(position, 500f, 400f));

        assertFalse(bar.handleKey(keyDown(Keyboard.KEY_NONE, 'b')));
        assertEquals("a", search.getQuery());
    }

    @Test
    void keysAreIgnoredUntilTheBarIsFocused() {
        assertFalse(bar.handleKey(keyDown(Keyboard.KEY_NONE, 'a')));

        assertEquals("", search.getQuery());
    }

    @Test
    void typingAppendsPrintableCharactersIncludingHanziAndSkipsControlCharacters() {
        focus();

        type("arm\u0001装");

        assertEquals("arm装", search.getQuery());
    }

    @Test
    void backspaceRemovesTheLastCharacterAndIsHarmlessOnAnEmptyQuery() {
        focus();
        type("ab");

        bar.handleKey(keyDown(Keyboard.KEY_BACK, '\b'));
        assertEquals("a", search.getQuery());
        bar.handleKey(keyDown(Keyboard.KEY_BACK, '\b'));
        bar.handleKey(keyDown(Keyboard.KEY_BACK, '\b'));
        assertEquals("", search.getQuery());
    }

    @Test
    void escapeClearsTheQueryAndReleasesFocus() {
        focus();
        type("armor");

        bar.handleKey(keyDown(Keyboard.KEY_ESCAPE, '\u001b'));

        assertEquals("", search.getQuery());
        assertFalse(bar.handleKey(keyDown(Keyboard.KEY_NONE, 'a')));
    }

    @Test
    void enterSubmitsEachPressAndKeepsTheQueryAndFocus() {
        focus();
        search.setQuery("armor");

        bar.handleKey(keyDown(Keyboard.KEY_RETURN, '\r'));
        bar.handleKey(keyDown(Keyboard.KEY_NUMPADENTER, '\r'));

        assertEquals(2, submits);
        assertEquals("armor", search.getQuery());
        assertTrue(bar.handleKey(keyDown(Keyboard.KEY_NONE, 's')));
        assertEquals("armors", search.getQuery());
    }

    @Test
    void theQueryStopsGrowingAtTheMaximumLength() {
        focus();

        type("x".repeat(SkillTreeSearchBar.MAX_QUERY_LENGTH + 5));

        assertEquals(SkillTreeSearchBar.MAX_QUERY_LENGTH, search.getQuery().length());
    }

    @Test
    void keyUpEventsAreConsumedWhileFocusedButChangeNothing() {
        focus();
        InputEventAPI keyUp = mock(InputEventAPI.class);
        when(keyUp.getEventChar()).thenReturn('a');

        assertTrue(bar.handleKey(keyUp));
        assertEquals("", search.getQuery());
    }

    @Test
    void textThatFitsIsShownWhole() {
        assertEquals("armor", SkillTreeSearchBar.fitEnd("armor", TEN_PIXELS_PER_CHARACTER));
        assertEquals("armor", SkillTreeSearchBar.fitStart("armor", TEN_PIXELS_PER_CHARACTER));
    }

    @Test
    void aLongQueryKeepsItsEndSoTheLatestTypingStaysVisible() {
        String query = "abcdefghijklmnopqrstuvwxyz0123456789";

        assertEquals("fghijklmnopqrstuvwxyz0123456789", SkillTreeSearchBar.fitEnd(query, TEN_PIXELS_PER_CHARACTER));
    }

    @Test
    void aLongPlaceholderKeepsItsStart() {
        String placeholder = "abcdefghijklmnopqrstuvwxyz0123456789";

        assertEquals("abcdefghijklmnopqrstuvwxyz01234", SkillTreeSearchBar.fitStart(placeholder, TEN_PIXELS_PER_CHARACTER));
    }

    @Test
    void trimmingNeverSplitsACharacter() {
        String hanzi = "装甲".repeat(20);

        assertEquals("甲" + "装甲".repeat(15), SkillTreeSearchBar.fitEnd(hanzi, TEN_PIXELS_PER_CHARACTER));
    }
}
