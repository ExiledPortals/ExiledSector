package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipVariantAPI;

import java.util.ArrayList;

final class SkillConflictWarnings {

    static final String TAG_PREFIX = "exiledSector_conflictRemoved|";
    private static final String SEPARATOR = "|";

    private SkillConflictWarnings() {
    }

    static void recordRemoval(ShipVariantAPI variant, String removedHullModId, String causeTypeId) {
        clear(variant);
        variant.addTag(TAG_PREFIX + removedHullModId + SEPARATOR + causeTypeId);
    }

    static void clear(ShipVariantAPI variant) {
        for (String tag : new ArrayList<>(variant.getTags())) {
            if (tag.startsWith(TAG_PREFIX)) {
                variant.removeTag(tag);
            }
        }
    }

    static Removal get(ShipVariantAPI variant) {
        for (String tag : variant.getTags()) {
            if (!tag.startsWith(TAG_PREFIX)) continue;
            String[] parts = tag.substring(TAG_PREFIX.length()).split("\\" + SEPARATOR, 2);
            if (parts.length == 2) {
                return new Removal(parts[0], parts[1]);
            }
        }
        return null;
    }

    record Removal(String removedHullModId, String causeTypeId) {
    }
}
