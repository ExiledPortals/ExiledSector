package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.i18n.I18n;
import exiledsector.persistence.OpSpentSlotManager;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.AllocatedSkillEffects;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.skills.skilleffect.FleetWideEffects;
import exiledsector.skills.skilleffect.SkillEffect;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SkillTreeHullMod extends BaseHullMod implements HullModFleetEffect {

    public static final String ID = "exiledSector_core";

    private static final String MOD_ID_PREFIX = "exiledSector_skill_";
    private static final String MAGICLIB_WARNING_HULLMOD_ID = "ML_incompatibleHullmodWarning";
    static final String OP_SPENT_HULLMOD_ID_PREFIX = "exiledSector_opSpent_";
    private static final String INSTALLED_HULLMOD_TAG_PREFIX = "exiledSector_installed_";
    private static final String COMBAT_PLAN_KEY = "exiledSector_combatPlan";

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipSkillData data = SkillDataResolver.resolve(stats.getFleetMember(), stats.getVariant());
        if (data == null) return;

        boolean npcTree = SkillDataResolver.isNpcTree(stats.getVariant());
        List<AllocatedNode> allocated = AllocatedNode.of(data);
        forEachAllocatedEffect(data, allocated, hullSize,
                (vanillaEffect, vanillaHullModId) -> vanillaEffect.applyEffectsBeforeShipCreation(hullSize, stats, vanillaHullModId),
                (effect, modId, magnitude) -> {
                    if (!effect.appliesAfterOtherEffects()) effect.apply(stats, modId, magnitude);
                });
        forEachAllocatedEffect(data, allocated, hullSize, (vanillaEffect, vanillaHullModId) -> { },
                (effect, modId, magnitude) -> {
                    if (effect.appliesAfterOtherEffects()) effect.apply(stats, modId, magnitude);
                });
        if (!npcTree) {
            syncOpSpentHullMod(stats.getFleetMember(), stats.getVariant());
        }
        syncInstalledHullMods(installedHullModIds(allocated), stats.getVariant());
        if (!npcTree) {
            removeHullModsConflictingWithAllocatedSkills(allocated, stats.getVariant());
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        forEachAllocatedEffect(dataFor(ship), ship.getHullSize(),
                (vanillaEffect, vanillaHullModId) -> vanillaEffect.applyEffectsAfterShipCreation(ship, vanillaHullModId),
                (effect, modId, magnitude) -> effect.applyAfterShipCreation(ship, modId, magnitude));
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        forEachAllocatedEffect(dataFor(ship), ship.getHullSize(),
                (vanillaEffect, vanillaHullModId) -> vanillaEffect.applyEffectsToFighterSpawnedByShip(fighter, ship, vanillaHullModId),
                (effect, modId, magnitude) -> effect.applyToFighterSpawnedByShip(fighter, ship, modId, magnitude));
    }

    @Override
    public boolean withOnFleetSync() {
        return true;
    }

    @Override
    public void onFleetSync(CampaignFleetAPI fleet) {
        if (fleet != null && fleet.isPlayerFleet()) {
            FleetWideEffects.markPhaseFieldStale();
        }
    }

    @Override
    public boolean withAdvanceInCampaign() {
        return false;
    }

    // withAdvanceInCampaign() returns false, so the engine never calls this
    @Override
    @SuppressWarnings("java:S1186")
    public void advanceInCampaign(CampaignFleetAPI fleet) {
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        combatPlanFor(ship).advance(ship, amount);
    }

    private ShipCombatPlan combatPlanFor(ShipAPI ship) {
        Map<String, Object> customData = ship.getCustomData();
        if (customData != null && customData.get(COMBAT_PLAN_KEY) instanceof ShipCombatPlan plan) {
            return plan;
        }
        ShipCombatPlan plan = buildCombatPlan(dataFor(ship), ship.getHullSize());
        ship.setCustomData(COMBAT_PLAN_KEY, plan);
        return plan;
    }

    private ShipCombatPlan buildCombatPlan(ShipSkillData data, HullSize hullSize) {
        ShipCombatPlan plan = new ShipCombatPlan();
        forEachAllocatedEffect(data, hullSize,
                (vanillaEffect, vanillaHullModId) -> plan.addVanillaEffect(vanillaEffect),
                (effect, modId, magnitude) -> {
                    if (effect.isConditional()) {
                        plan.addConditionalEffect(new ShipCombatPlan.AppliedEffect(effect, modId, magnitude));
                    }
                });
        if (data == null) return plan;

        for (AllocatedNode allocated : AllocatedNode.of(data)) {
            SkillType type = allocated.effectiveType();
            Float durationSeconds = type.getTemporaryAfterDeploymentSeconds();
            if (durationSeconds == null) continue;

            String modId = MOD_ID_PREFIX + allocated.node().getId();
            List<ShipCombatPlan.AppliedEffect> effects = new ArrayList<>();
            for (SkillTypeEffect effect : AllocatedSkillEffects.appliedEffects(data, type, hullSize)) {
                effects.add(new ShipCombatPlan.AppliedEffect(effect.effect(), modId, effect.magnitude()));
            }
            plan.addTemporaryNode(durationSeconds, effects);
        }
        return plan;
    }

    private static ShipSkillData dataFor(ShipAPI ship) {
        return SkillDataResolver.resolve(ship.getMutableStats().getFleetMember(), ship.getVariant());
    }

    private void forEachAllocatedEffect(ShipSkillData data, HullSize hullSize,
                                         VanillaDelegate vanillaDelegate, EffectAction action) {
        if (data == null) return;

        forEachAllocatedEffect(data, AllocatedNode.of(data), hullSize, vanillaDelegate, action);
    }

    private void forEachAllocatedEffect(ShipSkillData data, List<AllocatedNode> allocatedNodes, HullSize hullSize,
                                         VanillaDelegate vanillaDelegate, EffectAction action) {
        for (AllocatedNode allocated : allocatedNodes) {
            SkillType type = allocated.effectiveType();
            String vanillaHullModId = type.getVanillaHullModId();
            if (vanillaHullModId != null) {
                HullModSpecAPI spec = Global.getSettings().getHullModSpec(vanillaHullModId);
                if (spec != null && spec.getEffect() != null) {
                    vanillaDelegate.apply(spec.getEffect(), vanillaHullModId);
                }
            } else {
                String modId = MOD_ID_PREFIX + allocated.node().getId();
                for (SkillTypeEffect effect : AllocatedSkillEffects.appliedEffects(data, type, hullSize)) {
                    action.apply(effect.effect(), modId, effect.magnitude());
                }
            }
        }
    }

    public static void syncOpSpentHullMod(FleetMemberAPI member, ShipVariantAPI variant) {
        if (member == null || variant == null || SkillDataResolver.isNpcTree(variant)) return;

        ShipSkillData data = ShipSkillDataManager.find(member.getId());
        int opSpent = data == null ? 0 : data.getSpentOp(SkillNodeOpCost.perNode(member.getHullSpec()));
        if (opSpent <= 0) {
            Integer slot = OpSpentSlotManager.existingSlot(member.getId());
            if (slot != null) {
                String reserveId = OP_SPENT_HULLMOD_ID_PREFIX + slot;
                setReserveCost(reserveId, 0);
                variant.removeMod(reserveId);
            }
            removeReservesOfReleasedSlots(variant);
            return;
        }

        String reserveId = reserveHullModFor(member.getId(), opSpent);
        for (String hullModId : new ArrayList<>(variant.getHullMods())) {
            if (hullModId.startsWith(OP_SPENT_HULLMOD_ID_PREFIX) && !hullModId.equals(reserveId)) {
                variant.removeMod(hullModId);
            }
        }
        if (reserveId != null && !variant.hasHullMod(reserveId)) {
            variant.addMod(reserveId);
        }
    }

    private static void removeReservesOfReleasedSlots(ShipVariantAPI variant) {
        List<String> stale = null;
        for (String hullModId : variant.getHullMods()) {
            if (hullModId.startsWith(OP_SPENT_HULLMOD_ID_PREFIX) && isReleasedSlot(hullModId)) {
                if (stale == null) {
                    stale = new ArrayList<>();
                }
                stale.add(hullModId);
            }
        }
        if (stale != null) {
            stale.forEach(variant::removeMod);
        }
    }

    private static boolean isReleasedSlot(String reserveId) {
        try {
            return !OpSpentSlotManager.isAssigned(Integer.parseInt(reserveId.substring(OP_SPENT_HULLMOD_ID_PREFIX.length())));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String reserveHullModFor(String shipId, int opSpent) {
        String hullModId = OP_SPENT_HULLMOD_ID_PREFIX + OpSpentSlotManager.slotFor(shipId);
        return setReserveCost(hullModId, opSpent) ? hullModId : null;
    }

    private static boolean setReserveCost(String hullModId, int cost) {
        HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
        if (spec == null) return false;

        spec.setFrigateCost(cost);
        spec.setDestroyerCost(cost);
        spec.setCruiserCost(cost);
        spec.setCapitalCost(cost);
        return true;
    }

    public static void syncInstalledHullMods(FleetMemberAPI member, ShipVariantAPI variant) {
        ShipSkillData data = SkillDataResolver.resolve(member, variant);
        if (data == null) return;

        syncInstalledHullMods(installedHullModIds(AllocatedNode.of(data)), variant);
    }

    private static void syncInstalledHullMods(Set<String> wanted, ShipVariantAPI variant) {
        if (variant == null) return;

        for (String hullModId : wanted) {
            if (!variant.hasHullMod(hullModId)) {
                variant.addPermaMod(hullModId);
                if (!isInstalledBySkillTree(variant, hullModId)) {
                    variant.addTag(INSTALLED_HULLMOD_TAG_PREFIX + hullModId);
                }
            }
        }
        for (String tag : new ArrayList<>(variant.getTags())) {
            String hullModId = tag.startsWith(INSTALLED_HULLMOD_TAG_PREFIX) ? tag.substring(INSTALLED_HULLMOD_TAG_PREFIX.length()) : null;
            if (hullModId != null && !wanted.contains(hullModId)) {
                if (!variant.getSMods().contains(hullModId)) {
                    variant.removePermaMod(hullModId);
                }
                variant.removeTag(tag);
            }
        }
    }

    public static boolean restoreInstalledPermaMods(ShipVariantAPI variant) {
        boolean restored = false;
        for (String tag : variant.getTags()) {
            String hullModId = tag.startsWith(INSTALLED_HULLMOD_TAG_PREFIX) ? tag.substring(INSTALLED_HULLMOD_TAG_PREFIX.length()) : null;
            if (hullModId != null && variant.hasHullMod(hullModId) && !variant.getPermaMods().contains(hullModId)) {
                variant.addPermaMod(hullModId);
                restored = true;
            }
        }
        return restored;
    }

    public static boolean isInstalledBySkillTree(ShipVariantAPI variant, String hullModId) {
        return variant.hasTag(INSTALLED_HULLMOD_TAG_PREFIX + hullModId);
    }

    private static boolean isRemovableConflict(ShipVariantAPI variant, String hullModId) {
        boolean builtIn = variant.getHullSpec() != null && variant.getHullSpec().isBuiltInMod(hullModId);
        return variant.hasHullMod(hullModId) && !builtIn && !isInstalledBySkillTree(variant, hullModId);
    }

    private static Set<String> installedHullModIds(List<AllocatedNode> allocatedNodes) {
        Set<String> ids = new LinkedHashSet<>();
        for (AllocatedNode allocated : allocatedNodes) {
            ids.addAll(allocated.effectiveType().getInstalledHullModIds());
        }
        return ids;
    }

    public static void removeHullModsConflictingWithAllocatedSkills(FleetMemberAPI member, ShipVariantAPI variant) {
        if (member == null || SkillDataResolver.isNpcTree(variant)) return;

        removeHullModsConflictingWithAllocatedSkills(AllocatedNode.of(ShipSkillDataManager.get(member.getId())), variant);
    }

    private static void removeHullModsConflictingWithAllocatedSkills(List<AllocatedNode> allocatedNodes, ShipVariantAPI variant) {
        if (variant == null) return;

        boolean conflictFound = false;
        for (AllocatedNode allocated : allocatedNodes) {
            SkillType type = allocated.effectiveType();
            for (String hullModId : type.getExclusiveHullModIds()) {
                if (isRemovableConflict(variant, hullModId)) {
                    MagicIncompatibleHullmods.removeHullmodWithWarning(variant, hullModId, SkillConflictWarningHullMod.ID);
                    variant.removeMod(MAGICLIB_WARNING_HULLMOD_ID);
                    variant.addMod(SkillConflictWarningHullMod.ID);
                    SkillConflictWarnings.recordRemoval(variant, hullModId, I18n.forGameText(type::getDisplayName));
                    conflictFound = true;
                }
            }
        }

        if (!conflictFound) {
            if (variant.hasHullMod(SkillConflictWarningHullMod.ID)) {
                variant.removeMod(SkillConflictWarningHullMod.ID);
            }
            SkillConflictWarnings.clear(variant);
        }
    }

    private interface VanillaDelegate {
        void apply(HullModEffect vanillaEffect, String vanillaHullModId);
    }

    private interface EffectAction {
        void apply(SkillEffect effect, String modId, float magnitude);
    }
}
