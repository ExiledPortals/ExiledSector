package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.ExiledSectorModPlugin;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.npc.NpcTreeConfig;
import exiledsector.skills.npc.NpcTreeRecords;
import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.SkillNodeOpCost;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcFleetLevellerTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MutableCharacterStatsAPI playerStats;
    private CampaignFleetAPI playerFleet;
    private LocationAPI location;

    @BeforeEach
    void setUp() {
        SkillDataResolver.clearCache();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        SkillTree.register(new SkillNode("root_1", type("root", SkillTier.ROOT).build(), List.of(), 0f, 0f));
        SkillTree.register(new SkillNode("a_1", type("a", SkillTier.SMALL).build(), List.of("root_1"), 0f, 0f));
        SkillTree.register(new SkillNode("armor_1", type("heavyarmor", SkillTier.NOTABLE)
                .exclusiveHullModIds(List.of("heavyarmor")).build(), List.of("a_1"), 0f, 0f));

        playerStats = mock(MutableCharacterStatsAPI.class);
        when(playerStats.getLevel()).thenReturn(15);
        location = mock(LocationAPI.class);
        playerFleet = mock(CampaignFleetAPI.class);
        when(playerFleet.getContainingLocation()).thenReturn(location);
        when(playerFleet.getLocation()).thenReturn(new Vector2f(0f, 0f));
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPlayerStats()).thenReturn(playerStats);
        when(sector.getSeedString()).thenReturn("SEED-1");
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
        globalMock.close();
        NpcFactionVolumes.clear();
        SkillDataResolver.clearCache();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    private static SkillType.Builder type(String id, SkillTier tier) {
        return new SkillType.Builder(id, id, "a.png", tier);
    }

    private static ShipVariantAPI statefulVariant(Set<String> hullMods, List<String> tags) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHullSize()).thenReturn(HullSize.DESTROYER);
        when(hullSpec.getShieldType()).thenReturn(ShieldType.FRONT);
        when(variant.getHullSpec()).thenReturn(hullSpec);
        when(variant.getSource()).thenReturn(VariantSource.REFIT);
        when(variant.getHullMods()).thenAnswer(invocation -> new LinkedHashSet<>(hullMods));
        when(variant.hasHullMod(anyString())).thenAnswer(invocation -> hullMods.contains((String) invocation.getArgument(0)));
        doAnswer(invocation -> hullMods.add(invocation.getArgument(0))).when(variant).addMod(anyString());
        doAnswer(invocation -> hullMods.remove((String) invocation.getArgument(0))).when(variant).removeMod(anyString());
        when(variant.getTags()).thenAnswer(invocation -> new ArrayList<>(tags));
        doAnswer(invocation -> tags.add(invocation.getArgument(0))).when(variant).addTag(anyString());
        doAnswer(invocation -> tags.remove((String) invocation.getArgument(0))).when(variant).removeTag(anyString());
        return variant;
    }

    private static FleetMemberAPI member(String id, ShipVariantAPI variant, boolean officered) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(id);
        when(member.getVariant()).thenReturn(variant);
        ShipHullSpecAPI hullSpec = variant.getHullSpec();
        when(member.getHullSpec()).thenReturn(hullSpec);
        PersonAPI captain = mock(PersonAPI.class);
        when(captain.isDefault()).thenReturn(!officered);
        when(member.getCaptain()).thenReturn(captain);
        return member;
    }

    private static CampaignFleetAPI fleet(String id, List<FleetMemberAPI> members, LocationAPI location, Vector2f position) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getId()).thenReturn(id);
        when(fleet.getContainingLocation()).thenReturn(location);
        when(fleet.getLocation()).thenReturn(position);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenReturn(members);
        when(fleet.getFleetData()).thenReturn(fleetData);
        FactionAPI faction = mock(FactionAPI.class);
        when(fleet.getFaction()).thenReturn(faction);
        Map<String, Object> memoryStore = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.get(anyString())).thenAnswer(invocation -> memoryStore.get((String) invocation.getArgument(0)));
        doAnswer(invocation -> memoryStore.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(memory).set(anyString(), org.mockito.ArgumentMatchers.any());
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        return fleet;
    }

    private CampaignFleetAPI fleetOf(FleetMemberAPI... members) {
        return fleet("fleet-1", List.of(members), location, new Vector2f(100f, 0f));
    }

    private static Map<String, String> records(CampaignFleetAPI fleet) {
        return NpcTreeRecords.of(fleet.getMemoryWithoutUpdate());
    }

    private void setOtherShipChance(int percent) {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", NpcTreeConfig.OTHER_SHIP_CHANCE_FIELD_ID)).thenReturn(percent);
    }

    @Test
    void anOfficeredNpcShipGetsItsTreeTaggedOnItsVariantAndItsConvertedHullmodStripped() {
        Set<String> hullMods = new LinkedHashSet<>(List.of("heavyarmor", "hardenedshieldemitter"));
        List<String> tags = new ArrayList<>();
        FleetMemberAPI officered = member("m1", statefulVariant(hullMods, tags), true);

        NpcFleetLeveller.ensure(fleetOf(officered));

        String tag = NpcTreeTag.find(officered.getVariant());
        assertNotNull(tag);
        assertTrue(tag.endsWith("root_1,a_1,armor_1"));
        assertTrue(hullMods.contains(SkillTreeHullMod.ID));
        assertFalse(hullMods.contains("heavyarmor"));
        assertTrue(hullMods.contains("hardenedshieldemitter"));
    }

    @Test
    void anNpcTreeHoldsNoMoreNodesThanAPlayersCountingTheRoot() {
        lunaSettingsMock.when(() -> LunaSettings.getInt(ExiledSectorModPlugin.MOD_ID, ShipLevelConfig.MAX_ALLOCATED_NODES_FIELD_ID))
                .thenReturn(2);
        FleetMemberAPI officered = member("m1", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), true);

        NpcFleetLeveller.ensure(fleetOf(officered));

        assertTrue(NpcTreeTag.find(officered.getVariant()).endsWith("|root_1,a_1"));
    }

    @Test
    void opFreedByAStrippedHullmodIsSpentOnExtraTreeNodes() {
        String previous = "armor_1";
        for (String id : List.of("b_1", "c_1", "d_1", "e_1", "f_1", "g_1")) {
            SkillTree.register(new SkillNode(id, type(id, SkillTier.SMALL).build(), List.of(previous), 0f, 0f));
            previous = id;
        }
        when(playerStats.getLevel()).thenReturn(2);
        SettingsAPI settings = mock(SettingsAPI.class);
        HullModSpecAPI heavyArmor = mock(HullModSpecAPI.class);
        when(heavyArmor.getCostFor(HullSize.DESTROYER)).thenReturn(10);
        when(settings.getHullModSpec("heavyarmor")).thenReturn(heavyArmor);
        globalMock.when(Global::getSettings).thenReturn(settings);
        Set<String> hullMods = new LinkedHashSet<>(List.of("heavyarmor"));
        FleetMemberAPI officered = member("m1", statefulVariant(hullMods, new ArrayList<>()), true);

        NpcFleetLeveller.ensure(fleetOf(officered));

        String tag = NpcTreeTag.find(officered.getVariant());
        int opCostPerNode = SkillNodeOpCost.perNode(HullSize.DESTROYER);
        assertEquals(10 / opCostPerNode * opCostPerNode, NpcTreeTag.decode(tag).getSpentOp(opCostPerNode));
        assertFalse(hullMods.contains("heavyarmor"));
    }

    @Test
    void theDecisionIsStoredInFleetMemoryAndNeverRerolled() {
        FleetMemberAPI officered = member("m1", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), true);
        CampaignFleetAPI fleet = fleetOf(officered);

        NpcFleetLeveller.ensure(fleet);
        String firstRecord = records(fleet).get("m1");
        NpcFactionVolumes.clear();
        when(playerStats.getLevel()).thenReturn(1);
        NpcFleetLeveller.ensure(fleet);

        assertTrue(NpcTreeRecords.isLevelled(firstRecord));
        assertEquals(firstRecord, records(fleet).get("m1"));
        assertEquals(firstRecord, NpcTreeTag.find(officered.getVariant()));
    }

    @Test
    void aVariantRebuiltByReinflationGetsTheSameTreeReapplied() {
        Set<String> hullMods = new LinkedHashSet<>(List.of("heavyarmor"));
        List<String> tags = new ArrayList<>();
        FleetMemberAPI officered = member("m1", statefulVariant(hullMods, tags), true);
        CampaignFleetAPI fleet = fleetOf(officered);
        NpcFleetLeveller.ensure(fleet);
        String tag = NpcTreeTag.find(officered.getVariant());

        hullMods.clear();
        hullMods.add("heavyarmor");
        tags.clear();
        NpcFleetLeveller.ensure(fleet);

        assertEquals(tag, NpcTreeTag.find(officered.getVariant()));
        assertTrue(hullMods.contains(SkillTreeHullMod.ID));
        assertFalse(hullMods.contains("heavyarmor"));
    }

    @Test
    void anUnchosenShipIsRecordedAsNotLevelledAndLeftUntouched() {
        setOtherShipChance(0);
        Set<String> hullMods = new LinkedHashSet<>(List.of("heavyarmor"));
        List<String> tags = new ArrayList<>();
        FleetMemberAPI regular = member("m1", statefulVariant(hullMods, tags), false);
        CampaignFleetAPI fleet = fleetOf(regular);

        NpcFleetLeveller.ensure(fleet);

        assertEquals(NpcTreeRecords.NOT_LEVELLED, records(fleet).get("m1"));
        assertTrue(tags.isEmpty());
        assertEquals(Set.of("heavyarmor"), hullMods);
    }

    @Test
    void everyOtherShipIsLevelledAtAHundredPercentChance() {
        setOtherShipChance(100);
        FleetMemberAPI regular = member("m1", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), false);

        NpcFleetLeveller.ensure(fleetOf(regular));

        assertNotNull(NpcTreeTag.find(regular.getVariant()));
    }

    @Test
    void nothingHappensWhileNpcSkillTreesAreDisabled() {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", NpcTreeConfig.ENABLED_FIELD_ID)).thenReturn(false);
        FleetMemberAPI officered = member("m1", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), true);
        CampaignFleetAPI fleet = fleetOf(officered);

        NpcFleetLeveller.ensure(fleet);

        assertNull(fleet.getMemoryWithoutUpdate().get(NpcTreeRecords.MEMORY_KEY));
        assertNull(NpcTreeTag.find(officered.getVariant()));
    }

    @Test
    void identicalFleetsGetIdenticalTrees() {
        setOtherShipChance(100);
        FleetMemberAPI first = member("m1", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), false);
        FleetMemberAPI second = member("m1", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), false);

        NpcFleetLeveller.ensure(fleetOf(first));
        NpcFleetLeveller.ensure(fleetOf(second));

        assertEquals(NpcTreeTag.find(first.getVariant()), NpcTreeTag.find(second.getVariant()));
    }

    @Test
    void playerStationAndLocationlessFleetsAreNeverLevelled() {
        CampaignFleetAPI ordinary = fleetOf();
        CampaignFleetAPI player = fleetOf();
        when(player.isPlayerFleet()).thenReturn(true);
        CampaignFleetAPI station = fleetOf();
        when(station.isStationMode()).thenReturn(true);
        CampaignFleetAPI temporary = fleet("temp", List.of(), null, new Vector2f());

        assertTrue(NpcFleetLeveller.isLevellable(ordinary));
        assertFalse(NpcFleetLeveller.isLevellable(player));
        assertFalse(NpcFleetLeveller.isLevellable(station));
        assertFalse(NpcFleetLeveller.isLevellable(temporary));
        assertFalse(NpcFleetLeveller.isLevellable(null));
    }

    @Test
    void aTreeTaggedBeforeTheNpcRenameIsRetaggedWithTheCurrentPrefix() {
        List<String> tags = new ArrayList<>(List.of("exiledSector_enemyTree|bulwark|2|root_1,a_1,armor_1"));
        FleetMemberAPI officered = member("m1", statefulVariant(new LinkedHashSet<>(Set.of(SkillTreeHullMod.ID)), tags), true);

        NpcFleetLeveller.ensure(fleetOf(officered));

        assertEquals(1, tags.size());
        assertTrue(tags.get(0).startsWith(NpcTreeTag.PREFIX));
    }

    @Test
    void playerFactionFleetsAndShipsFightingAlongsideThePlayerAreLevelledToo() {
        FleetMemberAPI patrolOfficer = member("patrol", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), true);
        CampaignFleetAPI playerFactionPatrol = fleetOf(patrolOfficer);
        when(playerFactionPatrol.getFaction().isPlayerFaction()).thenReturn(true);
        FleetMemberAPI allyOfficer = member("ally", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), true);
        when(allyOfficer.isAlly()).thenReturn(true);

        NpcFleetLeveller.ensure(playerFactionPatrol);
        NpcFleetLeveller.ensure(fleetOf(allyOfficer));

        assertNotNull(NpcTreeTag.find(patrolOfficer.getVariant()));
        assertNotNull(NpcTreeTag.find(allyOfficer.getVariant()));
    }

    @Test
    void theSweepOnlyLevelsFleetsWithinRangeOfThePlayer() {
        FleetMemberAPI near = member("near", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), true);
        FleetMemberAPI far = member("far", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), true);
        CampaignFleetAPI nearFleet = fleet("near-fleet", List.of(near), location, new Vector2f(3000f, 0f));
        CampaignFleetAPI farFleet = fleet("far-fleet", List.of(far), location, new Vector2f(5000f, 0f));
        when(location.getFleets()).thenReturn(List.of(playerFleet, nearFleet, farFleet));

        NpcFleetSweepScript.sweepAround(playerFleet, NpcFleetSweepScript.SWEEP_RANGE);

        assertNotNull(NpcTreeTag.find(near.getVariant()));
        assertNull(NpcTreeTag.find(far.getVariant()));
    }

    @Test
    void theSweepChecksStraightAwayAndKeepsRunningWithTheGamePaused() {
        FleetMemberAPI near = member("near", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), true);
        CampaignFleetAPI nearFleet = fleet("near-fleet", List.of(near), location, new Vector2f(100f, 0f));
        when(location.getFleets()).thenReturn(List.of(nearFleet));
        NpcFleetSweepScript script = new NpcFleetSweepScript();

        script.advance(0f);
        assertNotNull(NpcTreeTag.find(near.getVariant()));
        assertFalse(script.isDone());
        assertFalse(script.runWhilePaused());
    }

    @Test
    void openingAnEncounterDialogWithAFleetLevelsTheFleetsAroundThePlayer() {
        FleetMemberAPI near = member("near", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), true);
        CampaignFleetAPI target = fleet("target", List.of(near), location, new Vector2f(50f, 0f));
        when(location.getFleets()).thenReturn(List.of(target));
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class);
        when(dialog.getInteractionTarget()).thenReturn(target);

        new NpcFleetDialogListener().reportShownInteractionDialog(dialog);

        assertNotNull(NpcTreeTag.find(near.getVariant()));
    }

    @Test
    void openingADialogWithSomethingOtherThanAFleetDoesNothing() {
        FleetMemberAPI near = member("near", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), true);
        CampaignFleetAPI nearby = fleet("nearby", List.of(near), location, new Vector2f(50f, 0f));
        when(location.getFleets()).thenReturn(List.of(nearby));
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class);
        when(dialog.getInteractionTarget()).thenReturn(mock(SectorEntityToken.class));

        new NpcFleetDialogListener().reportShownInteractionDialog(dialog);

        assertNull(NpcTreeTag.find(near.getVariant()));
    }

    @Test
    void theInflationListenerLevelsTheInflatedFleet() {
        FleetMemberAPI officered = member("m1", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), true);
        CampaignFleetAPI fleet = fleetOf(officered);

        new NpcFleetInflationListener().reportFleetInflated(fleet, null);

        assertNotNull(NpcTreeTag.find(officered.getVariant()));
    }

    @Test
    void aFactionShipCrossesItsOwnWormholeForANotableInItsFactionVolume() throws Exception {
        SkillType wormhole = type("wormhole", SkillTier.WORMHOLE).build();
        SkillTree.register(new SkillNode("gate_core", wormhole, List.of("a_1", "gate_far"), 0f, 0f,
                new SkillNodeDecoration(null, null, null, null, "gate_far"), List.of("core")));
        SkillTree.register(new SkillNode("gate_far", wormhole, List.of("gate_core"), 0f, 0f,
                new SkillNodeDecoration(null, null, null, null, "gate_core"), List.of("hegemony")));
        SkillTree.register(new SkillNode("hegemony_notable", type("pride", SkillTier.NOTABLE).build(), List.of("gate_far"), 0f, 0f,
                SkillNodeDecoration.NONE, List.of("hegemony")));
        NpcFactionVolumes.register(new JSONArray("[{\"faction\": \"hegemony\", \"region\": \"hegemony\"}]"));
        FleetMemberAPI officered = member("m1", statefulVariant(new LinkedHashSet<>(), new ArrayList<>()), true);
        CampaignFleetAPI fleet = fleetOf(officered);
        when(fleet.getFaction().getId()).thenReturn("hegemony");

        NpcFleetLeveller.ensure(fleet);

        String tag = NpcTreeTag.find(officered.getVariant());
        assertTrue(tag.contains("gate_core") && tag.contains("gate_far") && tag.contains("hegemony_notable"), tag);
    }

    @Test
    void aHullmodWhoseNodeIsOutOfReachStaysOnTheShip() {
        lunaSettingsMock.when(() -> LunaSettings.getInt(ExiledSectorModPlugin.MOD_ID, ShipLevelConfig.MAX_ALLOCATED_NODES_FIELD_ID))
                .thenReturn(2);
        Set<String> hullMods = new LinkedHashSet<>(List.of("heavyarmor"));
        FleetMemberAPI officered = member("m1", statefulVariant(hullMods, new ArrayList<>()), true);

        NpcFleetLeveller.ensure(fleetOf(officered));

        assertTrue(NpcTreeTag.find(officered.getVariant()).endsWith("|root_1,a_1"));
        assertTrue(hullMods.contains("heavyarmor"));
    }

    @Test
    void hullSkinsUseTheirBaseHullsDesignTypeWhenTheirOwnHasNoRoot() {
        ShipHullSpecAPI base = mock(ShipHullSpecAPI.class);
        when(base.getManufacturer()).thenReturn("Low Tech");
        ShipHullSpecAPI skin = mock(ShipHullSpecAPI.class);
        when(skin.getManufacturer()).thenReturn("Luddic Path");
        when(skin.getBaseHull()).thenReturn(base);
        ShipHullSpecAPI midline = mock(ShipHullSpecAPI.class);
        when(midline.getManufacturer()).thenReturn("Midline");
        when(midline.getBaseHull()).thenReturn(base);

        assertEquals("Low Tech", NpcFleetLeveller.designType(skin));
        assertEquals("Midline", NpcFleetLeveller.designType(midline));
        assertNull(NpcFleetLeveller.designType(null));
    }

    @Test
    void aShipFittedWithConvertedHangarSwapsItForTheHangarNode() {
        SkillTree.register(new SkillNode("hangar_1", type("hangar", SkillTier.KEYSTONE).tags(List.of("req_no_fighter_bays"))
                .exclusiveHullModIds(List.of("converted_hangar")).build(), List.of("root_1"), 0f, 0f));
        Set<String> hullMods = new LinkedHashSet<>(List.of("converted_hangar"));
        FleetMemberAPI officered = member("m1", statefulVariant(hullMods, new ArrayList<>()), true);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat bays = new MutableStat(0f);
        bays.modifyFlat("converted_hangar", 1f);
        when(stats.getNumFighterBays()).thenReturn(bays);
        when(officered.getStats()).thenReturn(stats);
        when(officered.getNumFlightDecks()).thenReturn(1);

        NpcFleetLeveller.ensure(fleetOf(officered));

        assertTrue(NpcTreeTag.find(officered.getVariant()).contains("hangar_1"));
        assertFalse(hullMods.contains("converted_hangar"));
    }
}
