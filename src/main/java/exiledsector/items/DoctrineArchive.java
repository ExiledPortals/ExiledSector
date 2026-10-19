package exiledsector.items;

import exiledsector.skills.SkillTier;
import exiledsector.skills.radius.RadiusSize;

import java.util.List;

public record DoctrineArchive(String definitionId, DoctrineBehaviour behaviour, RadiusSize radiusSize, List<String> notableTypeIds,
                              List<String> keystoneTypeIds, List<String> smallTypeIds, List<PoolEntry> modifierPool, int minLines,
                              int maxLines) {

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
}
