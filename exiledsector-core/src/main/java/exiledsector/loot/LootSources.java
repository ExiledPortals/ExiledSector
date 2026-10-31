package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class LootSources {

    private static final DomainContributions<LootSource> SOURCES = new DomainContributions<>(ExiledSectorDomain::lootSources);
    private static final DomainContributions<FleetDifficulty> DIFFICULTIES = DomainContributions.fromEveryDomain(ExiledSectorDomain::fleetDifficulties);

    private LootSources() {
    }

    public static List<LootSource> all() {
        return SOURCES.all();
    }

    public static ItemChances itemChancesFor(CampaignFleetAPI fleet) {
        return SOURCES.firstOr(source -> source.itemChancesFor(fleet), LootModifiers.NONE.itemChances());
    }

    public static LootModifiers modifiersFor(SectorEntityToken site) {
        return SOURCES.firstOr(source -> source.modifiersFor(site), LootModifiers.NONE);
    }

    public static List<ItemData> bossDrops(int chartTier, String bossFactionId, float chanceMult, Random random,
                                             Predicate<ItemDefinition> allowed) {
        List<ItemData> drops = new ArrayList<>();
        SOURCES.all().forEach(source -> drops.addAll(source.bossDrops(chartTier, bossFactionId, chanceMult, random, allowed)));
        return drops;
    }

    public static float difficultyMultiplier(float battleDifficulty) {
        return DIFFICULTIES.firstOr(difficulty -> difficulty.difficultyMultiplier(battleDifficulty), 1f);
    }

    public static boolean shipLevelsRated() {
        return DIFFICULTIES.all().stream().anyMatch(FleetDifficulty::ratesShipLevels);
    }

    public static int shipLevel(FleetMemberAPI member) {
        int level = 0;
        for (FleetDifficulty difficulty : DIFFICULTIES.all()) {
            level = Math.max(level, difficulty.shipLevel(member));
        }
        return level;
    }

    public static FleetLevelRange levelRange(MemoryAPI fleetMemory) {
        return DIFFICULTIES.first(difficulty -> difficulty.levelRange(fleetMemory));
    }

    public static String levelSeed(MemoryAPI fleetMemory) {
        return DIFFICULTIES.first(difficulty -> difficulty.levelSeed(fleetMemory));
    }
}
