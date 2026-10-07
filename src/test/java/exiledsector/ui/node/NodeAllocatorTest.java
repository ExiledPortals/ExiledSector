package exiledsector.ui.node;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.effects.OpReserveParity;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocationGate;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.skills.template.StepVerdict;
import exiledsector.skills.template.TemplateStep;
import exiledsector.skills.unlock.UnlockCondition;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NodeAllocatorTest {

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<Global> globalMock;
    private SettingsAPI settings;
    private CargoAPI cargo;
    private FleetMemberAPI member;
    private ShipVariantAPI variant;
    private ShipVariantAPI shipVariant;

    private SkillNode root;
    private SkillNode frontShield;
    private SkillType omniShieldType;
    private SkillType armorType;
    private SkillNode coreSlot;
    private SkillNode unreachable;

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        globalMock = Mockito.mockStatic(Global.class);
        SectorAPI sector = mock(SectorAPI.class);
        Map<String, Object> persistentData = new HashMap<>();
        when(sector.getPersistentData()).thenReturn(persistentData);
        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        cargo = mock(CargoAPI.class);
        when(playerFleet.getCargo()).thenReturn(cargo);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        settings = mock(SettingsAPI.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        globalMock.when(Global::getSettings).thenReturn(settings);

        SkillTree.clearNodes();
        SkillTree.clearTypes();
        root = register("root_1", type("root", "Low Tech", SkillTier.ROOT).build());
        frontShield = register("frontshield_1", type("frontshield", "Front Shield", SkillTier.NOTABLE).build(), "root_1");
        omniShieldType = type("omnishield", "Omni Shield", SkillTier.NOTABLE).exclusiveSkillTypeIds(List.of("frontshield")).build();
        armorType = type("heavyarmor", "Heavy Armor", SkillTier.NOTABLE).exclusiveHullModIds(List.of("heavyarmor")).build();
        coreSlot = register("core_1", type("core", "Core Slot", SkillTier.NOTABLE)
                .itemCost(new SkillItemCost("alpha_core", 1f)).build(), "root_1");
        SkillNode gate = register("gate_1", type("gate", "Gate", SkillTier.SMALL).build(), "frontshield_1");
        unreachable = register("beyond_1", type("beyond", "Beyond", SkillTier.SMALL).build(), gate.getId());

        member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn("ship-1");
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHullSize()).thenReturn(HullSize.CRUISER);
        when(hullSpec.getOrdnancePoints(any())).thenReturn(100);
        when(member.getHullSpec()).thenReturn(hullSpec);
        variant = mock(ShipVariantAPI.class);
        when(variant.computeOPCost(any())).thenReturn(80);
        shipVariant = mock(ShipVariantAPI.class);
        when(shipVariant.getSource()).thenReturn(VariantSource.REFIT);
        when(member.getVariant()).thenReturn(shipVariant);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        lunaSettingsMock.close();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    private static SkillType.Builder type(String id, String name, SkillTier tier) {
        return new SkillType.Builder(id, name, "a.png", tier);
    }

    private static String blockReason(NodeAllocator allocator, SkillType type) {
        return allocator.snapshot().allocationRefusalReason(new SkillNode("unplaced_" + type.getId(), type, List.of(), 0f, 0f), null);
    }

    private static SkillNode register(String id, SkillType type, String... connectedTo) {
        SkillNode node = new SkillNode(id, type, List.of(connectedTo), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    private NodeAllocator allocatorStartingAt(SkillNode startingRoot) {
        return new NodeAllocator(member, variant, () -> startingRoot);
    }

    private static ShipSkillData data() {
        return ShipSkillDataManager.get("ship-1");
    }

    @Test
    void theStartingRootIsFreeAndEveryOtherNodeCostsThePerNodeOp() {
        NodeAllocator.Snapshot snapshot = allocatorStartingAt(root).snapshot();

        assertEquals(0, snapshot.opCostFor(root));
        assertEquals(SkillNodeOpCost.perNode(HullSize.CRUISER), snapshot.opCostFor(frontShield));
    }

    private void reserveOnVariant(int cost) {
        HullModSpecAPI reserve = mock(HullModSpecAPI.class);
        when(reserve.getCostFor(any())).thenReturn(cost);
        when(settings.getHullModSpec("exiledSector_opSpent_0")).thenReturn(reserve);
        when(variant.getHullMods()).thenReturn(List.of("exiledSector_opSpent_0"));
    }

    @Test
    void theBudgetIsTheShipsFreeOpPlusTheOpItsReserveAlreadyHolds() {
        data().chooseStartingRoot(root);
        data().allocate(frontShield, 3);
        reserveOnVariant(3);

        assertEquals(100 - 80 + 3, allocatorStartingAt(root).snapshot().totalOpBudget());
    }

    @Test
    void theBudgetCheckReportsAReserveThatDisagreesWithThePaidNodes() {
        data().chooseStartingRoot(root);
        data().allocate(frontShield, 3);
        reserveOnVariant(1);
        Logger logger = mock(Logger.class);

        try (MockedStatic<Logger> loggerMock = Mockito.mockStatic(Logger.class)) {
            loggerMock.when(() -> Logger.getLogger(OpReserveParity.class)).thenReturn(logger);
            allocatorStartingAt(root).snapshot();
        }

        verify(logger).warn(contains("while allocating nodes"));
        verify(logger).warn(contains("its paid nodes cost 3 OP but the variant reserves 1 OP"));
    }

    @Test
    void aMissingReserveIsNeverCountedAsFreeOpForMoreNodes() {
        data().chooseStartingRoot(root);
        data().allocate(frontShield, 3);
        when(variant.computeOPCost(any())).thenReturn(97);

        NodeAllocator.Snapshot snapshot = allocatorStartingAt(root).snapshot();

        assertEquals(100 - 97, snapshot.totalOpBudget());
        assertFalse(snapshot.canAllocate(coreSlot));
    }

    @Test
    void theSnapshotKnowsWhichNodesCanBeAllocatedNext() {
        data().chooseStartingRoot(root);

        NodeAllocator.Snapshot snapshot = allocatorStartingAt(root).snapshot();

        assertTrue(snapshot.canAllocate(frontShield));
        assertFalse(snapshot.canAllocate(unreachable));
    }

    @Test
    void theStartingRootCanNeverBeDeallocated() {
        data().chooseStartingRoot(root);

        assertEquals(AllocationGate.Refusal.STARTING_ROOT, allocatorStartingAt(root).snapshot().gate().deallocation(root).refusal());
    }

    @Test
    void anAllocatedTypeThatIsExclusiveBlocksAllocation() {
        data().chooseStartingRoot(root);
        data().allocate(frontShield, 0);

        assertEquals("Already have Front Shield allocated.", blockReason(allocatorStartingAt(root), omniShieldType));
    }

    @Test
    void anExclusivityDeclaredOnlyByTheAllocatedTypeStillBlocksAllocation() {
        SkillNode lister = register("lister_1", type("lister", "Lister", SkillTier.NOTABLE)
                .exclusiveSkillTypeIds(List.of("listed")).build(), "root_1");
        SkillType listed = type("listed", "Listed", SkillTier.NOTABLE).build();
        data().chooseStartingRoot(root);
        data().allocate(lister, 0);

        assertEquals("Already have Lister allocated.", blockReason(allocatorStartingAt(root), listed));
    }

    @Test
    void aNodeRestrictedToOtherHullSizesIsBlocked() {
        SkillType frigateOnly = type("frigate_only", "Frigate Only", SkillTier.SMALL).requiredHullSizes(List.of(HullSize.FRIGATE)).build();
        SkillType cruiserOrCapital = type("large_only", "Large Only", SkillTier.SMALL)
                .requiredHullSizes(List.of(HullSize.CRUISER, HullSize.CAPITAL_SHIP)).build();

        assertNotNull(blockReason(allocatorStartingAt(root), frigateOnly));
        assertNull(blockReason(allocatorStartingAt(root), cruiserOrCapital));
    }

    @Test
    void anInstalledExclusiveHullmodBlocksAllocation() {
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(spec.getDisplayName()).thenReturn("Heavy Armor");
        when(settings.getHullModSpec("heavyarmor")).thenReturn(spec);
        when(variant.hasHullMod("heavyarmor")).thenReturn(true);

        assertEquals("Ship already has Heavy Armor installed.", blockReason(allocatorStartingAt(root), armorType));
    }

    @Test
    void aHullModTheSkillTreePlacedItselfNeverBlocksANodeThatStandsInForIt() {
        when(variant.hasHullMod("magazines")).thenReturn(true);
        when(variant.hasTag("exiledSector_installed_magazines")).thenReturn(true);
        SkillType magazines = type("magazines", "Expanded Magazines", SkillTier.NOTABLE)
                .exclusiveHullModIds(List.of("magazines")).phantomHullModIds(List.of("magazines")).build();

        assertNull(blockReason(allocatorStartingAt(root), magazines));
    }

    @Test
    void anOptionalNodesContainerRulesApplyToTheOptionChosen() {
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(spec.getDisplayName()).thenReturn("Heavy Armor");
        when(settings.getHullModSpec("heavyarmor")).thenReturn(spec);
        when(variant.hasHullMod("heavyarmor")).thenReturn(true);
        SkillType plain = type("plain", "Plain", SkillTier.SMALL).build();
        SkillTree.registerType(plain);
        SkillNode slot = register("slot_1", type("slot", "Slot", SkillTier.SMALL).exclusiveHullModIds(List.of("heavyarmor"))
                .optionalOptionIds(List.of("plain")).build(), "root_1");

        assertEquals("Ship already has Heavy Armor installed.", allocatorStartingAt(root).snapshot().allocationRefusalReason(slot, plain));
    }

    @Test
    void aNodeWhoseItemIsNotInCargoIsBlockedUntilItIs() {
        CommoditySpecAPI spec = mock(CommoditySpecAPI.class);
        when(spec.getName()).thenReturn("Alpha Core");
        when(settings.getCommoditySpec("alpha_core")).thenReturn(spec);
        NodeAllocator allocator = allocatorStartingAt(root);

        assertEquals("Requires 1 Alpha Core (have 0).", allocator.snapshot().allocationRefusalReason(coreSlot, null));
        assertFalse(allocator.snapshot().canAllocate(coreSlot));
        when(cargo.getCommodityQuantity("alpha_core")).thenReturn(1f);
        assertNull(allocator.snapshot().allocationRefusalReason(coreSlot, null));
        assertTrue(allocator.snapshot().canAllocate(coreSlot));
    }

    @Test
    void allocatingANodeTakesItsItemAndRemovingItRefundsItRefreshingTheShipEachTime() {
        data().chooseStartingRoot(root);
        when(cargo.getCommodityQuantity("alpha_core")).thenReturn(1f);
        NodeAllocator allocator = allocatorStartingAt(root);
        allocator.snapshot();
        clearInvocations(member);

        assertTrue(allocator.allocate(coreSlot, null, allocator.snapshot()));
        verify(cargo).removeCommodity("alpha_core", 1f);
        assertTrue(allocator.deallocate(coreSlot, allocator.snapshot()));
        verify(cargo).addCommodity("alpha_core", 1f);
        verify(member, times(2)).updateStats();
        assertNull(data().itemCharge("core_1"));
    }

    @Test
    void removingAnItemNodeThatWasNeverChargedRefundsNothing() {
        data().chooseStartingRoot(root);
        data().allocate(coreSlot, 0);
        NodeAllocator allocator = allocatorStartingAt(root);

        assertTrue(allocator.deallocate(coreSlot, allocator.snapshot()));

        verify(cargo, never()).addCommodity(anyString(), anyFloat());
    }

    private SkillNode optionalNodeWithPricedOptions() {
        SkillTree.registerType(type("scanner", "Scanner", SkillTier.SMALL).itemCost(new SkillItemCost("alpha_core", 1f)).build());
        SkillTree.registerType(type("sensor", "Sensor", SkillTier.SMALL).itemCost(new SkillItemCost("beta_core", 2f)).build());
        return register("picker_1", type("picker", "Picker", SkillTier.SMALL).optionalOptionIds(List.of("scanner", "sensor")).build(), "root_1");
    }

    @Test
    void anOptionsItemCostIsChargedWhenChosenAndRefundedWhenTheNodeIsRemoved() {
        SkillNode picker = optionalNodeWithPricedOptions();
        data().chooseStartingRoot(root);
        when(cargo.getCommodityQuantity("alpha_core")).thenReturn(1f);
        NodeAllocator allocator = allocatorStartingAt(root);

        assertTrue(allocator.allocate(picker, SkillTree.getType("scanner"), allocator.snapshot()));
        verify(cargo).removeCommodity("alpha_core", 1f);
        assertEquals(new SkillItemCost("alpha_core", 1f), data().itemCharge("picker_1"));

        assertTrue(allocator.deallocate(picker, allocator.snapshot()));
        verify(cargo).addCommodity("alpha_core", 1f);
    }

    @Test
    void anOptionWhoseItemIsNotInCargoCannotBeChosen() {
        SkillNode picker = optionalNodeWithPricedOptions();
        data().chooseStartingRoot(root);
        when(cargo.getCommodityQuantity("alpha_core")).thenReturn(1f);
        CommoditySpecAPI spec = mock(CommoditySpecAPI.class);
        when(spec.getName()).thenReturn("Beta Core");
        when(settings.getCommoditySpec("beta_core")).thenReturn(spec);
        NodeAllocator allocator = allocatorStartingAt(root);
        NodeAllocator.Snapshot snapshot = allocator.snapshot();

        assertTrue(snapshot.canAllocate(picker));
        assertEquals("Requires 2 Beta Core (have 0).", snapshot.optionRefusalReason(picker, SkillTree.getType("sensor")));
        assertFalse(allocator.allocate(picker, SkillTree.getType("sensor"), snapshot));
        assertFalse(data().isAllocated("picker_1"));
    }

    @Test
    void switchingOptionsRefundsTheOldOptionsItemAndChargesTheNewOne() {
        SkillNode picker = optionalNodeWithPricedOptions();
        data().chooseStartingRoot(root);
        when(cargo.getCommodityQuantity("alpha_core")).thenReturn(1f);
        when(cargo.getCommodityQuantity("beta_core")).thenReturn(2f);
        NodeAllocator allocator = allocatorStartingAt(root);
        assertTrue(allocator.allocate(picker, SkillTree.getType("scanner"), allocator.snapshot()));

        assertTrue(allocator.switchOption(picker, SkillTree.getType("sensor"), allocator.snapshot()));

        verify(cargo).addCommodity("alpha_core", 1f);
        verify(cargo).removeCommodity("beta_core", 2f);
        assertEquals(new SkillItemCost("beta_core", 2f), data().itemCharge("picker_1"));
        assertEquals(SkillTree.getType("sensor"), picker.resolveEffectiveType(data()));
    }

    @Test
    void switchingBetweenTwoMutuallyExclusiveOptionsDoesNotConflictWithTheOptionBeingReplaced() {
        SkillTree.registerType(type("front_mode", "Front Mode", SkillTier.SMALL).exclusiveSkillTypeIds(List.of("omni_mode")).build());
        SkillTree.registerType(type("omni_mode", "Omni Mode", SkillTier.SMALL).exclusiveSkillTypeIds(List.of("front_mode")).build());
        SkillNode mode = register("mode_1", type("mode", "Mode", SkillTier.SMALL).optionalOptionIds(List.of("front_mode", "omni_mode")).build(),
                "root_1");
        data().chooseStartingRoot(root);
        data().selectOption(mode, SkillTree.getType("front_mode"), 0);
        NodeAllocator allocator = allocatorStartingAt(root);

        AllocationGate gate = allocator.snapshot().gate();
        assertTrue(gate.optionSwitch(mode, SkillTree.getType("omni_mode")).allowed());
        assertEquals(AllocationGate.Refusal.SAME_OPTION, gate.optionSwitch(mode, SkillTree.getType("front_mode")).refusal());
    }

    @Test
    void anOptionalContainerBehindAnUnmetUnlockConditionIsLockedWhicheverOptionIsChosen() {
        SkillTree.registerType(type("open_option", "Open Option", SkillTier.SMALL).build());
        SkillNode sealed = register("sealed_1", type("sealed", "Sealed", SkillTier.SMALL).optionalOptionIds(List.of("open_option"))
                .unlockConditions(List.of(UnlockCondition.minShipLevel(5))).build(), "root_1");
        data().chooseStartingRoot(root);

        AllocationGate.Verdict verdict = allocatorStartingAt(root).snapshot().gate().allocation(sealed, SkillTree.getType("open_option"));

        assertEquals(NodeEligibility.Kind.LOCKED, verdict.block().kind());
    }

    @Test
    void aNeighbourThatTheEligibilityRulesRefuseDoesNotCountAsAllocatable() {
        SkillNode frigateOnly = register("frigate_only_1", type("frigate_only", "Frigate Only", SkillTier.SMALL)
                .requiredHullSizes(List.of(HullSize.FRIGATE)).build(), "root_1");
        data().chooseStartingRoot(root);
        NodeAllocator allocator = allocatorStartingAt(root);

        assertFalse(allocator.snapshot().canAllocate(frigateOnly));
        assertEquals("This node can't be allocated on this hull size.", allocator.snapshot().refusalReason(frigateOnly));
        assertFalse(allocator.allocate(frigateOnly, null, allocator.snapshot()));
    }

    @Test
    void runningOutOfOpIsExplainedButAnUnconnectedNodeIsNot() {
        data().chooseStartingRoot(root);
        when(variant.computeOPCost(any())).thenReturn(100);

        String outOfOp = allocatorStartingAt(root).snapshot().refusalReason(frontShield);

        assertEquals("Not enough ordnance points: needs " + SkillNodeOpCost.perNode(HullSize.CRUISER) + " OP, 0 OP free.", outOfOp);
        assertNull(allocatorStartingAt(root).snapshot().refusalReason(unreachable));
    }

    @Test
    void theSnapshotHandsOutOneMemoisedVerdictPerNode() {
        data().chooseStartingRoot(root);
        AllocationGate gate = allocatorStartingAt(root).snapshot().gate();

        assertSame(gate.allocation(frontShield), gate.allocation(frontShield));
        assertSame(gate.deallocation(root), gate.deallocation(root));
    }

    @Test
    void theFirstPaidNodeAfterChoosingTheRootReservesItsOpOnTheVariantBeingEdited() {
        HullModSpecAPI reserve = mock(HullModSpecAPI.class);
        when(settings.getHullModSpec("exiledSector_opSpent_0")).thenReturn(reserve);
        NodeAllocator allocator = allocatorStartingAt(root);
        assertTrue(allocator.chooseStartingRoot(root));

        assertTrue(allocator.allocate(frontShield, null, allocator.snapshot()));

        verify(reserve).setCruiserCost(SkillNodeOpCost.perNode(HullSize.CRUISER));
        verify(variant).addMod("exiledSector_opSpent_0");
    }

    @Test
    void aRefusedAllocationTakesNoItemAndDoesNotRefreshTheShip() {
        data().chooseStartingRoot(root);
        NodeAllocator allocator = allocatorStartingAt(root);
        allocator.snapshot();
        clearInvocations(member);

        assertFalse(allocator.allocate(unreachable, null, allocator.snapshot()));

        verifyNoInteractions(cargo);
        verify(member, never()).updateStats();
    }

    @Test
    void aNodeBehindAnUnmetUnlockConditionIsLockedAndHidden() {
        SkillNode secret = register("secret_1", type("secret", "Secret", SkillTier.NOTABLE)
                .unlockConditions(List.of(UnlockCondition.minShipLevel(5))).build(), "root_1");
        NodeAllocator allocator = allocatorStartingAt(root);

        assertEquals("Unidentified - explore the sector to discover this node.", allocator.snapshot().allocationRefusalReason(secret, null));
        assertNull(allocator.snapshot().refusalReason(secret));
        assertTrue(allocator.snapshot().isHidden(secret));
        assertFalse(allocator.snapshot().isHidden(frontShield));
    }

    @Test
    void aDeactivatedSecondInCommandSModOfAnExclusiveHullmodBlocksAllocation() {
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(spec.getDisplayName()).thenReturn("Heavy Armor");
        when(settings.getHullModSpec("heavyarmor")).thenReturn(spec);
        when(variant.hasTag("sc_inactive_smods_heavyarmor")).thenReturn(true);
        SkillSpecAPI skill = mock(SkillSpecAPI.class);
        when(skill.getName()).thenReturn("Best of the Best");
        when(settings.getSkillSpec("best_of_the_best")).thenReturn(skill);

        assertEquals("Ship has a deactivated Heavy Armor S-mod that Best of the Best will restore.",
                blockReason(allocatorStartingAt(root), armorType));
    }

    @Test
    void aHullRequirementTagBlocksTheNodeUntilTheHullMeetsIt() {
        when(member.getVariant()).thenReturn(variant);
        SkillType civilianOnly = type("civilian_only", "Civilian Only", SkillTier.SMALL)
                .tags(List.of("req_civilian_hull")).build();

        assertNotNull(blockReason(allocatorStartingAt(root), civilianOnly));
        when(variant.hasHullMod(HullMods.CIVGRADE)).thenReturn(true);
        assertNull(blockReason(allocatorStartingAt(root), civilianOnly));
    }

    @Test
    void aRespecPlanListsEveryDependentBeforeTheNodeItself() {
        SkillNode hangar = register("hangar_1", type("hangar", "Hangar", SkillTier.NOTABLE).build(), "root_1");
        data().chooseStartingRoot(root);
        data().allocate(hangar, 0);

        assertEquals(List.of(hangar, root), allocatorStartingAt(root).respecPlan(root));
    }

    @Test
    void allocatingAnOptionChargesTheNodeRecordsTheChoiceAndRefreshesTheShip() {
        SkillType hangarOption = type("hangar_option", "Hangar Option", SkillTier.NOTABLE).build();
        SkillTree.registerType(hangarOption);
        SkillNode choice = register("choice_1", type("choice", "Choice", SkillTier.NOTABLE)
                .optionalOptionIds(List.of("hangar_option")).build(), "root_1");
        data().chooseStartingRoot(root);
        NodeAllocator allocator = allocatorStartingAt(root);
        allocator.snapshot();
        clearInvocations(member);

        assertTrue(allocator.allocate(choice, hangarOption, allocator.snapshot()));

        assertTrue(data().isAllocated("choice_1"));
        assertEquals(hangarOption, choice.resolveEffectiveType(data()));
        assertEquals(SkillNodeOpCost.perNode(HullSize.CRUISER), data().getSpentOp(SkillNodeOpCost.perNode(HullSize.CRUISER)));
        verify(member).updateStats();
    }

    @Test
    void anOptionalNodeCannotBeAllocatedWithoutAnOption() {
        SkillTree.registerType(type("only_option", "Only Option", SkillTier.SMALL).build());
        SkillNode choice = register("choice_1", type("choice", "Choice", SkillTier.SMALL).optionalOptionIds(List.of("only_option")).build(),
                "root_1");
        data().chooseStartingRoot(root);
        NodeAllocator allocator = allocatorStartingAt(root);

        assertTrue(allocator.snapshot().canAllocate(choice));
        assertFalse(allocator.allocate(choice, null, allocator.snapshot()));
    }

    @Test
    void allocatingOnAShipThatHasNotHadItsHullModInstalledYetInstallsItOnTheShipAndTheRefitCopy() {
        data().chooseStartingRoot(root);

        NodeAllocator allocator = allocatorStartingAt(root);
        assertTrue(allocator.allocate(frontShield, null, allocator.snapshot()));

        verify(shipVariant).addPermaMod(SkillTreeHullMod.ID);
        verify(variant).addPermaMod(SkillTreeHullMod.ID);
    }

    @Test
    void choosingTheStartingRootOnlyWorksOnce() {
        NodeAllocator allocator = allocatorStartingAt(null);

        assertTrue(allocator.chooseStartingRoot(root));
        assertFalse(allocator.chooseStartingRoot(root));
        assertTrue(data().isAllocated("root_1"));
    }

    @Test
    void theStartingRootCanBeUnchosenUntilAnotherNodeIsAllocatedAndUnchoosingRefreshesTheShip() {
        data().chooseStartingRoot(root);
        NodeAllocator allocator = allocatorStartingAt(root);

        assertTrue(allocator.canUnchooseStartingRoot(root));
        assertFalse(allocator.canUnchooseStartingRoot(frontShield));
        assertTrue(allocator.unchooseStartingRoot(root));

        assertFalse(data().isAllocated("root_1"));
        verify(member).updateStats();
    }

    @Test
    void theStartingRootIsLockedOnceAnotherNodeIsAllocated() {
        data().chooseStartingRoot(root);
        data().allocate(frontShield, 0);
        NodeAllocator allocator = allocatorStartingAt(root);

        assertFalse(allocator.canUnchooseStartingRoot(root));
        assertFalse(allocator.unchooseStartingRoot(root));
        assertTrue(data().isAllocated("root_1"));
    }

    private TemplateStepExecutor executor(List<SkillNode> allocated) {
        data().chooseStartingRoot(root);
        NodeAllocator allocator = allocatorStartingAt(root);
        return new TemplateStepExecutor(allocator, allocator::snapshot, allocated::add);
    }

    @Test
    void aTemplateStepAllocatesItsNodeTheWayAClickWould() {
        List<SkillNode> allocated = new ArrayList<>();

        StepVerdict verdict = executor(allocated).attempt(new TemplateStep("frontshield_1", null));

        assertEquals(StepVerdict.ALLOCATE, verdict);
        assertTrue(data().isAllocated("frontshield_1"));
        assertEquals(List.of(frontShield), allocated);
    }

    @Test
    void aTemplateStepNeverSpendsCargoOnANodeWithAnItemCost() {
        when(cargo.getCommodityQuantity("alpha_core")).thenReturn(5f);

        assertEquals(StepVerdict.ITEM_COST, executor(new ArrayList<>()).attempt(new TemplateStep("core_1", null)));
        assertFalse(data().isAllocated("core_1"));
        verify(cargo, never()).removeCommodity(anyString(), anyFloat());
    }

    @Test
    void aTemplateStepSkipsNodesThatAreUnreachableOrBlocked() {
        register("frigate_only_1", type("frigate_only", "Frigate Only", SkillTier.SMALL)
                .requiredHullSizes(List.of(HullSize.FRIGATE)).build(), "root_1");
        TemplateStepExecutor executor = executor(new ArrayList<>());

        assertEquals(StepVerdict.NOT_ALLOCATABLE, executor.attempt(new TemplateStep("beyond_1", null)));
        assertEquals(StepVerdict.BLOCKED, executor.attempt(new TemplateStep("frigate_only_1", null)));
        assertEquals(StepVerdict.ALREADY_ALLOCATED, executor.attempt(new TemplateStep("root_1", null)));
    }

    @Test
    void aTemplateStepOnAnOptionalNodeAllocatesTheTemplatesOption() {
        SkillType option = type("hull_option", "Hull Option", SkillTier.SMALL).build();
        SkillTree.registerType(option);
        SkillNode slot = register("slot_1", type("slot", "Slot", SkillTier.SMALL).optionalOptionIds(List.of("hull_option")).build(), "root_1");

        assertEquals(StepVerdict.ALLOCATE, executor(new ArrayList<>()).attempt(new TemplateStep("slot_1", "hull_option")));
        assertEquals(option, slot.resolveEffectiveType(data()));
    }

    @Test
    void pointsRunOutWhenTheNextNodeWouldNotFitTheShipsFreeOp() {
        TemplateStepExecutor executor = executor(new ArrayList<>());
        assertTrue(executor.hasPointsLeft());

        when(variant.computeOPCost(any())).thenReturn(99);
        assertFalse(executor.hasPointsLeft());
    }
}
