package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.compat.SecondInCommandCompat;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.PhantomHullModStatus;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.progression.ShipLevelConfig;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipTreeSyncTest {

    private static final String MILITARIZED_SUBSYSTEMS = "militarized_subsystems";

    private MockedStatic<Global> globalMock;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private SectorAPI sector;
    private SettingsAPI settings;
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
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getInt(anyString(), anyString())).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getFloat(anyString(), anyString())).thenReturn(null);
        globalMock.when(Global::getSector).thenReturn(sector);
        settings = mock(SettingsAPI.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(fleetData.getMembersListCopy()).thenReturn(List.of());

        SkillTree.clearNodes();
        ShipTreeSync.takePlayerFleetSyncRequest();
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        lunaSettingsMock.close();
        SkillTree.clearNodes();
        PhantomHullModStatus.clear();
    }

    static FleetMemberAPI mockMember(String id, boolean hasHullMod) {
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

    private CampaignFleetAPI playerFleet() {
        return sector.getPlayerFleet();
    }

    @Test
    void installsIntoAShipSpecificCopyWhenTheVariantIsNotAlreadyARefitVariant() {
        FleetMemberAPI member = mockMember("stock-ship", false);
        ShipVariantAPI shared = member.getVariant();
        ShipVariantAPI copy = mock(ShipVariantAPI.class);
        when(shared.getSource()).thenReturn(VariantSource.STOCK);
        when(shared.clone()).thenReturn(copy);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        ShipTreeSync.fleetChanged(playerFleet());

        verify(copy).setSource(VariantSource.REFIT);
        verify(member).setVariant(copy, false, true);
        verify(copy).addPermaMod(SkillTreeHullMod.ID);
        verify(shared, never()).addPermaMod(SkillTreeHullMod.ID);
    }

    @Test
    void installsDirectlyIntoAnAlreadyShipSpecificRefitVariant() {
        FleetMemberAPI member = mockMember("refit-ship", false);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        ShipTreeSync.fleetChanged(playerFleet());

        verify(member.getVariant(), never()).clone();
        verify(member, never()).setVariant(any(), anyBoolean(), anyBoolean());
        verify(member.getVariant()).addPermaMod(SkillTreeHullMod.ID);
    }

    @Test
    void doesNothingWithoutAFleet() {
        assertFalse(ShipTreeSync.fleetChanged(null));
    }

    @Test
    void addsTheHullModOnlyToShipsMissingItAndAsksForOneFleetSync() {
        FleetMemberAPI hasIt = mockMember("ship-with-mod", true);
        FleetMemberAPI missingIt = mockMember("ship-without-mod", false);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(hasIt, missingIt));

        assertTrue(ShipTreeSync.fleetChanged(playerFleet()));

        verify(hasIt.getVariant(), never()).addPermaMod(SkillTreeHullMod.ID);
        verify(missingIt.getVariant()).addPermaMod(SkillTreeHullMod.ID);
        verify(missingIt).setStatUpdateNeeded(true);
        verify(fleetData).setSyncNeeded();
    }

    @Test
    void aFleetThatIsAlreadyInstalledNeedsNoSync() {
        FleetMemberAPI member = mockMember("ship", true);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        assertFalse(ShipTreeSync.fleetChanged(playerFleet()));

        verify(fleetData, never()).setSyncNeeded();
    }

    @Test
    void movesItsHullModBehindTheSecondInCommandControllerSoItAppliesAfterIt() {
        FleetMemberAPI member = mockMember("ship", true);
        ShipVariantAPI variant = member.getVariant();
        when(variant.hasHullMod(SecondInCommandCompat.CONTROLLER_HULLMOD_ID)).thenReturn(true);
        when(variant.getHullMods()).thenReturn(List.of(SkillTreeHullMod.ID, SecondInCommandCompat.CONTROLLER_HULLMOD_ID));
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        ShipTreeSync.fleetChanged(playerFleet());

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

        ShipTreeSync.fleetChanged(playerFleet());

        InOrder order = inOrder(variant);
        order.verify(variant).removePermaMod(SkillTreeHullMod.ID);
        order.verify(variant).addPermaMod(SkillTreeHullMod.ID);
        verify(member).setStatUpdateNeeded(true);
    }

    @Test
    void ignoresTheHullModsTheTreeItselfPlacesAfterItsOwn() {
        FleetMemberAPI member = mockMember("ship", true);
        ShipVariantAPI variant = member.getVariant();
        when(variant.getHullMods()).thenReturn(List.of(SkillTreeHullMod.ID, SkillConflictWarningHullMod.ID,
                OpReserveHullMods.ID_PREFIX + "3", MILITARIZED_SUBSYSTEMS));
        when(variant.hasTag("exiledSector_installed_" + MILITARIZED_SUBSYSTEMS)).thenReturn(true);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        ShipTreeSync.fleetChanged(playerFleet());

        verify(variant, never()).removePermaMod(SkillTreeHullMod.ID);
        verify(member, never()).setStatUpdateNeeded(anyBoolean());
    }

    @Test
    void aCopyOfAPhantomHullModThePlayerInstalledThemselvesStillCountsAsAHullModAfterOurs() {
        FleetMemberAPI member = mockMember("ship", true);
        ShipVariantAPI variant = member.getVariant();
        when(variant.getHullMods()).thenReturn(List.of(SkillTreeHullMod.ID, MILITARIZED_SUBSYSTEMS));
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        ShipTreeSync.fleetChanged(playerFleet());

        verify(variant).removePermaMod(SkillTreeHullMod.ID);
        verify(variant).addPermaMod(SkillTreeHullMod.ID);
    }

    @Test
    void leavesItsHullModInPlaceWhenItIsAlreadyLast() {
        FleetMemberAPI member = mockMember("ship", true);
        ShipVariantAPI variant = member.getVariant();
        when(variant.getHullMods()).thenReturn(List.of("hardened_shields", "additional_berthing", SkillTreeHullMod.ID));
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        ShipTreeSync.fleetChanged(playerFleet());

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

        ShipTreeSync.fleetChanged(playerFleet());

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

        ShipTreeSync.fleetChanged(playerFleet());

        verify(hullBonus, never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void aPlayerFleetSyncRequestIsTakenOnlyOnce() {
        ShipTreeSync.requestPlayerFleetSync();

        assertTrue(ShipTreeSync.takePlayerFleetSyncRequest());
        assertFalse(ShipTreeSync.takePlayerFleetSyncRequest());
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

        ShipTreeSync.fleetChanged(playerFleet());

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
    void adoptingAnNpcTreePutsTheTreeHullModBackInTheSameCallSoTheShipNeverSitsWithoutIt() {
        registerNpcTreeNodes();
        Set<String> hullMods = new HashSet<>(Set.of(SkillTreeHullMod.ID));
        List<String> tags = new ArrayList<>(List.of("exiledSector_npcTree|bulwark|3|root_1,a_1"));
        FleetMemberAPI member = recoveredNpc("recovered", hullMods, tags);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        assertTrue(ShipTreeSync.fleetChanged(playerFleet()));

        assertTrue(hullMods.contains(SkillTreeHullMod.ID));
        verify(member).setStatUpdateNeeded(true);
        verify(fleetData).setSyncNeeded();
    }

    @Test
    void adoptingAFleetsNpcTreesRequestsASyncOnlyWhenSomethingWasAdopted() {
        registerNpcTreeNodes();
        List<String> tags = new ArrayList<>(List.of("exiledSector_npcTree|bulwark|3|root_1,a_1"));
        FleetMemberAPI recovered = recoveredNpc("recovered", new HashSet<>(), tags);
        FleetMemberAPI ownShip = mockMember("own-ship", true);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(ownShip));

        ShipTreeSync.fleetChanged(playerFleet());
        verify(fleetData, never()).setSyncNeeded();

        when(fleetData.getMembersListCopy()).thenReturn(List.of(ownShip, recovered));
        ShipTreeSync.fleetChanged(playerFleet());

        assertEquals(List.of("root_1", "a_1"), List.copyOf(ShipSkillDataManager.get("recovered").getAllocatedNodeIds()));
        assertTrue(tags.isEmpty());
        verify(fleetData).setSyncNeeded();
    }

    @Test
    void aShipThatAlreadyHasSavedProgressKeepsItAndOnlyLosesTheNpcTag() {
        registerNpcTreeNodes();
        ShipSkillDataManager.get("returning").addXp(40f);
        List<String> tags = new ArrayList<>(List.of("exiledSector_npcTree|bulwark|3|root_1,a_1"));
        FleetMemberAPI member = recoveredNpc("returning", new HashSet<>(), tags);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        ShipTreeSync.fleetChanged(playerFleet());

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

        ShipTreeSync.fleetChanged(playerFleet());

        assertTrue(ShipSkillDataManager.get("damaged").isBlank());
        assertTrue(tags.isEmpty());
    }

    @Test
    void shipsWithoutAnNpcTagAreNotAdopted() {
        FleetMemberAPI member = mockMember("own-ship", true);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        ShipTreeSync.fleetChanged(playerFleet());

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

        assertTrue(ShipTreeSync.memberChanged(member, refitCopy));

        verify(owned).addPermaMod(SkillTreeHullMod.ID);
        verify(member).setVariant(owned, false, true);
        verify(refitCopy).addPermaMod(SkillTreeHullMod.ID);
        verify(member, atLeastOnce()).setStatUpdateNeeded(true);
    }

    @Test
    void thePanelChangesNothingOnAShipThatAlreadyHasItsHullMod() {
        FleetMemberAPI member = mockMember("veteran", true);
        ShipVariantAPI refitCopy = mock(ShipVariantAPI.class);
        when(refitCopy.hasHullMod(SkillTreeHullMod.ID)).thenReturn(true);

        assertFalse(ShipTreeSync.memberChanged(member, refitCopy));

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

        assertTrue(ShipTreeSync.memberChanged(member, refitCopy));

        assertEquals(List.of("root_1", "a_1"), List.copyOf(ShipSkillDataManager.get("recovered").getAllocatedNodeIds()));
        assertTrue(shipTags.isEmpty());
        assertTrue(copyTags.isEmpty());
        verify(refitCopy).removeMod(SkillTreeHullMod.ID);
        verify(refitCopy).addPermaMod(SkillTreeHullMod.ID);
    }

    private static SkillNode registerMilitarizedNode() {
        SkillType militarizedType = new SkillType.Builder(MILITARIZED_SUBSYSTEMS, "Militarized Subsystems", "a.png", SkillTier.NOTABLE)
                .phantomHullModIds(List.of(MILITARIZED_SUBSYSTEMS))
                .build();
        PhantomHullModStatus.markActive(MILITARIZED_SUBSYSTEMS);
        SkillNode militarizedNode = new SkillNode("militarized_subsystems_1", militarizedType, List.of(), 0f, 0f);
        SkillTree.register(militarizedNode);
        return militarizedNode;
    }

    private static void demoteTreeInstalledMilitarizedSubsystems(ShipVariantAPI variant) {
        when(variant.getHullMods()).thenReturn(List.of(MILITARIZED_SUBSYSTEMS, SkillTreeHullMod.ID));
        when(variant.getTags()).thenReturn(List.of("exiledSector_installed_" + MILITARIZED_SUBSYSTEMS));
        when(variant.hasTag("exiledSector_installed_" + MILITARIZED_SUBSYSTEMS)).thenReturn(true);
        when(variant.hasHullMod(MILITARIZED_SUBSYSTEMS)).thenReturn(true);
        when(variant.getPermaMods()).thenReturn(new LinkedHashSet<>(List.of(SkillTreeHullMod.ID)));
    }

    @Test
    void thePanelMakesTreeInstalledHullModsPermanentAgainOnTheRefitCopyItEdits() {
        ShipSkillDataManager.get("ship").allocate(registerMilitarizedNode(), 1);
        FleetMemberAPI member = mockMember("ship", true);
        ShipVariantAPI refitCopy = mock(ShipVariantAPI.class);
        when(refitCopy.hasHullMod(SkillTreeHullMod.ID)).thenReturn(true);
        demoteTreeInstalledMilitarizedSubsystems(refitCopy);

        assertTrue(ShipTreeSync.memberChanged(member, refitCopy));

        verify(refitCopy).addPermaMod(MILITARIZED_SUBSYSTEMS);
        verify(member).setStatUpdateNeeded(true);
    }

    @Test
    void syncingAVariantPlacesTheShipTagTheReserveAndThePhantomsInOnePass() {
        ShipSkillDataManager.get("ship").allocate(registerMilitarizedNode(), 1);
        when(settings.getHullModSpec("exiledSector_opSpent_0")).thenReturn(mock(HullModSpecAPI.class));
        FleetMemberAPI member = mockMember("ship", true);
        ShipVariantAPI variant = member.getVariant();

        assertTrue(ShipTreeSync.syncVariant(member, variant));

        verify(variant).addTag("exiledSector_ship_ship");
        verify(variant).addMod("exiledSector_opSpent_0");
        verify(variant).addPermaMod(MILITARIZED_SUBSYSTEMS);
        verify(variant).addTag("exiledSector_installed_" + MILITARIZED_SUBSYSTEMS);
    }

    @Test
    void syncingAnNpcTreeVariantLeavesThePlayerShipTagAndReserveAlone() {
        registerNpcTreeNodes();
        FleetMemberAPI member = mockMember("npc", true);
        ShipVariantAPI variant = member.getVariant();
        when(variant.getTags()).thenReturn(List.of("exiledSector_npcTree|bulwark|3|root_1,a_1"));

        ShipTreeSync.syncVariant(member, variant);

        verify(variant, never()).addTag(anyString());
        verify(variant, never()).addMod(anyString());
    }

    @Test
    void levelUpsMarkTheLevelledShipsForAStatRebuildSoTheirReserveIsRepriced() {
        FleetMemberAPI levelled = mockMember("levelled", true);

        ShipTreeSync.levelsChanged(playerFleet(), List.of(levelled));

        verify(levelled).setStatUpdateNeeded(true);
        verify(fleetData).setSyncNeeded();
    }

    @Test
    void noLevelUpsMeansNoSync() {
        ShipTreeSync.levelsChanged(playerFleet(), List.of());

        verify(fleetData, never()).setSyncNeeded();
    }

    @Test
    void aNewShipStartsAtThePlayersLevelWithAFreeAllocationForEveryLevel() {
        playerLevel(15);
        FleetMemberAPI member = mockMember("bought", true);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        ShipTreeSync.raiseToLevelFloor(playerFleet());

        ShipSkillData data = ShipSkillDataManager.get("bought");
        assertEquals(15, data.getLevel());
        assertEquals(15, data.getBankedFreeAllocations());
        assertEquals(0f, data.getXp());
        verify(member).setStatUpdateNeeded(true);
        verify(fleetData).setSyncNeeded();
    }

    @Test
    void afterAnEngagementARecoveredNpcShipKeepsItsTreeAndIsRaisedToThePlayersLevel() {
        playerLevel(10);
        registerNpcTreeNodes();
        List<String> tags = new ArrayList<>(List.of("exiledSector_npcTree|bulwark|3|root_1,a_1"));
        FleetMemberAPI member = recoveredNpc("recovered", new HashSet<>(Set.of(SkillTreeHullMod.ID)), tags);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        ShipTreeSync.afterPlayerEngagement(playerFleet());

        ShipSkillData data = ShipSkillDataManager.get("recovered");
        assertEquals(List.of("root_1", "a_1"), List.copyOf(data.getAllocatedNodeIds()));
        assertEquals(10, data.getLevel());
        assertEquals(9, data.getBankedFreeAllocations());
    }

    @Test
    void aFleetChangeLeavesShipLevelsAlone() {
        playerLevel(15);
        FleetMemberAPI member = mockMember("bought", true);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        ShipTreeSync.fleetChanged(playerFleet());

        assertEquals(0, ShipSkillDataManager.get("bought").getLevel());
    }

    @Test
    void shipsAlreadyAtOrAboveTheFloorAreLeftAlone() {
        playerLevel(15);
        ShipSkillData veteran = ShipSkillDataManager.get("veteran");
        for (int i = 0; i < 30; i++) {
            veteran.incrementLevel();
        }
        veteran.addXp(25f);
        FleetMemberAPI member = mockMember("veteran", true);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(member));

        assertFalse(ShipTreeSync.raiseToLevelFloor(member, ShipTreeSync.currentLevelFloor()));
        assertEquals(30, veteran.getLevel());
        assertEquals(0, veteran.getBankedFreeAllocations());
        assertEquals(25f, veteran.getXp());
        verify(member, never()).setStatUpdateNeeded(true);
    }

    @Test
    void theFloorIsTheConfiguredPercentageOfThePlayersLevel() {
        lunaSettingsMock.when(() -> LunaSettings.getInt(anyString(), eq(ShipLevelConfig.LEVEL_FLOOR_PERCENT_FIELD_ID))).thenReturn(50);
        playerLevel(15);

        assertEquals(7, ShipTreeSync.currentLevelFloor());
    }

    @Test
    void thereIsNoFloorWithoutPlayerStats() {
        assertEquals(0, ShipTreeSync.currentLevelFloor());
        assertFalse(ShipTreeSync.raiseToLevelFloor(mockMember("any", true), 0));
    }

    @Test
    void thePanelRaisesAShipBoughtWhilePausedToTheFloor() {
        playerLevel(12);
        FleetMemberAPI member = mockMember("bought-while-paused", true);

        assertTrue(ShipTreeSync.memberChanged(member, member.getVariant()));
        assertEquals(12, ShipSkillDataManager.get("bought-while-paused").getLevel());
    }

    private void playerLevel(int level) {
        MutableCharacterStatsAPI playerStats = mock(MutableCharacterStatsAPI.class);
        when(playerStats.getLevel()).thenReturn(level);
        when(sector.getPlayerStats()).thenReturn(playerStats);
    }
}
