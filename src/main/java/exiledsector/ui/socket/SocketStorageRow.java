package exiledsector.ui.socket;

import exiledsector.i18n.StyledText;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableDefinition;
import exiledsector.socketables.SocketableKind;
import exiledsector.socketables.SocketableName;
import exiledsector.socketables.SocketableRarity;

import java.util.Locale;
import java.util.function.Function;

public record SocketStorageRow(Socketable socketable, int order, String name, SocketableRarity rarity, SocketableKind kind,
                               String grade, String alignment, int effectCount, String searchText, String installedIn) {

    static SocketStorageRow of(Socketable socketable, int order, Function<Socketable, String> installedIn) {
        SocketableDefinition definition = socketable.definition();
        SocketableName name = socketable.displayName();
        StringBuilder search = new StringBuilder(name.title());
        if (name.baseName() != null) {
            search.append('\n').append(name.baseName());
        }
        for (StyledText line : socketable.tooltipLines()) {
            search.append('\n').append(line.plain());
        }
        return new SocketStorageRow(socketable, order, name.title(), name.rarity(), socketable.kind(),
                definition == null ? "" : definition.grade(), definition == null ? "" : definition.alignment(),
                socketable.effects().size(), search.toString().toLowerCase(Locale.ROOT), installedIn.apply(socketable));
    }

    boolean installed() {
        return installedIn != null;
    }
}
