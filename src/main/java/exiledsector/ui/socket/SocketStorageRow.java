package exiledsector.ui.socket;

import exiledsector.i18n.StyledText;
import exiledsector.socketables.EffectThemes;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableDefinition;
import exiledsector.socketables.SocketableKind;
import exiledsector.socketables.SocketableName;
import exiledsector.socketables.SocketableRarity;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

public record SocketStorageRow(Socketable socketable, int order, String name, SocketableRarity rarity, String baseName,
                               SocketableKind kind, String grade, String alignment, Set<String> themes, int effectCount, String searchText, String installedIn,
                               List<StyledText> headerLines, List<StyledText> effectLines) {

    static SocketStorageRow of(Socketable socketable, int order, EffectThemes themes, Function<Socketable, String> installedIn) {
        SocketableDefinition definition = socketable.definition();
        List<StyledText> headerLines = socketable.headerLines();
        List<StyledText> effectLines = socketable.effectLines();
        SocketableName name = socketable.displayName();
        StringBuilder search = new StringBuilder(name.title());
        if (name.baseName() != null) {
            search.append('\n').append(name.baseName());
        }
        for (StyledText line : socketable.tooltipLines()) {
            search.append('\n').append(line.plain());
        }
        return new SocketStorageRow(socketable, order, name.title(), name.rarity(), name.baseName(), socketable.kind(),
                definition == null ? "" : definition.grade(), definition == null ? "" : definition.alignment(),
                themes.of(socketable.effects()), socketable.effects().size(), search.toString().toLowerCase(Locale.ROOT),
                installedIn.apply(socketable), headerLines, effectLines);
    }

    boolean installed() {
        return installedIn != null;
    }
}
