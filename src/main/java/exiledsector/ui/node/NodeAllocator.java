package exiledsector.ui.node;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.compat.SecondInCommandCompat;
import exiledsector.effects.FighterBayOverflow;
import exiledsector.i18n.I18n;
import exiledsector.effects.OpReserveParity;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.effects.SkillTreeInstaller;
import exiledsector.i18n.Translation;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.HullModNames;
import exiledsector.skills.InstalledHullMods;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.RespecPlan;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipOpBudget;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.skills.skilleffect.FleetWideEffects;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

final class NodeAllocator {

    static final String LOCKED_REASON = "Unidentified - explore the sector to discover this node.";
    private static final String BEST_OF_THE_BEST_SKILL_ID = "best_of_the_best";

    record Snapshot(ShipSkillData data, String satisfiedRootId, ShipOpBudget budget, int totalOpBudget, int opCostPerNode,
                    int maxAllocatedNodes, int revision, Set<String> hiddenNodeIds, Set<String> allocatableNodeIds) {

        int opCostFor(SkillNode node) {
            return NodeAllocator.opCostFor(node, satisfiedRootId, opCostPerNode);
        }

        boolean canAllocate(SkillNode node) {
            return allocatableNodeIds.contains(node.getId());
        }

        boolean isHidden(SkillNode node) {
            return hiddenNodeIds.contains(node.getId());
        }
    }

    private final FleetMemberAPI member;
    private final ShipVariantAPI variant;
    private final Supplier<SkillNode> startingRoot;
    private int revision;

    NodeAllocator(FleetMemberAPI member, ShipVariantAPI variant, Supplier<SkillNode> startingRoot) {
        this.member = member;
        this.variant = variant;
        this.startingRoot = startingRoot;
    }

    ShipSkillData data() {
        return ShipSkillDataManager.get(member.getId());
    }

    String satisfiedRootId() {
        SkillNode root = startingRoot.get();
        return root == null ? null : root.getId();
    }

    private boolean isStartingRoot(SkillNode node) {
        SkillNode root = startingRoot.get();
        return root != null && node.getId().equals(root.getId());
    }

    Snapshot snapshot() {
        ShipSkillData data = data();
        ShipOpBudget budget = ShipOpBudget.of(member, variant);
        int opCostPerNode = SkillNodeOpCost.perNode(member.getHullSpec());
        String rootId = satisfiedRootId();
        int reservedOp = OpReserveParity.reservedOp(variant);
        OpReserveParity.warnIfOutOfSync(member, variant, data.getSpentOp(opCostPerNode), reservedOp, "while allocating nodes");
        int totalOpBudget = budget.total - budget.used + reservedOp;
        int maxAllocatedNodes = ShipLevelConfig.maxAllocatedNodes();
        return new Snapshot(data, rootId, budget, totalOpBudget, opCostPerNode, maxAllocatedNodes, revision,
                hiddenNodeIds(data), allocatableNodeIds(data, rootId, totalOpBudget, opCostPerNode, maxAllocatedNodes));
    }

    private static int opCostFor(SkillNode node, String rootId, int opCostPerNode) {
        return node.getId().equals(rootId) ? 0 : opCostPerNode;
    }

