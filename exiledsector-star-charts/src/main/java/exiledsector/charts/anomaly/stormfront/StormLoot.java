package exiledsector.charts.anomaly.stormfront;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.Seeds;
import exiledsector.charts.anomaly.FlareLuck;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.charts.loot.ImmutableDrops;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.modifier.AffixLayout;
import exiledsector.items.modifier.GrantedModifiers;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class StormLoot {

    private static final long GRANT_SEED_SALT = 0x73746f726d6c6fL;

    private StormLoot() {
    }

    public static ItemData shapedDrop(SectorAPI sector, ItemData item, LocationAPI location) {
        ItemData withBiome = withBiomeModifier(sector, item, location);
        StormFrontTerrainPlugin front = StormBattles.playerFront();
        if (front == null) {
            return withBiome;
        }
        ChartRun run = StormBattles.runOf(front);
        float immutableChancePercent = front.captured() ? StormTuning.of(run).capturedImmutableChancePercent() : 0f;
        return shapedByFront(withBiome, run, front.solar(), immutableChancePercent);
    }

    static ItemData shapedByFront(ItemData item, ChartRun run, boolean solar, float immutableChancePercent) {
        ItemData shaped = solar ? FlareLuck.withLuckyModifiers(item, run, rolled -> BiomeModifiers.isBiomeModifier(rolled.modifierId())) : item;
        return immutableChancePercent > 0f ? ImmutableDrops.withChance(shaped, immutableChancePercent / 100f) : shaped;
    }

    public static ItemData withBiomeModifier(SectorAPI sector, ItemData item, LocationAPI location) {
        ChartRun run = ChartRuns.liveIn(location);
        if (run == null) {
            return item;
        }
        AtlasUnlocks unlocks = AtlasData.unlocksFor(sector);
        return withBiomeModifier(item, AtlasRuntime.regionOf(sector, run.atlasNode()), biomeId -> AtlasData.regionOpen(biomeId, unlocks));
    }

    static ItemData withBiomeModifier(ItemData item, String systemBiomeId, Predicate<String> biomeOpen) {
        ItemDefinition definition = item == null ? null : item.definition();
        if (definition == null || !definition.isHullItem() || definition.unique()) {
            return item;
        }
        Item preview = item.preview();
        if (preview == null || BiomeModifiers.biomeOf(preview) != null) {
            return item;
        }
        Random random = Seeds.derived(item.seed(), GRANT_SEED_SALT);
        String modifierId = modifierFor(definition, systemBiomeId, biomeOpen, random);
        return modifierId == null ? item : GrantedModifiers.add(item, modifierId, AffixLayout.dropLimits(definition.type()), random);
    }

    private static String modifierFor(ItemDefinition definition, String systemBiomeId, Predicate<String> biomeOpen, Random random) {
        String systemModifier = BiomeModifiers.modifierFor(systemBiomeId, definition.kind());
        if (systemModifier != null && GrantedModifiers.canGrant(definition, systemModifier)) {
            return systemModifier;
        }
        List<String> grantable = BiomeModifiers.modifiersFor(definition.kind()).stream()
                .filter(modifierId -> biomeOpen.test(BiomeModifiers.biomeOfModifier(modifierId)) && GrantedModifiers.canGrant(definition, modifierId))
                .toList();
        return grantable.isEmpty() ? null : grantable.get(random.nextInt(grantable.size()));
    }
}
