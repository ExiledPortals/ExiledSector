package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledlib.skills.AffinitySteps;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.special.SpecialSkillEffect;
import exiledsector.items.DoctrineOverrides;
import exiledsector.items.EmptyShells;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.NodeSocketable;
import exiledsector.items.RootConduits;
import exiledsector.items.Subroutine;
import exiledsector.persistence.ShipSkillDataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class AllocatedSkillEffects {

    private AllocatedSkillEffects() {
    }

    public static List<SkillEffect> forMember(FleetMemberAPI member) {
        return forData(ShipSkillDataManager.get(member.getId()), member.getHullSpec().getHullSize());
    }

    public static List<SkillEffect> forData(ShipSkillData data, HullSize hullSize) {
        return forNodes(data, AllocatedNode.of(data), hullSize);
    }

    public static List<SkillEffect> forNodes(ShipSkillData data, List<AllocatedNode> allocatedNodes, HullSize hullSize) {
        List<SkillEffect> effects = new ArrayList<>();
        for (SkillTypeEffect effect : treeEffects(data, allocatedNodes, hullSize)) {
            effects.add(effect.effect());
        }
        return effects;
    }

    public static List<SkillTypeEffect> treeEffects(ShipSkillData data, List<AllocatedNode> allocatedNodes, HullSize hullSize) {
        List<SkillTypeEffect> effects = new ArrayList<>();
        Set<String> hollowNodeIds = EmptyShells.hollowNodeIds(data);
        for (AllocatedNode allocated : allocatedNodes) {
            if (!hollowNodeIds.contains(allocated.node().getId())) {
                effects.addAll(nodeEffects(data, allocated, hullSize));
            }
        }
        return effects;
    }

    public static List<SkillTypeEffect> treeEffects(ShipSkillData data, AllocatedNode allocated, HullSize hullSize) {
        return EmptyShells.hollowNodeIds(data).contains(allocated.node().getId()) ? List.of() : nodeEffects(data, allocated, hullSize);
    }

    private static List<SkillTypeEffect> nodeEffects(ShipSkillData data, AllocatedNode allocated, HullSize hullSize) {
        return isVanillaPassthrough(allocated) ? doctrineEffects(data, allocated, hullSize, new ArrayList<>()) : appliedEffects(data, allocated, hullSize);
    }

    private static List<SkillTypeEffect> doctrineEffects(ShipSkillData data, AllocatedNode allocated, HullSize hullSize, List<SkillTypeEffect> applied) {
        for (SkillTypeEffect effect : DoctrineOverrides.addedEffects(data, allocated.node(), hullSize)) {
            if (appliesTo(data, effect.effect())) {
                applied.add(effect);
            }
        }
        return applied;
    }

    public static boolean isVanillaPassthrough(AllocatedNode allocated) {
        return allocated.effectiveType().getVanillaHullModId() != null;
    }

    public static List<SkillTypeEffect> appliedEffects(ShipSkillData data, AllocatedNode allocated, HullSize hullSize) {
        List<SkillTypeEffect> applied = appliedEffects(data, allocated.effectiveType(), hullSize);
        Item socketed = ItemStore.lookup(data.getSocketedItem(allocated.node().getId()));
        SkillTypeEffect conduit = socketed instanceof Subroutine ? RootConduits.effectAt(data, allocated.node().getId()) : null;
        if (conduit != null && conduit.magnitude() > 0f) {
            applied.add(conduit);
        }
        if (socketed != null && NodeSocketable.fits(socketed, allocated.node())) {
            for (SkillTypeEffect effect : socketed.skillEffects(hullSize)) {
                if (effect.effect() == SpecialSkillEffect.EVERY_AFFINITY_FLAT_PER_HOLLOW_NODE) {
                    float affinity = effect.magnitude() * EmptyShells.hollowCount(data, allocated.node().getId());
                    AffinitySteps.affinities().forEach(type -> applied.add(new SkillTypeEffect(type, affinity)));
                } else if (appliesTo(data, effect.effect())) {
                    applied.add(effect);
                }
            }
        }
        return doctrineEffects(data, allocated, hullSize, applied);
    }

    public static List<SkillTypeEffect> appliedEffects(ShipSkillData data, SkillType type, HullSize hullSize) {
        List<SkillTypeEffect> applied = new ArrayList<>();
        for (SkillTypeEffect effect : type.effectsFor(hullSize)) {
            if (appliesTo(data, effect.effect())) {
                applied.add(effect);
            }
        }
        return applied;
    }

    static boolean appliesTo(ShipSkillData data, SkillEffect effect) {
        return !data.isNpcBuild() || effect.appliesToNpcShips();
    }
}
