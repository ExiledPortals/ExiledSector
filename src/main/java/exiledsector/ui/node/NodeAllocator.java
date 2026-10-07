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
import exiledsector.effects.OpReserveHullMods;
import exiledsector.effects.OpReserveParity;
import exiledsector.effects.PhantomInstallSync;
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

    private static final String BEST_OF_THE_BEST_SKILL_ID = "best_of_the_best";

    record Snapshot(ShipSkillData skillData, String satisfiedRootId, ShipOpBudget opBudget, int totalOpBudget, int opCostPerNode,
                    int maxAllocatedNodes, int statsRevision, Set<String> hiddenNodeIds, Set<String> allocatableNodeIds) {

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

    private final FleetMemberAPI fleetMember;
    private final ShipVariantAPI shipVariant;
    private final Supplier<SkillNode> startingRootSupplier;
    private int statsRevision;

    NodeAllocator(FleetMemberAPI fleetMember, ShipVariantAPI shipVariant, Supplier<SkillNode> startingRootSupplier) {
        this.fleetMember = fleetMember;
        this.shipVariant = shipVariant;
        this.startingRootSupplier = startingRootSupplier;
    }

    ShipSkillData data() {
        return ShipSkillDataManager.get(fleetMember.getId());
    }

    String satisfiedRootId() {
        SkillNode root = startingRootSupplier.get();
        return root == null ? null : root.getId();
    }

    private boolean isStartingRoot(SkillNode node) {
        SkillNode root = startingRootSupplier.get();
        return root != null && node.getId().equals(root.getId());
    }

    Snapshot snapshot() {
        ShipSkillData skillData = data();
        ShipOpBudget opBudget = ShipOpBudget.of(fleetMember, shipVariant);
        int opCostPerNode = SkillNodeOpCost.perNode(fleetMember.getHullSpec());
        String rootId = satisfiedRootId();
        int reservedOp = OpReserveParity.reservedOp(shipVariant);
        OpReserveParity.warnIfOutOfSync(fleetMember, shipVariant, skillData.getSpentOp(opCostPerNode), reservedOp, "while allocating nodes");
        int totalOpBudget = opBudget.totalOp - opBudget.usedOp + reservedOp;
        int maxAllocatedNodes = ShipLevelConfig.maxAllocatedNodes();
        return new Snapshot(skillData, rootId, opBudget, totalOpBudget, opCostPerNode, maxAllocatedNodes, statsRevision,
                hiddenNodeIds(skillData), allocatableNodeIds(skillData, rootId, totalOpBudget, opCostPerNode, maxAllocatedNodes));
    }

    private static int opCostFor(SkillNode node, String rootId, int opCostPerNode) {
        return node.getId().equals(rootId) ? 0 : opCostPerNode;
    }

    private static Set<String> allocatableNodeIds(ShipSkillData skillData, String rootId, int totalOpBudget, int opCostPerNode,
                                                  int maxAllocatedNodes) {
        Set<String> allocatable = new HashSet<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            if (skillData.canAllocate(node, rootId, totalOpBudget, opCostFor(node, rootId, opCostPerNode), maxAllocatedNodes)) {
                allocatable.add(node.getId());
            }
        }
        return allocatable;
    }

    private static Set<String> hiddenNodeIds(ShipSkillData skillData) {
        Set<String> hidden = new HashSet<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            if (SkillTypeUnlockStatus.isHidden(node.getType(), skillData)) {
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
        ShipSkillData skillData = snapshot.skillData();
        boolean wasAllocated = skillData.isAllocated(node.getId());
        skillData.toggle(node, SkillTree.topology(), snapshot.satisfiedRootId(), snapshot.totalOpBudget(),
                snapshot.opCostFor(node), snapshot.maxAllocatedNodes());
        boolean isAllocatedNow = skillData.isAllocated(node.getId());
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
        ShipSkillData skillData = data();
        if (!AllocatedNode.planned(node, option).exclusiveHullModIds().isEmpty()) {
            refreshVariantHullMods();
        }
        NodeEligibility.Block block = NodeEligibility.check(node, option, NodeEligibility.Context.of(skillData,
                ShipFacts.of(fleetMember.getHullSpec(), this::hasHullMod), type -> SkillTypeUnlockStatus.isLocked(type, skillData)));
        if (block != null) {
            return describe(block);
        }
        return itemCostReason(option != null ? option : node.getType());
    }

    List<SkillNode> respecPlan(SkillNode node) {
        return RespecPlan.of(data(), SkillTree.topology(), satisfiedRootId(), node);
    }

    boolean hasDeallocationCondition(SkillNode node) {
        SkillType type = node.resolveEffectiveType(data());
        for (SkillTypeEffect effect : type.effectsFor(fleetMember.getHullSpec().getHullSize())) {
            if (effect.effect().hasDeallocationCondition()) {
                return true;
            }
        }
        return false;
    }

    String blockDeallocationReason(SkillNode node) {
        SkillType type = node.resolveEffectiveType(data());
        for (SkillTypeEffect effect : type.effectsFor(fleetMember.getHullSpec().getHullSize())) {
            String blockReason = effect.effect().blockDeallocationReason(fleetMember, effect.magnitude());
            if (blockReason != null) {
                return blockReason;
            }
        }
        return null;
    }

    private void refreshShipStats() {
        statsRevision++;
        SkillTreeInstaller.ensureInstalled(fleetMember, shipVariant);
        FleetWideEffects.markPhaseFieldStale();
        new SkillTreeHullMod().applyEffectsBeforeShipCreation(fleetMember.getHullSpec().getHullSize(), fleetMember.getStats(), SkillTreeHullMod.ID);
        SkillDataResolver.syncShipTag(fleetMember, shipVariant);
        OpReserveHullMods.sync(fleetMember, shipVariant);
        PhantomInstallSync.sync(fleetMember, shipVariant);
        fleetMember.setStatUpdateNeeded(true);
        fleetMember.updateStats();
        returnUnhousedWings();
    }

    private void returnUnhousedWings() {
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        List<String> returnedWingIds = FighterBayOverflow.returnUnhousedWings(fleetMember, shipVariant, playerFleet == null ? null : playerFleet.getCargo());
        if (returnedWingIds.isEmpty()) {
            return;
        }
        fleetMember.setStatUpdateNeeded(true);
        fleetMember.updateStats();
        CampaignUIAPI campaignUi = Global.getSector().getCampaignUI();
        if (campaignUi == null) {
            return;
        }
        for (String wingId : returnedWingIds) {
            FighterWingSpecAPI wing = Global.getSettings().getFighterWingSpec(wingId);
            String name = wing == null ? wingId : wing.getWingName();
            String message = I18n.forGameText(() -> Translation.msg("fighterBay.returned").arg("wing", name).text());
            campaignUi.addMessage(message.replace("%", "%%"), Misc.getTextColor());
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
        fleetMember.setStatUpdateNeeded(true);
        fleetMember.updateStats();
        OpReserveHullMods.sync(fleetMember, shipVariant);
    }

    private boolean hasHullMod(String hullModId) {
        return InstalledHullMods.hasHullModOfItsOwn(shipVariant, hullModId) || SecondInCommandCompat.hasDeactivatedSMod(shipVariant, hullModId);
    }

    private static String bestOfTheBestName() {
        SkillSpecAPI spec = Global.getSettings().getSkillSpec(BEST_OF_THE_BEST_SKILL_ID);
        return spec == null ? BEST_OF_THE_BEST_SKILL_ID : spec.getName();
    }

    private String describe(NodeEligibility.Block block) {
        return switch (block.kind()) {
            case LOCKED -> Translation.text("node.block.locked");
            case INVALID_OPTION -> Translation.text("node.block.invalidOption");
            case WRONG_HULL_SIZE -> Translation.text("node.block.wrongHullSize");
            case UNMET_HULL_REQUIREMENT, UNMET_SHIP_REQUIREMENT -> Translation.text("node.requires." + block.detail());
            case HULL_MOD_CONFLICT -> shipVariant.hasHullMod(block.detail())
                    ? Translation.msg("node.block.hullModInstalled").arg("hullmod", HullModNames.displayName(block.detail())).text()
                    : Translation.msg("node.block.deactivatedSMod").arg("hullmod", HullModNames.displayName(block.detail()))
                            .arg("skill", bestOfTheBestName()).text();
            case TYPE_CONFLICT -> Translation.msg("node.block.typeAllocated").arg("node", block.conflictingType().getDisplayName()).text();
            case EFFECT_BLOCK -> block.detail();
        };
    }
}
