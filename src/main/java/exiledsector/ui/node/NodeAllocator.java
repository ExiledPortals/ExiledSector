package exiledsector.ui.node;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.compat.SecondInCommandCompat;
import exiledsector.effects.FighterBayOverflow;
import exiledsector.i18n.I18n;
import exiledsector.effects.OpReserveParity;
import exiledsector.effects.PhantomConflictWatch;
import exiledsector.effects.ShipTreeSync;
import exiledsector.i18n.Translation;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocationGate;
import exiledsector.skills.FrameworkFit;
import exiledsector.skills.FrameworkSlots;
import exiledsector.skills.HullModNames;
import exiledsector.skills.InstalledHullMods;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.RespecPlan;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipOpBudget;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.skills.skilleffect.FleetWideEffects;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.socketables.FrameworkSockets;
import exiledsector.socketables.SocketType;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableStore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

final class NodeAllocator {

    private static final String BEST_OF_THE_BEST_SKILL_ID = "best_of_the_best";

    static final class Snapshot {

        private final ShipSkillData skillData;
        private final ShipOpBudget opBudget;
        private final int statsRevision;
        private final int dataRevision;
        private final Set<String> hiddenNodeIds;
        private final AllocationGate gate;
        private final BiFunction<AllocationGate.Verdict, AllocationGate, String> describer;
        private final Map<String, String> reasonsByKey = new HashMap<>();

        Snapshot(ShipSkillData skillData, ShipOpBudget opBudget, int statsRevision, Set<String> hiddenNodeIds, AllocationGate gate,
                 BiFunction<AllocationGate.Verdict, AllocationGate, String> describer) {
            this.skillData = skillData;
            this.opBudget = opBudget;
            this.statsRevision = statsRevision;
            this.dataRevision = skillData.revision();
            this.hiddenNodeIds = hiddenNodeIds;
            this.gate = gate;
            this.describer = describer;
        }

        ShipSkillData skillData() {
            return skillData;
        }

        String satisfiedRootId() {
            return gate.rootId();
        }

        ShipOpBudget opBudget() {
            return opBudget;
        }

        int totalOpBudget() {
            return gate.budget().totalOp();
        }

        int opCostPerNode() {
            return gate.budget().opCostPerNode();
        }

        int maxAllocatedNodes() {
            return gate.budget().maxAllocatedNodes();
        }

        int statsRevision() {
            return statsRevision;
        }

        AllocationGate gate() {
            return gate;
        }

        int opCostFor(SkillNode node) {
            return gate.opCostFor(node);
        }

        boolean canAllocate(SkillNode node) {
            return gate.allocation(node).allowed();
        }

        boolean isHidden(SkillNode node) {
            return hiddenNodeIds.contains(node.getId());
        }

        boolean isCurrent(ShipSkillData currentData) {
            return currentData == skillData && currentData.revision() == dataRevision;
        }

        String refusalReason(SkillNode node) {
            if (isHidden(node) || skillData.isAllocated(node.getId())) {
                return null;
            }
            return allocationRefusalReason(node, null);
        }

        String allocationRefusalReason(SkillNode node, SkillType option) {
            if (option == null) {
                return reason(node.getId(), gate.allocation(node));
            }
            return reason(node.getId() + '/' + option.getId(), gate.allocation(node, option));
        }

        String optionRefusalReason(SkillNode node, SkillType option) {
            if (!skillData.isAllocated(node.getId())) {
                return allocationRefusalReason(node, option);
            }
            return reason(node.getId() + '/' + option.getId(), gate.optionSwitch(node, option));
        }

        private String reason(String key, AllocationGate.Verdict verdict) {
            if (!verdict.worthExplaining()) {
                return null;
            }
            return reasonsByKey.computeIfAbsent(key, ignored -> describer.apply(verdict, gate));
        }
    }

