package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.items.ItemEffects;
import exiledlib.items.ItemType;
import exiledlib.modifiers.ModifierEffect;
import exiledlib.modifiers.ScaleContext;
import exiledlib.modifiers.TierRange;
import exiledsector.skills.SkillType;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.List;

public record DoctrineRewrite(String nodeId, String doctrineDefinitionId, int revision, SkillType originalType, SkillType replacementType,
                              List<DoctrineRewriter.AugmentLine> augmentLines) {

    public DoctrineRewrite {
        augmentLines = List.copyOf(augmentLines);
    }

    public boolean transformed() {
        return replacementType != null;
    }

    public boolean augmented() {
        return !augmentLines.isEmpty();
    }

    public List<SkillTypeEffect> addedEffects(HullSize hullSize) {
        if (augmentLines.isEmpty()) {
            return List.of();
        }
        ScaleContext context = ScaleContext.of(hullSize, TierRange.NO_TIER);
        List<SkillTypeEffect> effects = new ArrayList<>(augmentLines.size());
        for (DoctrineRewriter.AugmentLine augmentLine : augmentLines) {
            ModifierEffect effect = ItemEffects.forType(ItemType.SUBROUTINE).byName(augmentLine.line().effectName());
            if (effect instanceof SkillEffect skillEffect) {
                effects.add(new SkillTypeEffect(skillEffect, augmentLine.line().valueFor(augmentLine.rolledMagnitude(), context)));
            }
        }
        return effects;
    }
}
