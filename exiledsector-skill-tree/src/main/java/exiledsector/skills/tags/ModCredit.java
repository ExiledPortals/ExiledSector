package exiledsector.skills.tags;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum ModCredit {
    LOST_SECTOR(VolumeToggles.LOST_SECTOR_VOLUMES, "node.modCredit.lostSector"),
    RANDOM_ASSORTMENT(VolumeToggles.RANDOM_ASSORTMENT_VOLUMES, "node.modCredit.randomAssortment"),
    NIGHTCROSS(VolumeToggles.NIGHTCROSS_VOLUMES, "node.modCredit.nightcross"),
    TANIA(VolumeToggles.TANIA_VOLUMES, "node.modCredit.tania"),
    MORE_HULLMODS("mhmods_", "node.modCredit.moreHullmods"),
    EMERGENT_THREATS("vice_", "node.modCredit.emergentThreats");

    private final Set<String> volumes;
    private final String hullModIdPrefix;
    private final String lineKey;

    ModCredit(Set<String> volumes, String lineKey) {
        this.volumes = volumes;
        this.hullModIdPrefix = null;
        this.lineKey = lineKey;
    }

    ModCredit(String hullModIdPrefix, String lineKey) {
        this.volumes = Set.of();
        this.hullModIdPrefix = hullModIdPrefix;
        this.lineKey = lineKey;
    }

    public StyledText line() {
        return Translation.styled(lineKey);
    }

    public static Map<String, ModCredit> byType(Collection<SkillNode> nodes) {
        Map<String, ModCredit> credits = new HashMap<>();
        Set<String> uncredited = new HashSet<>();
        for (SkillNode node : nodes) {
            ModCredit hullModCredit = forHullMods(node.getType());
            ModCredit credit = hullModCredit != null ? hullModCredit : forVolume(node.getVolume());
            for (String typeId : typeIds(node.getType())) {
                if (uncredited.contains(typeId)) {
                    continue;
                }
                ModCredit earlierCredit = credits.get(typeId);
                if (credit == null || earlierCredit != null && earlierCredit != credit) {
                    credits.remove(typeId);
                    uncredited.add(typeId);
                } else {
                    credits.put(typeId, credit);
                }
            }
        }
        return Map.copyOf(credits);
    }

    static ModCredit forVolume(String volume) {
        if (volume == null) {
            return null;
        }
        for (ModCredit credit : values()) {
            if (credit.volumes.contains(volume)) {
                return credit;
            }
        }
        return null;
    }

    static ModCredit forHullMods(SkillType type) {
        if (type == null) {
            return null;
        }
        for (String hullModId : type.getOwnHullModIds()) {
            for (ModCredit credit : values()) {
                if (credit.hullModIdPrefix != null && hullModId.regionMatches(true, 0, credit.hullModIdPrefix, 0, credit.hullModIdPrefix.length())) {
                    return credit;
                }
            }
        }
        return null;
    }

    private static List<String> typeIds(SkillType type) {
        if (type == null || type.getTier() == SkillTier.SMALL) {
            return List.of();
        }
        List<String> typeIds = new ArrayList<>(type.getChoiceOptionIds().size() + 1);
        typeIds.add(type.getId());
        typeIds.addAll(type.getChoiceOptionIds());
        return typeIds;
    }
}
