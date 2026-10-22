package exiledsector.items;

import exiledlib.items.ItemType;
import exiledlib.items.ModifierScope;
import exiledlib.modifiers.ModifierLine;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ItemDataRules {

    ItemType type();

    default String modifierFile() {
        return null;
    }

    default boolean rollsAffixes() {
        return true;
    }

    default void checkModifierRow(ItemDataIntegrity integrity, String file, int line, String modifierId, Set<ModifierScope> scopes,
                                  List<ModifierLine> lines) {
    }

    default void checkDefinitionRow(ItemDataIntegrity integrity, String file, int line, String definitionId, JSONObject row) {
    }

    default void checkTierGate(ItemDataIntegrity integrity, String file, int line, String definitionId, String entryText, String tierText) {
        integrity.error(file, line, definitionId + " entry \"" + entryText + "\" has a tier range, but only star charts have tiers");
    }

    default void checkModifierFiles(ItemDataIntegrity integrity) throws IOException, JSONException {
    }

    default void checkItemFiles(ItemDataIntegrity integrity) throws IOException, JSONException {
    }

    default List<ItemDataIntegrity.Violation> checkSeparately(Collection<String> definitionIds) throws IOException, JSONException {
        return List.of();
    }
}
