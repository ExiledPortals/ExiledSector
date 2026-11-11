package exiledsector.charts.anomaly.stormfront;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.AffixLayout;
import exiledsector.items.GrantedModifiers;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemType;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class StormLoot {

    private static final long GRANT_SEED_SALT = 0x73746f726d6c6fL;

    private StormLoot() {
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
        if (definition == null || definition.type() != ItemType.HULL_ITEM || definition.unique()) {
            return item;
        }
        Item preview = item.preview();
        if (preview == null || BiomeModifiers.biomeOf(preview) != null) {
            return item;
        }
        Random random = new Random(item.seed() ^ GRANT_SEED_SALT);
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
