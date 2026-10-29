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
import exiledsector.loot.LootSources;
import exiledsector.skills.progression.ShipLevelConfig;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class NpcLevelCaps {

    public static final int UNCAPPED = Integer.MAX_VALUE;
    public static final int BOUNTY_TOP_CAP = 25;
    public static final int BOUNTY_FLAGSHIP_LEVELS = 3;
    public static final String TAG_PREFIX = "exiledSector_npcLevelCap|";
    public static final String RECOVERY_TAG_PREFIX = "exiledSector_npcTreeRecovery|";
    static final String DATA_PATH = "data/config/exiledSector/npc_levels.csv";
    private static final String ID_COLUMN = "id";
    private static final String CATEGORY_COLUMN = "category";
    private static final String CAP_COLUMN = "cap";
    private static final String MIN_COLUMN = "min";
    private static final String MODE_COLUMN = "mode";
    private static final String KEEP_TREE_COLUMN = "keep_tree";
    private static final String MODE_FIXED = "fixed";
    private static final String MODE_PLAYER = "player";
    private static final String KEEP = "keep";
    private static final String WIPE = "wipe";
    private static final Logger LOG = Logger.getLogger(NpcLevelCaps.class);
    private static final AtomicReference<CapData> CAP_DATA = new AtomicReference<>(CapData.defaults());

    private record LevelRule(Integer cap, Integer min, Boolean fixed, Boolean keepTree) {

        static final LevelRule EMPTY = new LevelRule(null, null, null, null);

        boolean isEmpty() {
            return equals(EMPTY);
        }

        LevelRule over(LevelRule base) {
            return new LevelRule(cap != null ? cap : base.cap(), min != null ? min : base.min(), fixed != null ? fixed : base.fixed(),
                    keepTree != null ? keepTree : base.keepTree());
        }
    }

    private record FactionOverride(NpcLevelCategory category, LevelRule rule) {
    }

    private record CapData(Map<NpcLevelCategory, LevelRule> ruleByCategory, Map<String, FactionOverride> overrideByFaction) {

        static CapData defaults() {
            Map<NpcLevelCategory, LevelRule> ruleByCategory = new EnumMap<>(NpcLevelCategory.class);
            for (NpcLevelCategory category : NpcLevelCategory.values()) {
                ruleByCategory.put(category, new LevelRule(category.defaultCap(), null, null, null));
            }
            return new CapData(ruleByCategory, Map.of());
        }
    }

    private NpcLevelCaps() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, DATA_PATH, LOG, NpcLevelCaps::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<NpcLevelCategory, LevelRule> ruleByCategory = new EnumMap<>(CapData.defaults().ruleByCategory());
        Map<String, FactionOverride> overrideByFaction = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String rowId = ModCsv.text(row, ID_COLUMN);
            String categoryText = ModCsv.text(row, CATEGORY_COLUMN);
            if (rowId.isEmpty()) {
                return;
            }
            NpcLevelCategory category = NpcLevelCategory.fromCsvId(categoryText);
            if (!categoryText.isEmpty() && category == null) {
                LOG.warn("Ignoring " + rowId + " in " + DATA_PATH + ": '" + categoryText + "' is not pirate, faction or ai");
                return;
            }
            LevelRule rule;
            try {
                rule = new LevelRule(parseLevel(ModCsv.text(row, CAP_COLUMN), CAP_COLUMN), parseLevel(ModCsv.text(row, MIN_COLUMN), MIN_COLUMN),
                        parseChoice(ModCsv.text(row, MODE_COLUMN), MODE_COLUMN, MODE_FIXED, MODE_PLAYER),
                        parseChoice(ModCsv.text(row, KEEP_TREE_COLUMN), KEEP_TREE_COLUMN, KEEP, WIPE));
            } catch (IllegalArgumentException e) {
                LOG.warn("Ignoring " + rowId + " in " + DATA_PATH + ": " + e.getMessage());
                return;
            }
            if (category != null && rowId.equals(category.csvId())) {
                ruleByCategory.put(category, rule.over(ruleByCategory.get(category)));
                return;
            }
            if (category == null && rule.isEmpty()) {
                LOG.warn("Ignoring " + rowId + " in " + DATA_PATH + ": it sets no category, cap, min, mode or keep_tree");
                return;
            }
            overrideByFaction.put(rowId, new FactionOverride(category, rule));
        });
        CAP_DATA.set(new CapData(ruleByCategory, Map.copyOf(overrideByFaction)));
    }

    private static Integer parseLevel(String text, String column) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Math.max(0, Integer.parseInt(text));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(column + " '" + text + "' is not a whole number", e);
        }
    }

    private static Boolean parseChoice(String text, String column, String yes, String no) {
        if (text.isEmpty()) {
            return null;
        }
        if (yes.equalsIgnoreCase(text)) {
            return true;
        }
        if (no.equalsIgnoreCase(text)) {
            return false;
        }
        throw new IllegalArgumentException(column + " '" + text + "' is not " + yes + " or " + no);
    }

    public static void clear() {
        CAP_DATA.set(CapData.defaults());
    }

    public static NpcLevelLimit limitFor(CampaignFleetAPI fleet) {
        if (fleet == null) {
            return withMode(null, NpcLevelCategory.FACTION, NpcLevelLimit.of(capFor(null, false, false)));
        }
        if (isChartFleet(fleet.getMemoryWithoutUpdate())) {
            return NpcLevelLimit.UNCAPPED;
        }
        String factionId = factionIdOf(fleet);
        NpcBounties.Bounty bounty = NpcBounties.find(fleet);
        NpcLevelCategory category = categoryFor(fleet, bounty);
        int categoryCap = capFor(factionId, category);
        return withMode(factionId, category, bounty == null ? NpcLevelLimit.of(categoryCap) : bountyLimit(categoryCap, bounty.difficulty()));
    }

    private static String factionIdOf(CampaignFleetAPI fleet) {
        FactionAPI faction = fleet.getFaction();
        return faction == null ? null : faction.getId();
    }

    private static NpcLevelCategory categoryFor(CampaignFleetAPI fleet, NpcBounties.Bounty bounty) {
        FactionAPI faction = fleet.getFaction();
        PersonAPI commander = fleet.getCommander();
        return categoryFor(factionIdOf(fleet),
                (bounty != null && bounty.personBounty()) || (faction != null && faction.getCustomBoolean(Factions.CUSTOM_PIRATE_BEHAVIOR)),
                commander != null && commander.isAICore());
    }

    private static NpcLevelLimit withMode(String factionId, NpcLevelCategory category, NpcLevelLimit limit) {
        Boolean fixed = resolve(factionId, category, LevelRule::fixed);
        if (fixed == null ? !NpcTreeConfig.fixedLevels() : !fixed) {
            return limit;
        }
        Integer min = resolve(factionId, category, LevelRule::min);
        return limit.fixedFrom(min != null ? min : Math.round(capFor(factionId, category) * NpcTreeConfig.fixedMinPercent() / 100f));
    }

    public static Boolean keepTreeSetting(CampaignFleetAPI fleet) {
        if (fleet == null) {
            return null;
        }
        return resolve(factionIdOf(fleet), categoryFor(fleet, NpcBounties.find(fleet)), LevelRule::keepTree);
    }

    public static int capFor(CampaignFleetAPI fleet) {
        return limitFor(fleet).cap();
    }

    public static int capFor(FleetMemberAPI member) {
        FleetDataAPI fleetData = member == null ? null : member.getFleetData();
        return limitFor(fleetData == null ? null : fleetData.getFleet()).capFor(member != null && member.isFlagship());
    }

    static NpcLevelLimit bountyLimit(int categoryCap, float bountyDifficulty) {
        int bountyTopCap = clampToMaxLevel(BOUNTY_TOP_CAP);
        int bountyCap = categoryCap >= bountyTopCap ? categoryCap
                : Math.round(categoryCap + (bountyTopCap - categoryCap) * Math.max(0f, Math.min(1f, bountyDifficulty)));
        return new NpcLevelLimit(bountyCap, clampToMaxLevel(bountyCap + BOUNTY_FLAGSHIP_LEVELS), BOUNTY_FLAGSHIP_LEVELS);
    }

    public static int capFor(FleetMemberAPI member, ShipVariantAPI variant) {
        String capTag = findTag(variant, TAG_PREFIX);
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
        replaceTag(variant, TAG_PREFIX, TAG_PREFIX + levelCap);
    }

    public static void tagRecovery(ShipVariantAPI variant, Boolean keepTree) {
        replaceTag(variant, RECOVERY_TAG_PREFIX, keepTree == null ? null : RECOVERY_TAG_PREFIX + (keepTree ? KEEP : WIPE));
    }

    public static boolean keepsTreeOnRecovery(ShipVariantAPI variant) {
        String recoveryTag = findTag(variant, RECOVERY_TAG_PREFIX);
        if (recoveryTag == null) {
            return !NpcTreeConfig.recoveredShipsStartBlank();
        }
        return !WIPE.equals(recoveryTag.substring(RECOVERY_TAG_PREFIX.length()));
    }

    public static boolean hasTag(ShipVariantAPI variant) {
        return findTag(variant, TAG_PREFIX) != null;
    }

    public static void removeTag(ShipVariantAPI variant) {
        removeTags(variant, TAG_PREFIX);
        removeTags(variant, RECOVERY_TAG_PREFIX);
    }

    private static void replaceTag(ShipVariantAPI variant, String prefix, String wantedTag) {
        if (wantedTag != null && wantedTag.equals(findTag(variant, prefix))) {
            return;
        }
        removeTags(variant, prefix);
        if (wantedTag != null) {
            variant.addTag(wantedTag);
        }
    }

    private static void removeTags(ShipVariantAPI variant, String prefix) {
        for (String foundTag = findTag(variant, prefix); foundTag != null; foundTag = findTag(variant, prefix)) {
            variant.removeTag(foundTag);
        }
    }

    private static String findTag(ShipVariantAPI variant, String prefix) {
        if (variant == null || variant.getTags() == null) {
            return null;
        }
        for (String variantTag : variant.getTags()) {
            if (variantTag.startsWith(prefix)) {
                return variantTag;
            }
        }
        return null;
    }

    public static NpcLevelCategory categoryFor(String factionId, boolean pirateBehavior, boolean aiCommander) {
        FactionOverride override = overrideFor(factionId);
        if (override != null && override.category() != null) {
            return override.category();
        }
        if (aiCommander) {
            return NpcLevelCategory.AI;
        }
        return pirateBehavior ? NpcLevelCategory.PIRATE : NpcLevelCategory.FACTION;
    }

    public static int capFor(String factionId, boolean pirateBehavior, boolean aiCommander) {
        return capFor(factionId, categoryFor(factionId, pirateBehavior, aiCommander));
    }

    private static int capFor(String factionId, NpcLevelCategory category) {
        return clampToMaxLevel(resolve(factionId, category, LevelRule::cap));
    }

    public static int capOf(NpcLevelCategory category) {
        return clampToMaxLevel(CAP_DATA.get().ruleByCategory().get(category).cap());
    }

    private static <T> T resolve(String factionId, NpcLevelCategory category, Function<LevelRule, T> field) {
        FactionOverride override = overrideFor(factionId);
        T factionValue = override == null ? null : field.apply(override.rule());
        return factionValue != null ? factionValue : field.apply(CAP_DATA.get().ruleByCategory().get(category));
    }

    private static FactionOverride overrideFor(String factionId) {
        return factionId == null ? null : CAP_DATA.get().overrideByFaction().get(factionId);
    }

    private static int clampToMaxLevel(int cap) {
        return Math.min(cap, Math.min(ShipLevelConfig.maxLevel(), ShipLevelConfig.maxAllocatedNodesBesidesRoot()));
    }

    private static boolean isChartFleet(MemoryAPI fleetMemory) {
        return LootSources.levelRange(fleetMemory) != null;
    }
}
