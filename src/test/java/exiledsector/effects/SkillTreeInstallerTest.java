package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.compat.SecondInCommandCompat;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillTreeInstallerTest {

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private FleetDataAPI fleetData;

    @BeforeEach
    void setUp() {
        Map<String, Object> persistentData = new HashMap<>();
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);

        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        fleetData = mock(FleetDataAPI.class);
        when(playerFleet.getFleetData()).thenReturn(fleetData);
        MutableFleetStatsAPI fleetStats = mock(MutableFleetStatsAPI.class);
        when(playerFleet.getStats()).thenReturn(fleetStats);
        StatBonus detectedRangeMod = mock(StatBonus.class);
        when(fleetStats.getDetectedRangeMod()).thenReturn(detectedRangeMod);
        when(playerFleet.isTransponderOn()).thenReturn(true);

        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(fleetData.getMembersListCopy()).thenReturn(List.of());

        SkillTree.getAllNodes().clear();
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        SkillTree.getAllNodes().clear();
    }

    private static FleetMemberAPI mockMember(String id, boolean hasHullMod) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(member.getId()).thenReturn(id);
        when(member.getVariant()).thenReturn(variant);
        when(member.getHullSpec()).thenReturn(hullSpec);
        when(member.getStats()).thenReturn(stats);
        when(stats.getFleetMember()).thenReturn(member);
        when(hullSpec.getHullSize()).thenReturn(HullSize.FRIGATE);
        when(variant.hasHullMod(SkillTreeHullMod.ID)).thenReturn(hasHullMod);
        when(variant.getSource()).thenReturn(VariantSource.REFIT);
        return member;
    }

    @Test
    void installsIntoAShipSpecificCopyWhenTheVariantIsNotAlreadyARefitVariant() {
        FleetMemberAPI member = mockMember("stock-ship", false);
        ShipVariantAPI shared = member.getVariant();
        ShipVariantAPI copy = mock(ShipVariantAPI.class);
        when(shared.getSource()).thenReturn(VariantSource.STOCK);
        when(shared.clone()).thenReturn(copy);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        new SkillTreeInstaller().advance(0.01f);

        verify(copy).setSource(VariantSource.REFIT);
        verify(member).setVariant(copy, false, true);
        verify(copy).addPermaMod(SkillTreeHullMod.ID);
        verify(shared, never()).addPermaMod(SkillTreeHullMod.ID);
    }

    @Test
    void installsDirectlyIntoAnAlreadyShipSpecificRefitVariant() {
        FleetMemberAPI member = mockMember("refit-ship", false);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        new SkillTreeInstaller().advance(0.01f);

        verify(member.getVariant(), never()).clone();
        verify(member, never()).setVariant(any(), anyBoolean(), anyBoolean());
        verify(member.getVariant()).addPermaMod(SkillTreeHullMod.ID);
    }

    @Test
    void isNeverDone() {
        assertFalse(new SkillTreeInstaller().isDone());
    }

    @Test
    void doesNotRunWhilePaused() {
        assertFalse(new SkillTreeInstaller().runWhilePaused());
    }

    @Test
    void firstAdvanceChecksImmediatelyEvenWithATinyAmount() {
        SkillTreeInstaller installer = new SkillTreeInstaller();

        installer.advance(0.01f);

        verify(fleetData, times(1)).getMembersListCopy();
    }

    @Test
    void doesNotCheckAgainUntilTheIntervalElapsesAfterACheck() {
        SkillTreeInstaller installer = new SkillTreeInstaller();
        installer.advance(0.01f);

        installer.advance(0.5f);
        verify(fleetData, times(1)).getMembersListCopy();

        installer.advance(0.5f);
        verify(fleetData, times(2)).getMembersListCopy();
    }

    @Test
    void doesNothingWhenThereIsNoPlayerFleet() {
        when(sector.getPlayerFleet()).thenReturn(null);
        SkillTreeInstaller installer = new SkillTreeInstaller();

        installer.advance(0.01f);

        verify(fleetData, never()).getMembersListCopy();
    }

    @Test
    void addsTheHullModOnlyToShipsMissingIt() {
        FleetMemberAPI hasIt = mockMember("ship-with-mod", true);
        FleetMemberAPI missingIt = mockMember("ship-without-mod", false);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(hasIt, missingIt));

        new SkillTreeInstaller().advance(0.01f);

        verify(hasIt.getVariant(), never()).addPermaMod(SkillTreeHullMod.ID);
        verify(missingIt.getVariant()).addPermaMod(SkillTreeHullMod.ID);
    }

    @Test
    void movesItsHullModBehindTheSecondInCommandControllerSoItAppliesAfterIt() {
        FleetMemberAPI member = mockMember("ship", true);
        ShipVariantAPI variant = member.getVariant();
        when(variant.hasHullMod(SecondInCommandCompat.CONTROLLER_HULLMOD_ID)).thenReturn(true);
        when(variant.getHullMods()).thenReturn(List.of(SkillTreeHullMod.ID, SecondInCommandCompat.CONTROLLER_HULLMOD_ID));
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        new SkillTreeInstaller().advance(0.01f);

        InOrder order = inOrder(variant);
        order.verify(variant).removePermaMod(SkillTreeHullMod.ID);
        order.verify(variant).addPermaMod(SkillTreeHullMod.ID);
    }

    @Test
    void movesItsHullModBehindAnyHullModInstalledAfterItSoCrewChangesAreCounted() {
        FleetMemberAPI member = mockMember("ship", true);
        ShipVariantAPI variant = member.getVariant();
        when(variant.getHullMods()).thenReturn(List.of("hardened_shields", SkillTreeHullMod.ID, "additional_berthing"));
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        new SkillTreeInstaller().advance(0.01f);

        InOrder order = inOrder(variant);
        order.verify(variant).removePermaMod(SkillTreeHullMod.ID);
        order.verify(variant).addPermaMod(SkillTreeHullMod.ID);
        verify(member).setStatUpdateNeeded(true);
    }

    @Test
    void leavesItsHullModInPlaceWhenItIsAlreadyLast() {
        FleetMemberAPI member = mockMember("ship", true);
        ShipVariantAPI variant = member.getVariant();
        when(variant.getHullMods()).thenReturn(List.of("hardened_shields", "additional_berthing", SkillTreeHullMod.ID));
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        new SkillTreeInstaller().advance(0.01f);

        verify(variant, never()).removePermaMod(SkillTreeHullMod.ID);
        verify(variant, never()).addPermaMod(SkillTreeHullMod.ID);
    }

    @Test
    void leavesItsHullModInPlaceWhenItAlreadyAppliesAfterTheSecondInCommandController() {
        FleetMemberAPI member = mockMember("ship", true);
        ShipVariantAPI variant = member.getVariant();
        when(variant.hasHullMod(SecondInCommandCompat.CONTROLLER_HULLMOD_ID)).thenReturn(true);
        when(variant.getHullMods()).thenReturn(List.of(SecondInCommandCompat.CONTROLLER_HULLMOD_ID, SkillTreeHullMod.ID));
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        new SkillTreeInstaller().advance(0.01f);

        verify(variant, never()).removePermaMod(SkillTreeHullMod.ID);
        verify(variant, never()).addPermaMod(SkillTreeHullMod.ID);
    }

    @Test
    void leavesApplyingEffectsToTheEnginesStatsRebuild() {
        SkillType hullType = new SkillType.Builder("hull", "Hull", "graphics/hullmods/reinforced_bulkheads.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .build();
        SkillNode hullNode = new SkillNode("hull_1", hullType, List.of(), 0f, 0f);
        SkillTree.register(hullNode);

        FleetMemberAPI member = mockMember("ship-a", true);
        ShipSkillDataManager.get("ship-a").allocate(hullNode, 1);
        StatBonus hullBonus = mock(StatBonus.class);
        when(member.getStats().getHullBonus()).thenReturn(hullBonus);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        new SkillTreeInstaller().advance(0.01f);

        verify(hullBonus, never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void requestsOneFleetSyncAfterLoadSoStatsAreRebuiltWithTheLoadedTrees() {
        FleetMemberAPI member = mockMember("ship-a", true);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));
        SkillTreeInstaller installer = new SkillTreeInstaller();

        installer.advance(0.01f);
        installer.advance(1f);

        verify(fleetData, times(1)).setSyncNeeded();
    }

    @Test
    void requestsAFleetSyncWhenItInstallsItsHullModOnANewShip() {
        FleetMemberAPI veteran = mockMember("veteran", true);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(veteran));
        SkillTreeInstaller installer = new SkillTreeInstaller();
        installer.advance(0.01f);

        FleetMemberAPI newcomer = mockMember("newcomer", false);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(veteran, newcomer));
        installer.advance(1f);

        verify(newcomer.getVariant()).addPermaMod(SkillTreeHullMod.ID);
        verify(newcomer).setStatUpdateNeeded(true);
        verify(fleetData, times(2)).setSyncNeeded();
    }

    private static FleetMemberAPI recoveredNpc(String id, Set<String> hullMods, List<String> tags) {
        FleetMemberAPI member = mockMember(id, false);
        ShipVariantAPI variant = member.getVariant();
        when(variant.hasHullMod(anyString())).thenAnswer(invocation -> hullMods.contains((String) invocation.getArgument(0)));
        doAnswer(invocation -> hullMods.add(invocation.getArgument(0))).when(variant).addPermaMod(anyString());
        doAnswer(invocation -> hullMods.remove((String) invocation.getArgument(0))).when(variant).removeMod(anyString());
        when(variant.getTags()).thenAnswer(invocation -> new ArrayList<>(tags));
        doAnswer(invocation -> tags.remove((String) invocation.getArgument(0))).when(variant).removeTag(anyString());
        return member;
    }

    private static void registerNpcTreeNodes() {
        SkillTree.register(new SkillNode("root_1", new SkillType.Builder("root", "Root", "a.png", SkillTier.ROOT).build(),
                List.of(), 0f, 0f));
        SkillTree.register(new SkillNode("a_1", new SkillType.Builder("a", "A", "a.png", SkillTier.SMALL).build(),
                List.of("root_1"), 0f, 0f));
    }

    @Test
    void aRecoveredNpcShipKeepsItsTreeAsItsOwnSavedTree() {
        registerNpcTreeNodes();
        Set<String> hullMods = new HashSet<>(Set.of(SkillTreeHullMod.ID));
        List<String> tags = new ArrayList<>(List.of("exiledSector_npcTree|bulwark|3|root_1,a_1"));
        FleetMemberAPI member = recoveredNpc("recovered", hullMods, tags);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        new SkillTreeInstaller().advance(0.01f);

        ShipSkillData adopted = ShipSkillDataManager.get("recovered");
        assertEquals(List.of("root_1", "a_1"), List.copyOf(adopted.getAllocatedNodeIds()));
        assertEquals("root_1", adopted.resolveStartingRootId(SkillTree.getAllNodes().values()));
        assertEquals(3, adopted.getLevel());
        assertEquals(2, adopted.getBankedFreeAllocations());
        assertTrue(adopted.isFreeNode("a_1"));
        assertEquals(0, adopted.getSpentOp(1));
        assertFalse(adopted.isNpcBuild());
        assertTrue(tags.isEmpty());
        verify(member.getVariant()).removeMod(SkillTreeHullMod.ID);
        verify(member.getVariant()).addPermaMod(SkillTreeHullMod.ID);
    }

    @Test
    void aShipThatAlreadyHasSavedProgressKeepsItAndOnlyLosesTheNpcTag() {
        registerNpcTreeNodes();
        ShipSkillDataManager.get("returning").addXp(40f);
        List<String> tags = new ArrayList<>(List.of("exiledSector_npcTree|bulwark|3|root_1,a_1"));
        FleetMemberAPI member = recoveredNpc("returning", new HashSet<>(), tags);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        new SkillTreeInstaller().advance(0.01f);

        ShipSkillData saved = ShipSkillDataManager.get("returning");
        assertTrue(saved.getAllocatedNodeIds().isEmpty());
        assertEquals(40f, saved.getXp());
        assertTrue(tags.isEmpty());
    }

    @Test
    void aDamagedNpcTagIsDroppedWithoutTouchingTheSavedTree() {
        List<String> tags = new ArrayList<>(List.of("exiledSector_npcTree|broken"));
        FleetMemberAPI member = recoveredNpc("damaged", new HashSet<>(), tags);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        new SkillTreeInstaller().advance(0.01f);

        assertTrue(ShipSkillDataManager.get("damaged").isBlank());
        assertTrue(tags.isEmpty());
    }

    @Test
    void shipsWithoutAnNpcTagAreNotAdopted() {
        FleetMemberAPI member = mockMember("own-ship", true);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        new SkillTreeInstaller().advance(0.01f);

        verify(member.getVariant(), never()).removeMod(SkillTreeHullMod.ID);
        verify(member.getVariant(), never()).removeTag(anyString());
    }

    @Test
    void thePanelInstallsItsHullModOnAShipBoughtWhilePausedAndOnTheRefitCopyBeingEdited() {
        FleetMemberAPI member = mockMember("bought", false);
        ShipVariantAPI stock = member.getVariant();
        ShipVariantAPI owned = mock(ShipVariantAPI.class);
        when(stock.getSource()).thenReturn(VariantSource.STOCK);
        when(stock.clone()).thenReturn(owned);
        ShipVariantAPI refitCopy = mock(ShipVariantAPI.class);

        assertTrue(SkillTreeInstaller.ensureInstalled(member, refitCopy));

        verify(owned).addPermaMod(SkillTreeHullMod.ID);
        verify(member).setVariant(owned, false, true);
        verify(refitCopy).addPermaMod(SkillTreeHullMod.ID);
        verify(member).setStatUpdateNeeded(true);
    }

    @Test
    void thePanelChangesNothingOnAShipThatAlreadyHasItsHullMod() {
        FleetMemberAPI member = mockMember("veteran", true);
        ShipVariantAPI refitCopy = mock(ShipVariantAPI.class);
        when(refitCopy.hasHullMod(SkillTreeHullMod.ID)).thenReturn(true);

        assertFalse(SkillTreeInstaller.ensureInstalled(member, refitCopy));

        verify(member.getVariant(), never()).addPermaMod(anyString());
        verify(refitCopy, never()).addPermaMod(anyString());
        verify(member, never()).setStatUpdateNeeded(anyBoolean());
    }

    @Test
    void thePanelAdoptsAnNpcTreeAndStripsTheTagFromTheRefitCopyToo() {
        registerNpcTreeNodes();
        String tag = "exiledSector_npcTree|bulwark|3|root_1,a_1";
        List<String> shipTags = new ArrayList<>(List.of(tag));
        FleetMemberAPI member = recoveredNpc("recovered", new HashSet<>(Set.of(SkillTreeHullMod.ID)), shipTags);
        List<String> copyTags = new ArrayList<>(List.of(tag));
        ShipVariantAPI refitCopy = recoveredNpc("copy-holder", new HashSet<>(Set.of(SkillTreeHullMod.ID)), copyTags).getVariant();

        assertTrue(SkillTreeInstaller.ensureInstalled(member, refitCopy));

        assertEquals(List.of("root_1", "a_1"), List.copyOf(ShipSkillDataManager.get("recovered").getAllocatedNodeIds()));
        assertTrue(shipTags.isEmpty());
        assertTrue(copyTags.isEmpty());
        verify(refitCopy).removeMod(SkillTreeHullMod.ID);
        verify(refitCopy).addPermaMod(SkillTreeHullMod.ID);
    }
}
