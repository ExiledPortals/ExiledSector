package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;
import exiledlib.items.ItemChances;
import exiledlib.items.ItemData;
import exiledlib.items.ItemDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class LootSources {

    private static final DomainContributions<LootSource> SOURCES = new DomainContributions<>(ExiledSectorDomain::lootSources);
    private static final DomainContributions<FleetDifficulty> DIFFICULTIES = new DomainContributions<>(ExiledSectorDomain::fleetDifficulties);

    private LootSources() {
    }

    public static List<LootSource> all() {
        return SOURCES.all();
    }

    public static ItemChances itemChancesFor(CampaignFleetAPI fleet) {
        for (LootSource source : SOURCES.all()) {
            ItemChances chances = source.itemChancesFor(fleet);
            if (chances != null) {
                return chances;
            }
        }
        return LootModifiers.NONE.itemChances();
    }

    public static LootModifiers modifiersAt(LocationAPI location) {
        for (LootSource source : SOURCES.all()) {
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
        SOURCES.all().forEach(source -> drops.addAll(source.wardenDrops(chartTier, wardenFactionId, chanceMult, random, allowed)));
        return drops;
    }

    public static float difficultyMultiplier(float battleDifficulty) {
        for (FleetDifficulty difficulty : DIFFICULTIES.all()) {
            Float multiplier = difficulty.difficultyMultiplier(battleDifficulty);
            if (multiplier != null) {
                return multiplier;
            }
        }
        return 1f;
    }

    public static int shipLevel(FleetMemberAPI member) {
        int level = 0;
        for (FleetDifficulty difficulty : DIFFICULTIES.all()) {
            level = Math.max(level, difficulty.shipLevel(member));
        }
        return level;
    }

    public static FleetLevelRange levelRange(MemoryAPI fleetMemory) {
        for (FleetDifficulty difficulty : DIFFICULTIES.all()) {
            FleetLevelRange range = difficulty.levelRange(fleetMemory);
            if (range != null) {
                return range;
            }
        }
        return null;
    }

    public static String levelSeed(MemoryAPI fleetMemory) {
        for (FleetDifficulty difficulty : DIFFICULTIES.all()) {
            String seed = difficulty.levelSeed(fleetMemory);
            if (seed != null) {
                return seed;
            }
        }
        return null;
    }
}
