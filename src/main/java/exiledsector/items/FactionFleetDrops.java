package exiledsector.items;

import exiledsector.skills.npc.NpcFactionVolumes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public final class FactionFleetDrops {

    public static final float MAX_CHANCE_PER_BATTLE = 0.5f;
    static final float DEPLOYMENT_POINTS_PER_CHANCE_STEP = 100f;
    private static final AtomicReference<Map<String, Map<String, Source>>> SOURCES_BY_GROUP = new AtomicReference<>(Map.of());

    public record Source(String volume, Set<String> extraFactionIds, float chancePer100DeploymentPoints, boolean anyFaction) {

        public Source {
            extraFactionIds = extraFactionIds == null ? Set.of() : Set.copyOf(extraFactionIds);
        }

        public Source(String volume, Set<String> extraFactionIds, float chancePer100DeploymentPoints) {
            this(volume, extraFactionIds, chancePer100DeploymentPoints, false);
        }

        public static Source anyFaction(float chancePer100DeploymentPoints) {
            return new Source(null, Set.of(), chancePer100DeploymentPoints, true);
        }

        boolean dropsFrom(String factionId) {
            return factionId != null && (anyFaction || extraFactionIds.contains(factionId) || volume != null && volume.equals(NpcFactionVolumes.regionFor(factionId)));
        }
    }

    private FactionFleetDrops() {
    }

    public static void register(ItemType type, Map<String, Source> sourcesByDefinitionId) {
        register(type.name(), sourcesByDefinitionId);
    }

    public static void register(String sourceGroup, Map<String, Source> sourcesByDefinitionId) {
        SOURCES_BY_GROUP.updateAndGet(current -> {
            Map<String, Map<String, Source>> updated = new LinkedHashMap<>(current);
            updated.put(sourceGroup, Collections.unmodifiableMap(new LinkedHashMap<>(sourcesByDefinitionId)));
            return Collections.unmodifiableMap(updated);
        });
    }

    public static Source source(String definitionId) {
        for (Map<String, Source> sources : SOURCES_BY_GROUP.get().values()) {
            Source source = sources.get(definitionId);
            if (source != null) {
                return source;
            }
        }
        return null;
    }

    public static float chance(Source source, Map<String, Float> defeatedDeploymentPointsByFaction) {
        float qualifyingDeploymentPoints = 0f;
        for (Map.Entry<String, Float> defeated : defeatedDeploymentPointsByFaction.entrySet()) {
            if (source.dropsFrom(defeated.getKey()) && defeated.getValue() != null && defeated.getValue() > 0f) {
                qualifyingDeploymentPoints += defeated.getValue();
            }
        }
        float chance = qualifyingDeploymentPoints / DEPLOYMENT_POINTS_PER_CHANCE_STEP * Math.max(0f, source.chancePer100DeploymentPoints());
        return Math.min(MAX_CHANCE_PER_BATTLE, chance);
    }

    public static List<ItemData> roll(Map<String, Float> defeatedDeploymentPointsByFaction, Random random, Predicate<ItemDefinition> allowed) {
        List<ItemData> droppedItems = new ArrayList<>();
        if (defeatedDeploymentPointsByFaction == null || defeatedDeploymentPointsByFaction.isEmpty()) {
            return droppedItems;
        }
        for (Map<String, Source> sources : SOURCES_BY_GROUP.get().values()) {
            for (Map.Entry<String, Source> sourceEntry : sources.entrySet()) {
                float chance = chance(sourceEntry.getValue(), defeatedDeploymentPointsByFaction);
                if (!(chance > 0f) || random.nextFloat() >= chance) {
                    continue;
                }
                ItemDefinition definition = ItemDefinitions.get(sourceEntry.getKey());
                if (definition != null && allowed.test(definition)) {
                    droppedItems.add(ItemData.rolled(definition, random.nextLong()));
                }
            }
        }
        return droppedItems;
    }

    public static boolean available(ItemDefinition definition) {
        if (definition == null || !(definition.rarity() > 0f)) {
            return false;
        }
        if (definition.type() == ItemType.WORMHOLE_ANCHOR) {
            return WormholeAnchorDefinitions.areaActive(definition.id());
        }
        return true;
    }

    public static void clear(ItemType type) {
        clear(type.name());
    }

    public static void clear(String sourceGroup) {
        SOURCES_BY_GROUP.updateAndGet(current -> {
            Map<String, Map<String, Source>> updated = new LinkedHashMap<>(current);
            updated.remove(sourceGroup);
            return Collections.unmodifiableMap(updated);
        });
    }
}
