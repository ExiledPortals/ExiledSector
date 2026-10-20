package exiledsector.skills.npc;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.ModCsv;
import exiledsector.charts.ChartIds;
import exiledsector.skills.progression.ShipLevelConfig;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class NpcLevelCaps {

    public static final int UNCAPPED = Integer.MAX_VALUE;
    public static final String TAG_PREFIX = "exiledSector_npcLevelCap|";
    static final String DATA_PATH = "data/config/exiledSector/npc_levels.csv";
    private static final String ID_COLUMN = "id";
    private static final String CATEGORY_COLUMN = "category";
    private static final String CAP_COLUMN = "cap";
    private static final Logger LOG = Logger.getLogger(NpcLevelCaps.class);
    private static final AtomicReference<CapData> CAP_DATA = new AtomicReference<>(CapData.defaults());

    private record FactionOverride(NpcLevelCategory category, Integer cap) {
    }

    private record CapData(Map<NpcLevelCategory, Integer> capByCategory, Map<String, FactionOverride> overrideByFaction) {

        static CapData defaults() {
            Map<NpcLevelCategory, Integer> capByCategory = new EnumMap<>(NpcLevelCategory.class);
            for (NpcLevelCategory category : NpcLevelCategory.values()) {
                capByCategory.put(category, category.defaultCap());
            }
            return new CapData(capByCategory, Map.of());
        }
    }

    private NpcLevelCaps() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, DATA_PATH, LOG, NpcLevelCaps::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        CapData loaded = CapData.defaults();
        Map<NpcLevelCategory, Integer> capByCategory = new EnumMap<>(loaded.capByCategory());
        Map<String, FactionOverride> overrideByFaction = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String rowId = ModCsv.text(row, ID_COLUMN);
            String categoryText = ModCsv.text(row, CATEGORY_COLUMN);
            String capText = ModCsv.text(row, CAP_COLUMN);
            if (rowId.isEmpty()) {
                return;
            }
            NpcLevelCategory category = NpcLevelCategory.fromCsvId(categoryText);
            if (!categoryText.isEmpty() && category == null) {
                LOG.warn("Ignoring " + rowId + " in " + DATA_PATH + ": '" + categoryText + "' is not pirate, faction or ai");
                return;
            }
            Integer cap = parseCap(rowId, capText);
            if (!capText.isEmpty() && cap == null) {
                return;
            }
            if (category != null && rowId.equals(category.csvId())) {
                if (cap != null) {
                    capByCategory.put(category, cap);
                }
                return;
            }
            if (category == null && cap == null) {
                LOG.warn("Ignoring " + rowId + " in " + DATA_PATH + ": it sets neither a category nor a cap");
                return;
            }
            overrideByFaction.put(rowId, new FactionOverride(category, cap));
        });
        CAP_DATA.set(new CapData(capByCategory, Map.copyOf(overrideByFaction)));
    }

    private static Integer parseCap(String rowId, String capText) {
        if (capText.isEmpty()) {
            return null;
        }
        try {
            return Math.max(0, Integer.parseInt(capText));
        } catch (NumberFormatException e) {
            LOG.warn("Ignoring " + rowId + " in " + DATA_PATH + ": cap '" + capText + "' is not a whole number");
            return null;
        }
    }

    public static void clear() {
        CAP_DATA.set(CapData.defaults());
    }

    public static int capFor(CampaignFleetAPI fleet) {
        if (fleet == null) {
            return capFor(null, false, false);
        }
        if (isChartFleet(fleet.getMemoryWithoutUpdate())) {
            return UNCAPPED;
        }
        FactionAPI faction = fleet.getFaction();
        PersonAPI commander = fleet.getCommander();
        return capFor(faction == null ? null : faction.getId(),
                faction != null && faction.getCustomBoolean(Factions.CUSTOM_PIRATE_BEHAVIOR),
                commander != null && commander.isAICore());
    }

    public static int capFor(FleetMemberAPI member) {
        FleetDataAPI fleetData = member == null ? null : member.getFleetData();
        return capFor(fleetData == null ? null : fleetData.getFleet());
    }

    public static int capFor(FleetMemberAPI member, ShipVariantAPI variant) {
        String capTag = findTag(variant);
        if (capTag != null) {
            try {
                return Integer.parseInt(capTag.substring(TAG_PREFIX.length()));
            } catch (NumberFormatException e) {
                LOG.warn("Ignoring malformed level cap tag " + capTag);
            }
        }
        return capFor(member);
    }

    public static void tag(ShipVariantAPI variant, int levelCap) {
        String capTag = TAG_PREFIX + levelCap;
        if (capTag.equals(findTag(variant))) {
            return;
        }
        removeTag(variant);
        variant.addTag(capTag);
    }

    public static void removeTag(ShipVariantAPI variant) {
        for (String capTag = findTag(variant); capTag != null; capTag = findTag(variant)) {
            variant.removeTag(capTag);
        }
    }

    private static String findTag(ShipVariantAPI variant) {
        if (variant == null || variant.getTags() == null) {
            return null;
        }
        for (String variantTag : variant.getTags()) {
            if (variantTag.startsWith(TAG_PREFIX)) {
                return variantTag;
            }
        }
        return null;
    }

    public static NpcLevelCategory categoryFor(String factionId, boolean pirateBehavior, boolean aiCommander) {
        FactionOverride override = factionId == null ? null : CAP_DATA.get().overrideByFaction().get(factionId);
        if (override != null && override.category() != null) {
            return override.category();
        }
        if (aiCommander) {
            return NpcLevelCategory.AI;
        }
        return pirateBehavior ? NpcLevelCategory.PIRATE : NpcLevelCategory.FACTION;
    }

    public static int capFor(String factionId, boolean pirateBehavior, boolean aiCommander) {
        FactionOverride override = factionId == null ? null : CAP_DATA.get().overrideByFaction().get(factionId);
        int cap = override != null && override.cap() != null ? override.cap() : capOf(categoryFor(factionId, pirateBehavior, aiCommander));
        return clampToMaxLevel(cap);
    }

    public static int capOf(NpcLevelCategory category) {
        return clampToMaxLevel(CAP_DATA.get().capByCategory().get(category));
    }

    private static int clampToMaxLevel(int cap) {
        return Math.min(cap, Math.min(ShipLevelConfig.maxLevel(), ShipLevelConfig.maxAllocatedNodesBesidesRoot()));
    }

    private static boolean isChartFleet(MemoryAPI fleetMemory) {
        return fleetMemory != null && fleetMemory.get(ChartIds.TIER_MEMORY_KEY) instanceof Number;
    }
}
