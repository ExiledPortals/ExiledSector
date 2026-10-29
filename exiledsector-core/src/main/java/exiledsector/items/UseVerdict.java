package exiledsector.items;

import exiledsector.i18n.Message;
import exiledsector.i18n.Translation;

import java.util.HashMap;
import java.util.Map;

public record UseVerdict(String refusalKey, Map<String, String> args) {

    public static final UseVerdict ALLOWED = new UseVerdict(null, Map.of());

    public UseVerdict {
        args = Map.copyOf(args);
    }

    public static UseVerdict refused(String refusalKey) {
        return new UseVerdict(refusalKey, Map.of());
    }

    public UseVerdict with(String name, String value) {
        Map<String, String> widened = new HashMap<>(args);
        widened.put(name, value);
        return new UseVerdict(refusalKey, widened);
    }

    public boolean allowed() {
        return refusalKey == null;
    }

    public String text(String name) {
        return text(name, "");
    }

    public String text(String name, String target) {
        if (allowed()) {
            return null;
        }
        Message message = Translation.msg(refusalKey).arg("name", name).arg("target", target);
        args.forEach(message::arg);
        return message.text();
    }
}