    private final FleetMemberAPI fleetMember;
    private final ShipVariantAPI shipVariant;
    private final Supplier<SkillNode> startingRootSupplier;
    private int statsRevision;
    private boolean variantHullModsRefreshed;

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
        refreshVariantHullModsOnce();
        ShipSkillData skillData = data();
        ShipOpBudget opBudget = ShipOpBudget.of(fleetMember, shipVariant);
        int opCostPerNode = SkillNodeOpCost.perNode(fleetMember.getHullSpec());
        int reservedOp = OpReserveParity.reservedOp(shipVariant);
        OpReserveParity.warnIfOutOfSync(fleetMember, shipVariant, skillData.getSpentOp(opCostPerNode), reservedOp, "while allocating nodes");
        int totalOpBudget = opBudget.totalOp - opBudget.usedOp + reservedOp;
        AllocationGate.Budget budget = new AllocationGate.Budget(totalOpBudget, opCostPerNode, ShipLevelConfig.maxAllocatedNodes());
        NodeEligibility.Context eligibility = NodeEligibility.Context.of(skillData, ShipFacts.of(fleetMember.getHullSpec(), this::hasHullMod),
                type -> SkillTypeUnlockStatus.isLocked(type, skillData), NodeAllocator::heldInCargo);
        AllocationGate gate = new AllocationGate(skillData, SkillTree.topology(), satisfiedRootId(), budget, eligibility);
        return new Snapshot(skillData, opBudget, statsRevision, hiddenNodeIds(skillData), gate, this::describe);
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

    boolean allocate(SkillNode node, SkillType option, Snapshot snapshot) {
        AllocationGate gate = snapshot.gate();
        SkillItemCost itemCost = NodeEligibility.itemCost(node, option);
        if (!gate.allocation(node, option).allowed() || itemCost != null && heldInCargo(itemCost.itemId()) < itemCost.quantity()) {
            return false;
        }
        ShipSkillData skillData = snapshot.skillData();
        if (option != null) {
            skillData.selectOption(node, option, gate.opCostFor(node));
        } else {
            skillData.allocate(node, gate.opCostFor(node));
        }
        chargeItems(skillData, node.getId(), itemCost);
        refreshShipStats();
        return true;
    }

    boolean switchOption(SkillNode node, SkillType option, Snapshot snapshot) {
        AllocationGate gate = snapshot.gate();
        if (!gate.optionSwitch(node, option).allowed()) {
            return false;
        }
        ShipSkillData skillData = snapshot.skillData();
        SkillItemCost refund = skillData.itemCharge(node.getId());
        SkillItemCost itemCost = NodeEligibility.itemCost(node, option);
        if (itemCost != null && heldInCargo(itemCost.itemId()) + refundOf(refund, itemCost.itemId()) < itemCost.quantity()) {
            return false;
        }
        refundItems(skillData.takeItemCharge(node.getId()));
        skillData.selectOption(node, option, gate.opCostFor(node));
        chargeItems(skillData, node.getId(), itemCost);
        refreshShipStats();
        return true;
    }

