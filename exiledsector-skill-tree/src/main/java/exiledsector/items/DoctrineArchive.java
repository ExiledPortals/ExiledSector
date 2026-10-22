package exiledsector.items;

import exiledsector.skills.SkillTier;
import exiledsector.skills.radius.RadiusSize;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DoctrineArchive(String definitionId, DoctrineBehaviour behaviour, RadiusSize radiusSize, List<String> notableTypeIds,
                              List<String> keystoneTypeIds, List<String> smallTypeIds, List<PoolEntry> modifierPool, int minLines,
                              int maxLines, String factionId) {

    public DoctrineArchive {
        notableTypeIds = List.copyOf(notableTypeIds);
        keystoneTypeIds = List.copyOf(keystoneTypeIds);
        smallTypeIds = List.copyOf(smallTypeIds);
        modifierPool = List.copyOf(modifierPool);
    }

    public List<String> replacementPool(SkillTier tier) {
        if (tier == null) {
            return List.of();
        }
        return switch (tier) {
            case NOTABLE -> notableTypeIds;
            case KEYSTONE -> keystoneTypeIds;
            case SMALL -> smallTypeIds;
            default -> List.of();
        };
    }

    public boolean transforms() {
        return behaviour == DoctrineBehaviour.TRANSFORM;
    }

    public boolean augments() {
        return behaviour == DoctrineBehaviour.AUGMENT;
    }

    public Map<String, Double> lineChances() {
        List<PoolEntry> pool = modifierPool;
        Map<String, Double> chances = new LinkedHashMap<>();
        int lineCountOptions = maxLines - minLines + 1;
        for (int targetIndex = 0; targetIndex < pool.size(); targetIndex++) {
            double chanceTotal = 0.0;
            for (int lineCount = minLines; lineCount <= maxLines; lineCount++) {
                chanceTotal += drawnChance(pool, pool.get(targetIndex), Math.min(lineCount, pool.size()));
            }
            chances.put(pool.get(targetIndex).modifier().id(), chanceTotal / lineCountOptions);
        }
        return chances;
    }

    private static double drawnChance(List<PoolEntry> remainingPool, PoolEntry target, int drawsLeft) {
        double weightTotal = 0.0;
        for (PoolEntry entry : remainingPool) {
            weightTotal += entry.weight();
        }
        if (drawsLeft <= 0 || weightTotal <= 0.0) {
            return 0.0;
        }
        double chance = 0.0;
        for (int drawnIndex = 0; drawnIndex < remainingPool.size(); drawnIndex++) {
            PoolEntry drawn = remainingPool.get(drawnIndex);
            double drawnShare = drawn.weight() / weightTotal;
            if (drawn == target) {
                chance += drawnShare;
            } else if (drawnShare > 0.0) {
                List<PoolEntry> poolAfterDraw = new ArrayList<>(remainingPool);
                poolAfterDraw.remove(drawnIndex);
                chance += drawnShare * drawnChance(poolAfterDraw, target, drawsLeft - 1);
            }
        }
        return chance;
    }
}
