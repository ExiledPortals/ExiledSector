package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledlib.skills.SkillTypeEffect;
import exiledsector.items.NodeSocketable;
import exiledsector.persistence.ShipSkillDataManager;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledsector.items.DoctrineOverrides;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;

import java.util.ArrayList;
import java.util.List;

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
        for (AllocatedNode allocated : allocatedNodes) {
            effects.addAll(treeEffects(data, allocated, hullSize));
        }
        return effects;
    }

    public static List<SkillTypeEffect> treeEffects(ShipSkillData data, AllocatedNode allocated, HullSize hullSize) {
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
        if (socketed != null && NodeSocketable.fits(socketed, allocated.node())) {
            for (SkillTypeEffect effect : socketed.skillEffects(hullSize)) {
                if (appliesTo(data, effect.effect())) {
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
