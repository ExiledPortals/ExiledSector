package exiledsector.ui.inspect;

import exiledsector.i18n.StyledText;
import exiledsector.items.ItemKind;

import java.util.List;
import java.util.function.Function;

public record CodexModifierTable(String headingKey, Function<ItemKind, List<Row>> rowsByPart) {

    public record Row(String modifierId, StyledText source) {
    }

    public List<Row> rows(ItemKind part) {
        return part == null ? List.of() : rowsByPart.apply(part);
    }
}
