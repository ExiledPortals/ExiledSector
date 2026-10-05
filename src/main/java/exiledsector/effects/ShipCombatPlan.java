package exiledsector.effects;

import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class ShipCombatPlan {

    private final List<HullModEffect> vanillaEffects = new ArrayList<>();
    private final List<AppliedEffect> conditionalEffects = new ArrayList<>();
    private final List<TemporaryNode> temporaryNodes = new ArrayList<>();

    void addVanillaEffect(HullModEffect effect) {
        vanillaEffects.add(effect);
    }

    void addConditionalEffect(AppliedEffect effect) {
        conditionalEffects.add(effect);
    }

    void addTemporaryNode(float durationSeconds, List<AppliedEffect> effects) {
        if (!effects.isEmpty()) {
            temporaryNodes.add(new TemporaryNode(durationSeconds, effects));
        }
    }

    void advance(ShipAPI ship, float amount) {
        for (HullModEffect vanillaEffect : vanillaEffects) {
            vanillaEffect.advanceInCombat(ship, amount);
        }
        for (AppliedEffect conditional : conditionalEffects) {
            conditional.effect().advanceInCombat(ship, conditional.modId(), conditional.magnitude(), amount);
        }
        if (!temporaryNodes.isEmpty()) {
            expireTemporaryNodes(ship);
        }
    }

    private void expireTemporaryNodes(ShipAPI ship) {
        float deployedSeconds = ship.getFullTimeDeployed();
        Iterator<TemporaryNode> iterator = temporaryNodes.iterator();
        while (iterator.hasNext()) {
            TemporaryNode node = iterator.next();
            if (deployedSeconds < node.durationSeconds()) {
                continue;
            }
            for (AppliedEffect effect : node.effects()) {
                effect.effect().apply(ship.getMutableStats(), effect.modId(), 0f);
            }
            iterator.remove();
        }
    }

    record AppliedEffect(SkillEffect effect, String modId, float magnitude) {
    }

    private record TemporaryNode(float durationSeconds, List<AppliedEffect> effects) {
    }
}
