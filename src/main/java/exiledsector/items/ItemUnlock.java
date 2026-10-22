package exiledsector.items;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.intel.misc.GateHaulerIntel;
import exiledsector.compat.RandomAssortmentCompat;

import java.util.function.Predicate;

public enum ItemUnlock {

    FOUND_ONSLAUGHT_MK1("found_onslaught_mk1", sector -> sector.getMemoryWithoutUpdate().getBoolean("$foundOneslaught")),
    FOUND_GATE_HAULER("found_gate_hauler", ItemUnlock::foundGateHauler),
    FOUND_PLANETKILLER("found_planetkiller", sector -> sector.getMemoryWithoutUpdate().getBoolean("$pk_recovered")),
    DEFEATED_ZIGGURAT("defeated_ziggurat", sector -> sector.getMemoryWithoutUpdate().getBoolean("$defeatedZiggurat")),
    RAT_PLAYER_EXOSHIP("rat_player_exoship", sector -> RandomAssortmentCompat.isModEnabled() && RandomAssortmentCompat.exoshipConfirmedRecovered());

    private static final String LATCH_PREFIX = "$exiledSector_socketUnlock_";
    public static final String CLEARED_CHART_TIER_PREFIX = "cleared_chart_tier_";
    static final String CLEARED_CHART_TIER_KEY = "$exiledSector_chartsClearedTier";
    static final int NO_CLEARED_TIER = 0;

    private final String id;
    private final Predicate<SectorAPI> condition;

    ItemUnlock(String id, Predicate<SectorAPI> condition) {
        this.id = id;
        this.condition = condition;
    }

    public String id() {
        return id;
    }

    public static ItemUnlock byId(String id) {
        for (ItemUnlock unlock : values()) {
            if (unlock.id.equals(id)) {
                return unlock;
            }
        }
        return null;
    }

    public static boolean sectionsOpen(SectorAPI sector) {
        MutableCharacterStatsAPI playerStats = sector == null ? null : sector.getPlayerStats();
        return playerStats != null && sectionsOpen(playerStats.getLevel());
    }

    public static boolean sectionsOpen(int playerLevel) {
        return HullUpgradesSetting.enabled() && playerLevel >= HullUpgradesSetting.unlockLevel();
    }

    public static boolean canDrop(ItemDefinition definition, SectorAPI sector) {
        if (!definition.unique()) {
            return true;
        }
        if (sector == null) {
            return false;
        }
        MutableCharacterStatsAPI playerStats = sector.getPlayerStats();
        if (playerStats == null || playerStats.getLevel() < uniqueMinPlayerLevel(definition)) {
            return false;
        }
        return unlockConditionMet(definition, sector);
    }

    static int uniqueMinPlayerLevel(ItemDefinition definition) {
        return definition.type() == ItemType.HULL_ITEM ? HullUpgradesSetting.unlockLevel() : UniqueSubroutinesSetting.unlockLevel();
    }

    public static boolean unlockConditionMet(ItemDefinition definition, SectorAPI sector) {
        if (definition.unlock().isEmpty()) {
            return true;
        }
        int requiredClearedTier = clearedChartTierRequiredBy(definition.unlock());
        if (requiredClearedTier != NO_CLEARED_TIER) {
            return sector != null && highestClearedChartTier(sector) >= requiredClearedTier;
        }
        ItemUnlock unlock = sector == null ? null : byId(definition.unlock());
        return unlock != null && unlock.isMet(sector);
    }

    public static boolean isKnownUnlock(String unlockId) {
        return unlockId == null || unlockId.isEmpty() || byId(unlockId) != null || clearedChartTierRequiredBy(unlockId) != NO_CLEARED_TIER;
    }

    public static boolean canDropChart(ItemDefinition definition, SectorAPI sector) {
        return !definition.unique() || unlockConditionMet(definition, sector);
    }

    static int clearedChartTierRequiredBy(String unlockId) {
        if (unlockId == null || !unlockId.startsWith(CLEARED_CHART_TIER_PREFIX)) {
            return NO_CLEARED_TIER;
        }
        try {
            int tier = Integer.parseInt(unlockId.substring(CLEARED_CHART_TIER_PREFIX.length()));
            return tier >= StarChart.LOWEST_TIER && tier <= StarChart.HIGHEST_TIER ? tier : NO_CLEARED_TIER;
        } catch (NumberFormatException e) {
            return NO_CLEARED_TIER;
        }
    }

    public static int highestClearedChartTier(SectorAPI sector) {
        MemoryAPI sectorMemory = sector == null ? null : sector.getMemoryWithoutUpdate();
        return sectorMemory == null || !sectorMemory.contains(CLEARED_CHART_TIER_KEY) ? NO_CLEARED_TIER : sectorMemory.getInt(CLEARED_CHART_TIER_KEY);
    }

    public static boolean recordChartCleared(SectorAPI sector, int tier) {
        MemoryAPI sectorMemory = sector == null ? null : sector.getMemoryWithoutUpdate();
        int clearedTier = StarChart.clampTier(tier);
        if (sectorMemory == null || highestClearedChartTier(sector) >= clearedTier) {
            return false;
        }
        sectorMemory.set(CLEARED_CHART_TIER_KEY, clearedTier);
        return true;
    }

    boolean isMet(SectorAPI sector) {
        MemoryAPI sectorMemory = sector.getMemoryWithoutUpdate();
        String latchKey = LATCH_PREFIX + id;
        if (sectorMemory.getBoolean(latchKey)) {
            return true;
        }
        if (!condition.test(sector)) {
            return false;
        }
        sectorMemory.set(latchKey, true);
        return true;
    }

    private static boolean foundGateHauler(SectorAPI sector) {
        if (sector.getIntelManager().hasIntelOfClass(GateHaulerIntel.class)) {
            return true;
        }
        for (StarSystemAPI system : sector.getStarSystems()) {
            if (system.getMemoryWithoutUpdate().getBoolean("$deployedGateHaulerHere")) {
                return true;
            }
        }
        return false;
    }
}
