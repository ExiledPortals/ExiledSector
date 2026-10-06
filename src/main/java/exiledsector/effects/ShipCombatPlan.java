package exiledsector.effects;

import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class ShipCombatPlan {

    private final List<HullModEffect> vanillaEffects = new ArrayList<>();
    private final List<ResolvedTree.EffectEntry> combatUpdates = new ArrayList<>();
    private final List<TemporaryNode> temporaryNodes = new ArrayList<>();

    void addVanillaEffect(HullModEffect effect) {
        vanillaEffects.add(effect);
    }

    void addCombatUpdate(ResolvedTree.EffectEntry effect) {
        combatUpdates.add(effect);
    }

    void addTemporaryNode(float durationSeconds, List<ResolvedTree.EffectEntry> effects) {
        if (!effects.isEmpty()) {
            temporaryNodes.add(new TemporaryNode(durationSeconds, effects));
        }
    }

    void advance(ShipAPI ship, float amount) {
        for (HullModEffect vanillaEffect : vanillaEffects) {
            vanillaEffect.advanceInCombat(ship, amount);
        }
        for (ResolvedTree.EffectEntry update : combatUpdates) {
            update.effect().advanceInCombat(ship, update.modId(), update.magnitude(), amount);
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
            for (ResolvedTree.EffectEntry effect : node.effects()) {
                effect.effect().apply(ship.getMutableStats(), effect.modId(), 0f);
            }
            iterator.remove();
        }
    }

    private record TemporaryNode(float durationSeconds, List<ResolvedTree.EffectEntry> effects) {
    }
}