    boolean deallocate(SkillNode node, Snapshot snapshot) {
        if (!snapshot.gate().deallocation(node).allowed()) {
            return false;
        }
        ShipSkillData skillData = snapshot.skillData();
        Set<String> allocatedBefore = new LinkedHashSet<>(skillData.getAllocatedNodeIds());
        skillData.deallocate(node);
        for (String releasedId : allocatedBefore) {
            if (!skillData.isAllocated(releasedId)) {
                refundItems(skillData.takeItemCharge(releasedId));
            }
        }
        refreshShipStats();
        return true;
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

    boolean installUpgrade() {
        if (!FrameworkSockets.installUpgrade(data(), hullSize(), SocketableStore.get())) {
            return false;
        }
        refreshShipStats();
        return true;
    }

    boolean unlockSocket(SocketType socketType) {
        if (!FrameworkSockets.unlock(data(), socketType, currentFit())) {
            return false;
        }
        refreshShipStats();
        return true;
    }

    boolean lockSocket(SocketType socketType) {
        if (!FrameworkSockets.lock(data(), socketType)) {
            return false;
        }
        refreshShipStats();
        return true;
    }

    boolean socketFrameworkItem(SocketType socketType, Socketable socketable) {
        if (!FrameworkSockets.socket(data(), socketType, socketable, ShipSkillDataManager.all())) {
            return false;
        }
        refreshShipStats();
        return true;
    }

    boolean unsocketFrameworkItem(SocketType socketType) {
        if (data().unsocketFrameworkItem(socketType.id()) == null) {
            return false;
        }
        refreshShipStats();
        return true;
    }

    List<FrameworkSlots.Slot> frameworkSlots() {
        return FrameworkSlots.forVariant(data(), shipVariant);
    }

    FrameworkSockets.UnlockBlock unlockBlock(SocketType socketType) {
        return FrameworkSockets.unlockBlock(data(), socketType, currentFit());
    }

    String unmetRequirement(SocketType socketType) {
        return FrameworkSlots.unmetRequirement(socketType, currentFit());
    }

    private ShipProfile currentFit() {
        return FrameworkFit.profile(fleetMember.getHullSpec(), shipVariant, data());
    }

    HullSize hullSize() {
        return fleetMember.getHullSpec().getHullSize();
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

    List<SkillNode> respecPlan(SkillNode node) {
        return RespecPlan.of(data(), SkillTree.topology(), satisfiedRootId(), node);
    }

    boolean applyPendingPhantomConflicts() {
        if (!PhantomConflictWatch.hasPendingReverts() || !PhantomConflictWatch.applyPendingReverts(fleetMember.getId())) {
            return false;
        }
        refreshShipStats();
        return true;
    }

    private void refreshShipStats() {
        statsRevision++;
        ShipTreeSync.memberChanged(fleetMember, shipVariant);
        FleetWideEffects.markPhaseFieldStale();
        fleetMember.setStatUpdateNeeded(true);
        fleetMember.updateStats();
        variantHullModsRefreshed = true;
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

    private static CargoAPI playerCargo() {
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        return playerFleet == null ? null : playerFleet.getCargo();
    }

    private static double heldInCargo(String itemId) {
        CargoAPI cargo = playerCargo();
        return cargo == null ? 0f : cargo.getCommodityQuantity(itemId);
    }

    private static float refundOf(SkillItemCost refund, String itemId) {
        return refund != null && refund.itemId().equals(itemId) ? refund.quantity() : 0f;
    }

    private static void chargeItems(ShipSkillData skillData, String nodeId, SkillItemCost itemCost) {
        CargoAPI cargo = playerCargo();
        if (itemCost == null || cargo == null) {
            return;
        }
        cargo.removeCommodity(itemCost.itemId(), itemCost.quantity());
        skillData.recordItemCharge(nodeId, itemCost);
    }

    private static void refundItems(SkillItemCost charged) {
        CargoAPI cargo = playerCargo();
        if (charged != null && cargo != null) {
            cargo.addCommodity(charged.itemId(), charged.quantity());
        }
    }

    private void refreshVariantHullModsOnce() {
        if (variantHullModsRefreshed) {
            return;
        }
        variantHullModsRefreshed = true;
        fleetMember.setStatUpdateNeeded(true);
        fleetMember.updateStats();
        ShipTreeSync.syncVariant(fleetMember, shipVariant);
    }

    private boolean hasHullMod(String hullModId) {
        return InstalledHullMods.hasHullModOfItsOwn(shipVariant, hullModId) || SecondInCommandCompat.hasDeactivatedSMod(shipVariant, hullModId);
    }

    private static String bestOfTheBestName() {
        SkillSpecAPI spec = Global.getSettings().getSkillSpec(BEST_OF_THE_BEST_SKILL_ID);
        return spec == null ? BEST_OF_THE_BEST_SKILL_ID : spec.getName();
    }

    private String describe(AllocationGate.Verdict verdict, AllocationGate gate) {
        return switch (verdict.refusal()) {
            case NODE_CAP -> Translation.msg("node.block.nodeCap").arg("maxNodes", gate.budget().maxAllocatedNodes()).text();
            case OUT_OF_OP -> Translation.msg("node.block.outOfOp").arg("cost", gate.budget().opCostPerNode()).arg("free", gate.freeOp()).text();
            case INELIGIBLE -> describe(verdict.block());
            default -> null;
        };
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
            case LEARNED_CONFLICT -> block.detail() == null ? Translation.text("node.block.learnedHullConflict")
                    : Translation.msg("node.block.learnedConflict").arg("hullmod", HullModNames.displayName(block.detail())).text();
            case TYPE_CONFLICT -> Translation.msg("node.block.typeAllocated").arg("node", block.conflictingType().getDisplayName()).text();
            case EFFECT_BLOCK -> block.detail();
            case ITEM_COST -> Translation.msg("node.block.itemCost").arg("quantity", block.itemCost().formattedQuantity())
                    .arg("item", block.itemCost().commodityName()).arg("have", SkillItemCost.formatQuantity((float) heldInCargo(block.itemCost().itemId())))
                    .text();
        };
    }
}
