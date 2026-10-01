package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CombatDamageData;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.ui.LabelAPI;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTree;
import exiledsector.skills.progression.ShipLevelConfig;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CombatXpListenerTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private FleetDataAPI fleetData;
    private TextPanelAPI textPanel;
    private InteractionDialogAPI dialog;

    @BeforeEach
    void setUp() {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        fleetData = mock(FleetDataAPI.class);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(playerFleet.getFleetData()).thenReturn(fleetData);
        when(fleetData.getMembersListCopy()).thenReturn(List.of());

        CampaignUIAPI campaignUi = mock(CampaignUIAPI.class);
        dialog = mock(InteractionDialogAPI.class);
        textPanel = mock(TextPanelAPI.class);
        when(sector.getCampaignUI()).thenReturn(campaignUi);
        when(campaignUi.getCurrentInteractionDialog()).thenReturn(dialog);
        when(dialog.getTextPanel()).thenReturn(textPanel);

        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", ShipLevelConfig.MAX_LEVEL_FIELD_ID)).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", ShipLevelConfig.XP_BASE_FIELD_ID)).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getFloat("exiledSector", ShipLevelConfig.XP_GROWTH_FIELD_ID)).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", ShipLevelConfig.XP_GROWTH_CUTOFF_LEVEL_FIELD_ID)).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getFloat("exiledSector", ShipLevelConfig.XP_PER_DEPLOYMENT_POINT_FIELD_ID)).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getFloat("exiledSector", ShipLevelConfig.XP_LOSS_MULTIPLIER_FIELD_ID)).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getFloat("exiledSector", ShipLevelConfig.XP_DIFFICULTY_STRENGTH_FIELD_ID)).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getFloat("exiledSector", ShipLevelConfig.XP_DIFFICULTY_MAX_MULTIPLIER_FIELD_ID)).thenReturn(null);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getColor("buttonShortcut")).thenReturn(Color.YELLOW);
        when(settings.getColor("textFriendColor")).thenReturn(Color.GREEN);
        globalMock.when(Global::getSettings).thenReturn(settings);
        FactionAPI playerFaction = mock(FactionAPI.class);
        when(playerFaction.getBaseUIColor()).thenReturn(Color.CYAN);
        when(sector.getPlayerFaction()).thenReturn(playerFaction);

        SkillTree.clearNodes();
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        lunaSettingsMock.close();
        SkillTree.clearNodes();
    }

    private static FleetMemberAPI member(String id) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(member.getId()).thenReturn(id);
        when(member.getShipName()).thenReturn("ISS " + id);
        when(member.getHullSpec()).thenReturn(hullSpec);
        when(hullSpec.getHullSize()).thenReturn(HullSize.FRIGATE);
        when(hullSpec.getHullNameWithDashClass()).thenReturn("Wolf-class");
        return member;
    }

    private static EngagementResultAPI engagement(boolean playerWon, float enemyDpDestroyed) {
        EngagementResultAPI result = mock(EngagementResultAPI.class);
        EngagementResultForFleetAPI enemy = mock(EngagementResultForFleetAPI.class);
        FleetMemberAPI destroyed = mock(FleetMemberAPI.class);
        when(destroyed.getDeploymentPointsCost()).thenReturn(enemyDpDestroyed);
        when(enemy.getDestroyed()).thenReturn(List.of(destroyed));
        when(result.didPlayerWin()).thenReturn(playerWon);
        if (playerWon) {
            when(result.getLoserResult()).thenReturn(enemy);
        } else {
            when(result.getWinnerResult()).thenReturn(enemy);
        }
        CombatDamageData combatDamageData = mock(CombatDamageData.class);
        when(result.getLastCombatDamageData()).thenReturn(combatDamageData);
        return result;
    }

    @Test
    void awardsEveryFleetMemberXpForEnemyDeploymentPointsDestroyed() {
        List<FleetMemberAPI> members = List.of(member("ship-a"), member("ship-b"));
        when(fleetData.getMembersListCopy()).thenReturn(members);

        new CombatXpListener().reportPlayerEngagement(engagement(true, 40f));

        assertEquals(40f, ShipSkillDataManager.get("ship-a").getXp());
        assertEquals(40f, ShipSkillDataManager.get("ship-b").getXp());
    }

    @Test
    void countsDisabledEnemyShipsAlongsideDestroyedOnes() {
        List<FleetMemberAPI> members = List.of(member("ship-a"));
        when(fleetData.getMembersListCopy()).thenReturn(members);
        EngagementResultAPI result = engagement(true, 40f);
        FleetMemberAPI disabled = mock(FleetMemberAPI.class);
        when(disabled.getDeploymentPointsCost()).thenReturn(15f);
        List<FleetMemberAPI> disabledShips = List.of(disabled);
        when(result.getLoserResult().getDisabled()).thenReturn(disabledShips);

        new CombatXpListener().reportPlayerEngagement(result);

        assertEquals(55f, ShipSkillDataManager.get("ship-a").getXp());
    }

    @Test
    void appliesTheLossMultiplierWhenThePlayerLost() {
        List<FleetMemberAPI> members = List.of(member("ship-a"));
        when(fleetData.getMembersListCopy()).thenReturn(members);

        new CombatXpListener().reportPlayerEngagement(engagement(false, 40f));

        assertEquals(40f * ShipLevelConfig.DEFAULT_XP_LOSS_MULTIPLIER, ShipSkillDataManager.get("ship-a").getXp());
        verify(textPanel).addPara(eq("%s"), (Color) any(), (Color) any(), contains("reduced because the battle was lost"));
    }

    @Test
    void ignoresAutoresolvedEngagementsWithoutCombatData() {
        List<FleetMemberAPI> members = List.of(member("ship-a"));
        when(fleetData.getMembersListCopy()).thenReturn(members);
        EngagementResultAPI autoresolved = engagement(true, 40f);
        when(autoresolved.getLastCombatDamageData()).thenReturn(null);

        new CombatXpListener().reportPlayerEngagement(autoresolved);

        assertEquals(0f, ShipSkillDataManager.get("ship-a").getXp());
        verify(textPanel, never()).addPara(anyString(), any(Color.class), any(Color.class), any(String[].class));
    }

    @Test
    void writesTheXpEarnedAndAnyLevelUpsToThePostBattleReport() {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", ShipLevelConfig.XP_BASE_FIELD_ID)).thenReturn(30);
        List<FleetMemberAPI> members = List.of(member("ship-a"));
        when(fleetData.getMembersListCopy()).thenReturn(members);
        LabelAPI label = mock(LabelAPI.class);
        when(textPanel.addPara(anyString(), (Color) any(), (Color) any(), any(String[].class))).thenReturn(label);

        new CombatXpListener().reportPlayerEngagement(engagement(true, 40f));

        ShipSkillData data = ShipSkillDataManager.get("ship-a");
        assertTrue(data.getLevel() >= 1);
        verify(textPanel).addPara("%s", Color.CYAN, Color.CYAN, "ExiledSector skill tree");
        verify(textPanel).addPara(eq("%s"), (Color) any(), (Color) any(),
                eq("Every ship in your fleet earned 40 XP from 40 enemy deployment points destroyed or disabled."));
        verify(label).setHighlight("40", "40");
        verify(label).setHighlightColors(Color.YELLOW, Color.YELLOW);
        verify(textPanel).addPara(eq("%s"), eq(Color.GREEN), eq(Color.GREEN), contains("ISS ship-a (Wolf-class) reached level"));
    }

    private void oneShipFleetThatWontLevelUp() {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", ShipLevelConfig.XP_BASE_FIELD_ID)).thenReturn(10000);
        List<FleetMemberAPI> members = List.of(member("ship-a"));
        when(fleetData.getMembersListCopy()).thenReturn(members);
    }

    private void encounterWithDifficulty(Object context) {
        InteractionDialogPlugin plugin = mock(InteractionDialogPlugin.class);
        when(plugin.getContext()).thenReturn(context);
        when(dialog.getPlugin()).thenReturn(plugin);
    }

    private static FleetEncounterContext encounterContext(float difficulty, boolean computed) {
        FleetEncounterContext context = mock(FleetEncounterContext.class);
        when(context.getDifficulty()).thenReturn(difficulty);
        when(context.isComputedDifficulty()).thenReturn(computed);
        return context;
    }

    @Test
    void vanillasBattleDifficultyMultipliesTheXpAndIsReported() {
        oneShipFleetThatWontLevelUp();
        encounterWithDifficulty(encounterContext(2f, true));

        new CombatXpListener().reportPlayerEngagement(engagement(true, 40f));

        assertEquals(80f, ShipSkillDataManager.get("ship-a").getXp());
        verify(textPanel).addPara(eq("%s"), (Color) any(), (Color) any(),
                eq("Includes +100% for the overall battle difficulty."));
    }

    @Test
    void aLostBattleAppliesBothTheLossAndDifficultyMultipliers() {
        oneShipFleetThatWontLevelUp();
        encounterWithDifficulty(encounterContext(3f, true));

        new CombatXpListener().reportPlayerEngagement(engagement(false, 40f));

        assertEquals(40f * ShipLevelConfig.DEFAULT_XP_LOSS_MULTIPLIER * 3f, ShipSkillDataManager.get("ship-a").getXp());
        verify(textPanel).addPara(eq("%s"), (Color) any(), (Color) any(), contains("Includes +200%"));
    }

    @Test
    void aFractionalDifficultyIsAppliedExactlyAndReportedToTheNearestPercent() {
        oneShipFleetThatWontLevelUp();
        encounterWithDifficulty(encounterContext(1.234f, true));

        new CombatXpListener().reportPlayerEngagement(engagement(true, 40f));

        assertEquals(40f * 1.234f, ShipSkillDataManager.get("ship-a").getXp(), 1e-4f);
        verify(textPanel).addPara(eq("%s"), (Color) any(), (Color) any(), contains("Includes +23%"));
    }

    @Test
    void aFightThatEarnedNoXpDoesNotAdvertiseADifficultyBonus() {
        oneShipFleetThatWontLevelUp();
        encounterWithDifficulty(encounterContext(4f, true));

        new CombatXpListener().reportPlayerEngagement(engagement(true, 0f));

        assertEquals(0f, ShipSkillDataManager.get("ship-a").getXp());
        verify(textPanel, never()).addPara(eq("%s"), (Color) any(), (Color) any(), contains("battle difficulty"));
    }

    @Test
    void anEasierBattleNeverReducesTheXp() {
        oneShipFleetThatWontLevelUp();
        encounterWithDifficulty(encounterContext(0.4f, true));

        new CombatXpListener().reportPlayerEngagement(engagement(true, 40f));

        assertEquals(40f, ShipSkillDataManager.get("ship-a").getXp());
        verify(textPanel, never()).addPara(eq("%s"), (Color) any(), (Color) any(), contains("battle difficulty"));
    }

    @Test
    void anUncomputedOrNonVanillaEncounterContextGivesNoBonus() {
        oneShipFleetThatWontLevelUp();

        encounterWithDifficulty(encounterContext(4f, false));
        new CombatXpListener().reportPlayerEngagement(engagement(true, 40f));
        encounterWithDifficulty(new Object());
        new CombatXpListener().reportPlayerEngagement(engagement(true, 40f));

        assertEquals(80f, ShipSkillDataManager.get("ship-a").getXp());
        verify(textPanel, never()).addPara(eq("%s"), (Color) any(), (Color) any(), contains("battle difficulty"));
    }

    private float xpWithDifficultySettings(float difficulty, float strength, float cap) {
        oneShipFleetThatWontLevelUp();
        lunaSettingsMock.when(() -> LunaSettings.getFloat("exiledSector", ShipLevelConfig.XP_DIFFICULTY_STRENGTH_FIELD_ID)).thenReturn(strength);
        lunaSettingsMock.when(() -> LunaSettings.getFloat("exiledSector", ShipLevelConfig.XP_DIFFICULTY_MAX_MULTIPLIER_FIELD_ID)).thenReturn(cap);
        encounterWithDifficulty(encounterContext(difficulty, true));

        new CombatXpListener().reportPlayerEngagement(engagement(true, 40f));

        return ShipSkillDataManager.get("ship-a").getXp();
    }

    @Test
    void theStrengthSettingScalesOnlyTheBonusAboveOne() {
        assertEquals(80f, xpWithDifficultySettings(3f, 0.5f, 6f));
    }

    @Test
    void theMaxMultiplierSettingCapsTheBonus() {
        assertEquals(80f, xpWithDifficultySettings(5f, 1f, 2f));
    }
}
