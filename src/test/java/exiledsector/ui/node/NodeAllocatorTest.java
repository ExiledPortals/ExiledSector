package exiledsector.ui.node;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
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
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.skills.skilleffect.FighterSkillEffect;
import exiledsector.skills.skilleffect.LogisticsSkillEffect;
import exiledsector.skills.template.StepVerdict;
import exiledsector.skills.template.TemplateStep;
import exiledsector.skills.unlock.UnlockCondition;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NodeAllocatorTest {

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<Global> globalMock;
    private MockedConstruction<SkillTreeHullMod> hullModConstruction;
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
        hullModConstruction = Mockito.mockConstruction(SkillTreeHullMod.class);

        SkillTree.getAllNodes().clear();
        SkillTree.getAllTypes().clear();
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
        hullModConstruction.close();
        globalMock.close();
        lunaSettingsMock.close();
        SkillTree.getAllNodes().clear();
        SkillTree.getAllTypes().clear();
    }

    private static SkillType.Builder type(String id, String name, SkillTier tier) {
        return new SkillType.Builder(id, name, "a.png", tier);
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

        assertFalse(allocatorStartingAt(root).canDeallocate(root));
    }

    @Test
    void anAllocatedTypeThatIsExclusiveBlocksAllocation() {
        data().chooseStartingRoot(root);
        data().allocate(frontShield, 0);

        assertEquals("Already have Front Shield allocated.", allocatorStartingAt(root).blockAllocationReason(omniShieldType));
    }

    @Test
    void anExclusivityDeclaredOnlyByTheAllocatedTypeStillBlocksAllocation() {
        SkillNode lister = register("lister_1", type("lister", "Lister", SkillTier.NOTABLE)
                .exclusiveSkillTypeIds(List.of("listed")).build(), "root_1");
        SkillType listed = type("listed", "Listed", SkillTier.NOTABLE).build();
        data().chooseStartingRoot(root);
        data().allocate(lister, 0);

        assertEquals("Already have Lister allocated.", allocatorStartingAt(root).blockAllocationReason(listed));
    }

    @Test
    void aNodeRestrictedToOtherHullSizesIsBlocked() {
        SkillType frigateOnly = type("frigate_only", "Frigate Only", SkillTier.SMALL).requiredHullSizes(List.of(HullSize.FRIGATE)).build();
        SkillType cruiserOrCapital = type("large_only", "Large Only", SkillTier.SMALL)
                .requiredHullSizes(List.of(HullSize.CRUISER, HullSize.CAPITAL_SHIP)).build();

        assertNotNull(allocatorStartingAt(root).blockAllocationReason(frigateOnly));
        assertNull(allocatorStartingAt(root).blockAllocationReason(cruiserOrCapital));
    }

    @Test
    void anInstalledExclusiveHullmodBlocksAllocation() {
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(spec.getDisplayName()).thenReturn("Heavy Armor");
        when(settings.getHullModSpec("heavyarmor")).thenReturn(spec);
        when(variant.hasHullMod("heavyarmor")).thenReturn(true);

        assertEquals("Ship already has Heavy Armor installed.", allocatorStartingAt(root).blockAllocationReason(armorType));
    }

    @Test
    void aNodeWhoseItemIsNotInCargoIsBlockedUntilItIs() {
        CommoditySpecAPI spec = mock(CommoditySpecAPI.class);
        when(spec.getName()).thenReturn("Alpha Core");
        when(settings.getCommoditySpec("alpha_core")).thenReturn(spec);
        NodeAllocator allocator = allocatorStartingAt(root);

        assertEquals("Requires 1 Alpha Core (have 0).", allocator.blockAllocationReason(coreSlot.getType()));
        when(cargo.getCommodityQuantity("alpha_core")).thenReturn(1f);
        assertNull(allocator.blockAllocationReason(coreSlot.getType()));
    }

    @Test
    void togglingANodeOnTakesItsItemAndTogglingItOffRefundsItRefreshingTheShipEachTime() {
        data().chooseStartingRoot(root);
        NodeAllocator allocator = allocatorStartingAt(root);

        assertTrue(allocator.toggle(coreSlot));
        verify(cargo).removeCommodity("alpha_core", 1f);
        assertTrue(allocator.toggle(coreSlot));
        verify(cargo).addCommodity("alpha_core", 1f);
        assertEquals(2, hullModConstruction.constructed().size());
        verify(member, times(2)).updateStats();
    }

    @Test
    void theFirstPaidNodeAfterChoosingTheRootReservesItsOpOnTheVariantBeingEdited() {
        HullModSpecAPI reserve = mock(HullModSpecAPI.class);
        when(settings.getHullModSpec("exiledSector_opSpent_0")).thenReturn(reserve);
        NodeAllocator allocator = allocatorStartingAt(root);
        assertTrue(allocator.chooseStartingRoot(root));

        assertTrue(allocator.toggle(frontShield));

        verify(reserve).setCruiserCost(SkillNodeOpCost.perNode(HullSize.CRUISER));
        verify(variant).addMod("exiledSector_opSpent_0");
    }

    @Test
    void aToggleThatChangesNothingTakesNoItemAndDoesNotRefreshTheShip() {
        data().chooseStartingRoot(root);

        assertFalse(allocatorStartingAt(root).toggle(unreachable));

        verifyNoInteractions(cargo);
        assertTrue(hullModConstruction.constructed().isEmpty());
    }

    @Test
    void aNodeBehindAnUnmetUnlockConditionIsLockedAndHidden() {
        SkillNode secret = register("secret_1", type("secret", "Secret", SkillTier.NOTABLE)
                .unlockConditions(List.of(UnlockCondition.minShipLevel(5))).build(), "root_1");
        NodeAllocator allocator = allocatorStartingAt(root);

        assertEquals(NodeAllocator.LOCKED_REASON, allocator.blockAllocationReason(secret.getType()));
        assertTrue(allocator.snapshot().isHidden(secret));
        assertFalse(allocator.snapshot().isHidden(frontShield));
    }

    @Test
    void aDeactivatedSecondInCommandSModOfAnExclusiveHullmodBlocksAllocation() {
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(spec.getDisplayName()).thenReturn("Heavy Armor");
        when(settings.getHullModSpec("heavyarmor")).thenReturn(spec);
        when(variant.hasTag("sc_inactive_smods_heavyarmor")).thenReturn(true);

        assertEquals("Ship has a deactivated Heavy Armor S-mod that Best of the Best will restore.",
                allocatorStartingAt(root).blockAllocationReason(armorType));
    }

    @Test
    void anEffectsOwnAllocationRuleBlocksTheNode() {
        when(member.getVariant()).thenReturn(variant);
        SkillType civilianOnly = type("civilian_only", "Civilian Only", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(LogisticsSkillEffect.REQUIRES_CIVILIAN_GRADE_HULL, 1f))).build();

        assertNotNull(allocatorStartingAt(root).blockAllocationReason(civilianOnly));
        when(variant.hasHullMod(HullMods.CIVGRADE)).thenReturn(true);
        assertNull(allocatorStartingAt(root).blockAllocationReason(civilianOnly));
    }

    private void fitWingsWithBays(int fittedWings, float bays) {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getNumFighterBays()).thenReturn(new MutableStat(bays));
        when(member.getStats()).thenReturn(stats);
        when(member.getVariant()).thenReturn(variant);
        when(variant.getFittedWings()).thenReturn(Collections.nCopies(fittedWings, "wing"));
    }

    @Test
    void anEffectsOwnDeallocationRuleKeepsTheNodeAllocatedUntilItIsSafeToRemove() {
        SkillNode hangar = register("hangar_1", type("hangar", "Hangar", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(FighterSkillEffect.FIGHTER_BAYS_FLAT, 1f))).build(), "root_1");
        data().chooseStartingRoot(root);
        data().allocate(hangar, 0);
        NodeAllocator allocator = allocatorStartingAt(root);

        fitWingsWithBays(2, 2f);
        assertNotNull(allocator.blockDeallocationReason(hangar));
        assertFalse(allocator.canDeallocate(hangar));

        fitWingsWithBays(1, 2f);
        assertNull(allocator.blockDeallocationReason(hangar));
        assertTrue(allocator.canDeallocate(hangar));
    }

    @Test
    void allocatingAnOptionChargesTheNodeRecordsTheChoiceAndRefreshesTheShip() {
        SkillType hangarOption = type("hangar_option", "Hangar Option", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(FighterSkillEffect.FIGHTER_BAYS_FLAT, 1f))).build();
        SkillTree.registerType(hangarOption);
        SkillNode choice = register("choice_1", type("choice", "Choice", SkillTier.NOTABLE)
                .optionalOptionIds(List.of("hangar_option")).build(), "root_1");
        data().chooseStartingRoot(root);
        NodeAllocator allocator = allocatorStartingAt(root);

        allocator.allocateOption(choice, hangarOption);

        assertTrue(data().isAllocated("choice_1"));
        assertEquals(hangarOption, choice.resolveEffectiveType(data()));
        assertEquals(SkillNodeOpCost.perNode(HullSize.CRUISER), data().getSpentOp(SkillNodeOpCost.perNode(HullSize.CRUISER)));
        assertEquals(1, hullModConstruction.constructed().size());
        fitWingsWithBays(2, 2f);
        assertNotNull(allocator.blockDeallocationReason(choice));
    }

    @Test
    void allocatingOnAShipThatHasNotHadItsHullModInstalledYetInstallsItOnTheShipAndTheRefitCopy() {
        data().chooseStartingRoot(root);

        assertTrue(allocatorStartingAt(root).toggle(frontShield));

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
