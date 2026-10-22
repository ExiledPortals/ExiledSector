package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.ExiledSectorModPlugin;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class LootSources {

    private static final List<LootSource> ALL = ExiledSectorModPlugin.DOMAINS.stream()
            .flatMap(domain -> domain.lootSources().stream())
            .toList();
    private static final List<FleetDifficulty> DIFFICULTIES = ExiledSectorModPlugin.DOMAINS.stream()
            .flatMap(domain -> domain.fleetDifficulties().stream())
            .toList();

    private LootSources() {
    }

    public static List<LootSource> all() {
        return ALL;
    }

    public static ItemChances itemChancesFor(CampaignFleetAPI fleet) {
        for (LootSource source : ALL) {
            ItemChances chances = source.itemChancesFor(fleet);
            if (chances != null) {
                return chances;
            }
        }
        return LootModifiers.NONE.itemChances();
    }

    public static LootModifiers modifiersAt(LocationAPI location) {
        for (LootSource source : ALL) {
            LootModifiers modifiers = source.modifiersAt(location);
            if (modifiers != null) {
                return modifiers;
            }
        }
        return LootModifiers.NONE;
    }

    public static List<ItemData> wardenDrops(int chartTier, String wardenFactionId, float chanceMult, Random random,
                                             Predicate<ItemDefinition> allowed) {
        List<ItemData> drops = new ArrayList<>();
        ALL.forEach(source -> drops.addAll(source.wardenDrops(chartTier, wardenFactionId, chanceMult, random, allowed)));
        return drops;
    }

    public static float difficultyMultiplier(float battleDifficulty) {
        for (FleetDifficulty difficulty : DIFFICULTIES) {
            Float multiplier = difficulty.difficultyMultiplier(battleDifficulty);
            if (multiplier != null) {
                return multiplier;
            }
        }
        return 1f;
    }

    public static int shipLevel(FleetMemberAPI member) {
        int level = 0;
        for (FleetDifficulty difficulty : DIFFICULTIES) {
            level = Math.max(level, difficulty.shipLevel(member));
        }
        return level;
    }

    public static FleetLevelRange levelRange(MemoryAPI fleetMemory) {
        for (FleetDifficulty difficulty : DIFFICULTIES) {
            FleetLevelRange range = difficulty.levelRange(fleetMemory);
            if (range != null) {
                return range;
            }
        }
        return null;
    }

    public static String levelSeed(MemoryAPI fleetMemory) {
        for (FleetDifficulty difficulty : DIFFICULTIES) {
            String seed = difficulty.levelSeed(fleetMemory);
            if (seed != null) {
                return seed;
            }
        }
        return null;
    }
}