    private static Set<String> allocatableNodeIds(ShipSkillData data, String rootId, int totalOpBudget, int opCostPerNode,
                                                  int maxAllocatedNodes) {
        Set<String> allocatable = new HashSet<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            if (data.canAllocate(node, rootId, totalOpBudget, opCostFor(node, rootId, opCostPerNode), maxAllocatedNodes)) {
                allocatable.add(node.getId());
            }
        }
        return allocatable;
    }

    private static Set<String> hiddenNodeIds(ShipSkillData data) {
        Set<String> hidden = new HashSet<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            if (SkillTypeUnlockStatus.isHidden(node.getType(), data)) {
                hidden.add(node.getId());
            }
        }
        return hidden;
    }

    boolean canAllocate(SkillNode node) {
        return snapshot().canAllocate(node);
    }

    boolean canDeallocate(SkillNode node) {
        return !isStartingRoot(node) && blockDeallocationReason(node) == null
                && data().canDeallocate(node, SkillTree.topology(), satisfiedRootId());
    }

    boolean toggle(SkillNode node) {
        Snapshot snapshot = snapshot();
        ShipSkillData data = snapshot.data();
        boolean wasAllocated = data.isAllocated(node.getId());
        data.toggle(node, SkillTree.topology(), snapshot.satisfiedRootId(), snapshot.totalOpBudget(),
                snapshot.opCostFor(node), snapshot.maxAllocatedNodes());
        boolean isAllocatedNow = data.isAllocated(node.getId());
        if (isAllocatedNow == wasAllocated) {
            return false;
        }
        applyItemCost(node.getType(), isAllocatedNow);
        refreshShipStats();
        return true;
    }

    void allocateOption(SkillNode node, SkillType chosenOption) {
        data().selectOption(node, chosenOption, snapshot().opCostFor(node));
        refreshShipStats();
    }

    boolean socketItem(SkillNode node, String socketableId) {
        if (!data().socketItem(node.getId(), socketableId)) {
            return false;
        }
        refreshShipStats();
        return true;
    }

    boolean unsocketItem(SkillNode node) {
        if (data().unsocketItem(node.getId()) == null) {
            return false;
        }
        refreshShipStats();
        return true;
    }

    boolean chooseStartingRoot(SkillNode root) {
        if (!data().chooseStartingRoot(root)) {
            return false;
        }
        refreshShipStats();
        return true;
    }

    boolean canUnchooseStartingRoot(SkillNode node) {
        return isStartingRoot(node) && data().canUnchooseStartingRoot();
    }

    boolean unchooseStartingRoot(SkillNode node) {
        if (!canUnchooseStartingRoot(node) || !data().unchooseStartingRoot()) {
            return false;
        }
        refreshShipStats();
        return true;
    }

    String blockAllocationReason(SkillNode node, SkillType option) {
        ShipSkillData data = data();
        SkillType effectiveType = option != null ? option : node.getType();
        if (SkillTypeUnlockStatus.isLocked(effectiveType, data)) {
            return LOCKED_REASON;
        }

        if (!AllocatedNode.planned(node, option).exclusiveHullModIds().isEmpty()) {
            refreshVariantHullMods();
        }
        NodeEligibility.Block block = NodeEligibility.check(node, option, data, ShipFacts.of(member.getHullSpec(), this::hasHullMod));
        if (block != null) {
            return describe(block);
        }

        return itemCostReason(effectiveType);
    }

    List<SkillNode> respecPlan(SkillNode node) {
        return RespecPlan.of(data(), SkillTree.topology(), satisfiedRootId(), node);
    }

    boolean hasDeallocationCondition(SkillNode node) {
        SkillType type = node.resolveEffectiveType(data());
        for (SkillTypeEffect effect : type.effectsFor(member.getHullSpec().getHullSize())) {
            if (effect.effect().hasDeallocationCondition()) {
                return true;
            }
        }
        return false;
    }

    String blockDeallocationReason(SkillNode node) {
        SkillType type = node.resolveEffectiveType(data());
        for (SkillTypeEffect effect : type.effectsFor(member.getHullSpec().getHullSize())) {
            String blockReason = effect.effect().blockDeallocationReason(member, effect.magnitude());
            if (blockReason != null) {
                return blockReason;
            }
        }
        return null;
    }

    private void refreshShipStats() {
        revision++;
        SkillTreeInstaller.ensureInstalled(member, variant);
        FleetWideEffects.markPhaseFieldStale();
        new SkillTreeHullMod().applyEffectsBeforeShipCreation(member.getHullSpec().getHullSize(), member.getStats(), SkillTreeHullMod.ID);
        SkillDataResolver.syncShipTag(member, variant);
        SkillTreeHullMod.syncOpSpentHullMod(member, variant);
        SkillTreeHullMod.syncInstalledHullMods(member, variant);
        member.setStatUpdateNeeded(true);
        member.updateStats();
        returnUnhousedWings();
    }

    private void returnUnhousedWings() {
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        List<String> returned = FighterBayOverflow.returnUnhousedWings(member, variant, playerFleet == null ? null : playerFleet.getCargo());
        if (returned.isEmpty()) {
            return;
        }
        member.setStatUpdateNeeded(true);
        member.updateStats();
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (ui == null) {
            return;
        }
        for (String wingId : returned) {
            FighterWingSpecAPI wing = Global.getSettings().getFighterWingSpec(wingId);
            String name = wing == null ? wingId : wing.getWingName();
            String message = I18n.forGameText(() -> Translation.msg("fighterBay.returned").arg("wing", name).text());
            ui.addMessage(message.replace("%", "%%"), Misc.getTextColor());
        }
    }

    private static void applyItemCost(SkillType type, boolean allocated) {
        SkillItemCost itemCost = type.getItemCost();
        if (itemCost == null) {
            return;
        }
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        if (allocated) {
            cargo.removeCommodity(itemCost.itemId(), itemCost.quantity());
        } else {
            cargo.addCommodity(itemCost.itemId(), itemCost.quantity());
        }
    }

    private static String itemCostReason(SkillType type) {
        SkillItemCost itemCost = type.getItemCost();
        if (itemCost == null) {
            return null;
        }
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        float have = cargo.getCommodityQuantity(itemCost.itemId());
        if (have >= itemCost.quantity()) {
            return null;
        }
        return Translation.msg("node.block.itemCost").arg("quantity", itemCost.formattedQuantity()).arg("item", itemCost.commodityName())
                .arg("have", SkillItemCost.formatQuantity(have)).text();
    }

    private void refreshVariantHullMods() {
        member.setStatUpdateNeeded(true);
        member.updateStats();
        SkillTreeHullMod.syncOpSpentHullMod(member, variant);
    }

    private boolean hasHullMod(String hullModId) {
        return InstalledHullMods.hasHullModOfItsOwn(variant, hullModId) || SecondInCommandCompat.hasDeactivatedSMod(variant, hullModId);
    }

    private static String bestOfTheBestName() {
        SkillSpecAPI spec = Global.getSettings().getSkillSpec(BEST_OF_THE_BEST_SKILL_ID);
        return spec == null ? BEST_OF_THE_BEST_SKILL_ID : spec.getName();
    }

    private String describe(NodeEligibility.Block block) {
        return switch (block.kind()) {
            case WRONG_HULL_SIZE -> Translation.text("node.block.wrongHullSize");
            case UNMET_HULL_REQUIREMENT -> Translation.text("node.requires." + block.detail());
            case HULL_MOD_CONFLICT -> variant.hasHullMod(block.detail())
                    ? Translation.msg("node.block.hullModInstalled").arg("hullmod", HullModNames.displayName(block.detail())).text()
                    : Translation.msg("node.block.deactivatedSMod").arg("hullmod", HullModNames.displayName(block.detail()))
                            .arg("skill", bestOfTheBestName()).text();
            case TYPE_CONFLICT -> Translation.msg("node.block.typeAllocated").arg("node", block.conflictingType().getDisplayName()).text();
            case EFFECT_BLOCK -> block.detail();
        };
    }
}
