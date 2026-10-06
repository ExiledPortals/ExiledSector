package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.npc.NpcSkillTreeBuilder;
import exiledsector.skills.npc.NpcTreeTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SkillDataResolver {

    public static final String SHIP_TAG_PREFIX = "exiledSector_ship_";

    private static final String OP_COST_REFRESH_MOD_ID = "exiledSector_opCostRefresh";
    private static final int MAX_CACHED_NPC_TREES = 1024;
    private static final Map<String, ShipSkillData> NPC_TREES = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ShipSkillData> eldest) {
            return size() > MAX_CACHED_NPC_TREES;
        }
    };

    private SkillDataResolver() {
    }

    public static ShipSkillData resolve(FleetMemberAPI member, ShipVariantAPI variant) {
        String npcTag = NpcTreeTag.find(variant);
        if (npcTag != null) {
            return NPC_TREES.computeIfAbsent(npcTag, SkillDataResolver::decodeOrEmpty);
        }
        if (member == null) {
            String shipId = taggedShipId(variant);
            return shipId == null ? null : ShipSkillDataManager.find(shipId);
        }
        ShipSkillData saved = ShipSkillDataManager.find(member.getId());
        return saved != null ? saved : new ShipSkillData();
    }

    public static boolean isNpcTree(ShipVariantAPI variant) {
        return NpcTreeTag.find(variant) != null;
    }

    public static void syncShipTag(FleetMemberAPI member, ShipVariantAPI variant) {
        if (member == null || variant == null) {
            return;
        }
        String wanted = ShipSkillDataManager.find(member.getId()) == null ? null : SHIP_TAG_PREFIX + member.getId();
        List<String> stale = null;
        for (String tag : variant.getTags()) {
            if (tag.startsWith(SHIP_TAG_PREFIX) && !tag.equals(wanted)) {
                if (stale == null) {
                    stale = new ArrayList<>();
                }
                stale.add(tag);
            }
        }
        boolean changed = stale != null;
        if (stale != null) {
            stale.forEach(variant::removeTag);
        }
        if (wanted != null && !variant.hasTag(wanted)) {
            variant.addTag(wanted);
            changed = true;
        }
        if (changed) {
            forgetCachedOpCostStats(variant);
        }
    }

    public static void clearCache() {
        NPC_TREES.clear();
    }

    private static String taggedShipId(ShipVariantAPI variant) {
        if (variant == null) {
            return null;
        }
        for (String tag : variant.getTags()) {
            if (tag.startsWith(SHIP_TAG_PREFIX)) {
                return tag.substring(SHIP_TAG_PREFIX.length());
            }
        }
        return null;
    }

    private static void forgetCachedOpCostStats(ShipVariantAPI variant) {
        if (!variant.hasHullMod(OP_COST_REFRESH_MOD_ID)) {
            variant.addMod(OP_COST_REFRESH_MOD_ID);
            variant.removeMod(OP_COST_REFRESH_MOD_ID);
        }
    }

    private static ShipSkillData decodeOrEmpty(String tag) {
        ShipSkillData data = NpcTreeTag.decode(tag);
        return data != null ? data : NpcSkillTreeBuilder.emptyTree();
    }
}
