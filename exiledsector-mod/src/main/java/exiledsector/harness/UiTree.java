package exiledsector.harness;

import com.fs.starfarer.api.ui.ButtonAPI;
import exiledsector.ui.refit.UiReflection;

import java.util.ArrayList;
import java.util.List;

final class UiTree {

    interface Match {
        boolean matches(Object component) throws Throwable;
    }

    private UiTree() {
    }

    static Object find(Object root, Match match) throws Throwable {
        for (Object child : UiReflection.children(root)) {
            if (match.matches(child)) {
                return child;
            }
            Object found = find(child, match);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    static <T> T find(Object root, Class<T> type) throws Throwable {
        return type.cast(find(root, type::isInstance));
    }

    static ButtonAPI button(Object root, String text) throws Throwable {
        return (ButtonAPI) find(root, component -> component instanceof ButtonAPI button && text.equalsIgnoreCase(button.getText()));
    }

    static List<String> buttonTexts(Object root) throws Throwable {
        List<String> texts = new ArrayList<>();
        find(root, component -> {
            if (component instanceof ButtonAPI button && button.getText() != null && !button.getText().isBlank()) {
                texts.add(button.getText());
            }
            return false;
        });
        return texts;
    }

    static void press(ButtonAPI button) throws Throwable {
        Object listener = UiReflection.call(button, "getListener");
        if (listener == null) {
            throw new IllegalStateException("the \"" + button.getText() + "\" button has no listener");
        }
        UiReflection.call(listener, "actionPerformed", null, button);
    }
}
