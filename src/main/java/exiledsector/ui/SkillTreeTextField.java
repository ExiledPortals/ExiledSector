package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import exiledsector.ui.util.ReusableText;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.function.ToDoubleFunction;

final class SkillTreeTextField {

    enum KeyResult {
        IGNORED, CONSUMED, EDITED, SUBMIT, CANCEL
    }

    private static final float CARET_BLINK_SECONDS = 0.5f;
    private static final String CARET = "|";
    private static final Color PLACEHOLDER_COLOR = new Color(130, 130, 130);

    private final int maxLength;
    private final float fontSize;
    private final Color textColor;
    private final ReusableText displayLine;

    private String enteredText = "";
    private boolean focused;
    private float caretSeconds;
    private String fittedText;
    private String fittedPlaceholder;
    private boolean fittedFocused;
    private float fittedWidth;
    private String fittedWithCaret;
    private String fittedWithoutCaret;

    SkillTreeTextField(int maxLength, float fontSize, Color textColor) {
        this.maxLength = maxLength;
        this.fontSize = fontSize;
        this.textColor = textColor;
        this.displayLine = new ReusableText(fontSize, textColor);
    }

    String text() {
        return enteredText;
    }

    void setText(String value) {
        enteredText = value == null ? "" : value;
    }

    void focus(boolean value) {
        focused = value;
        caretSeconds = 0f;
    }

    KeyResult handleKey(InputEventAPI event) {
        if (!focused) {
            return KeyResult.IGNORED;
        }
        if (!event.isKeyDownEvent()) {
            return KeyResult.CONSUMED;
        }
        caretSeconds = 0f;
        int keyCode = event.getEventValue();
        if (keyCode == Keyboard.KEY_BACK) {
            if (enteredText.isEmpty()) {
                return KeyResult.CONSUMED;
            }
            enteredText = withoutLastCodePoint(enteredText);
            return KeyResult.EDITED;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            return KeyResult.CANCEL;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            return KeyResult.SUBMIT;
        }
        char character = event.getEventChar();
        if (!Character.isISOControl(character) && enteredText.length() < maxLength) {
            enteredText = enteredText + character;
            return KeyResult.EDITED;
        }
        return KeyResult.CONSUMED;
    }

    void advance(float amount) {
        caretSeconds += amount;
    }

    void render(float x, float bottom, float width, float height, String placeholder, float alphaMult) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) {
            return;
        }
        displayLine.set(displayText(font, width, placeholder));
        displayLine.setColor(enteredText.isEmpty() && !focused ? PLACEHOLDER_COLOR : textColor);
        displayLine.setAlpha(alphaMult);
        displayLine.draw(x, bottom + height / 2f + fontSize / 2f);
    }

    private String displayText(LazyFont font, float width, String placeholder) {
        boolean stale = !enteredText.equals(fittedText) || focused != fittedFocused || width != fittedWidth
                || !placeholder.equals(fittedPlaceholder);
        if (stale) {
            refit(width, placeholder, value -> font.calcWidth(value, fontSize));
        }
        boolean caretVisible = focused && ((int) (caretSeconds / CARET_BLINK_SECONDS)) % 2 == 0;
        return caretVisible ? fittedWithCaret : fittedWithoutCaret;
    }

    private void refit(float width, String placeholder, ToDoubleFunction<String> widthOf) {
        fittedText = enteredText;
        fittedFocused = focused;
        fittedWidth = width;
        fittedPlaceholder = placeholder;
        if (!focused) {
            fittedWithoutCaret = enteredText.isEmpty() ? fitStart(placeholder, width, widthOf) : fitEnd(enteredText, width, widthOf);
            fittedWithCaret = fittedWithoutCaret;
            return;
        }
        fittedWithCaret = fitEnd(enteredText + CARET, width, widthOf);
        fittedWithoutCaret = fittedWithCaret.endsWith(CARET)
                ? fittedWithCaret.substring(0, fittedWithCaret.length() - CARET.length()) : fittedWithCaret;
    }

    static String fitEnd(String value, double maxWidth, ToDoubleFunction<String> widthOf) {
        String fitted = value;
        while (!fitted.isEmpty() && widthOf.applyAsDouble(fitted) > maxWidth) {
            fitted = fitted.substring(Character.charCount(fitted.codePointAt(0)));
        }
        return fitted;
    }

    static String fitStart(String value, double maxWidth, ToDoubleFunction<String> widthOf) {
        String fitted = value;
        while (!fitted.isEmpty() && widthOf.applyAsDouble(fitted) > maxWidth) {
            fitted = withoutLastCodePoint(fitted);
        }
        return fitted;
    }

    private static String withoutLastCodePoint(String value) {
        return value.substring(0, value.length() - Character.charCount(value.codePointBefore(value.length())));
    }
}
