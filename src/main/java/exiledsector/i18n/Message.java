package exiledsector.i18n;

import java.util.HashMap;
import java.util.Map;

public final class Message {

    private final String messageKey;
    private final Map<String, StyledText> args = new HashMap<>();
    private Integer pluralCount;

    Message(String messageKey) {
        this.messageKey = messageKey;
    }

    public Message arg(String name, String literal) {
        args.put(name, StyledText.of(literal));
        return this;
    }

    public Message arg(String name, float number) {
        return arg(name, NumberText.format(number));
    }

    public Message arg(String name, int number) {
        return arg(name, String.valueOf(number));
    }

    public Message arg(String name, StyledText value) {
        args.put(name, value);
        return this;
    }

    public Message count(int count) {
        this.pluralCount = count;
        return arg("count", count);
    }

    public StyledText styled() {
        Catalogue catalogue = I18n.catalogue();
        return catalogue.template(resolvedKey(catalogue)).render(args);
    }

    public String text() {
        return styled().plain();
    }

    private String resolvedKey(Catalogue catalogue) {
        if (pluralCount == null) {
            return messageKey;
        }
        String categoryKey = messageKey + "." + Plurals.category(catalogue.locale(), pluralCount);
        if (catalogue.has(categoryKey)) {
            return categoryKey;
        }
        String otherKey = messageKey + "." + Plurals.OTHER;
        return catalogue.has(otherKey) ? otherKey : messageKey;
    }
}
