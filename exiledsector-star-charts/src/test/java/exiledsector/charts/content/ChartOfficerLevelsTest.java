package exiledsector.charts.content;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartOfficerLevelsTest {

    private static final int OFFICER_CAP = 5;
    private static final int COMMANDER_CAP = 7;

    private SettingsAPI settings;
    private MockedStatic<Global> globalMock;
    private final Map<String, SkillSpecAPI> specs = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        when(settings.getInt(ChartOfficerLevels.OFFICER_LEVEL_CAP_SETTING)).thenReturn(OFFICER_CAP);
        when(settings.getInt(ChartOfficerLevels.COMMANDER_LEVEL_CAP_SETTING)).thenReturn(COMMANDER_CAP);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Test
    void levelBonusLiftsBothLimitsAboveTheVanillaCaps() {
        FleetParamsV3 params = new FleetParamsV3();
        params.officerLevelBonus = 10;

        ChartOfficerLevels.liftLevelLimits(params);

        assertEquals(15, params.officerLevelLimit);
        assertEquals(17, params.commanderLevelLimit);
    }

    @Test
    void noLevelBonusKeepsTheVanillaLimits() {
        FleetParamsV3 params = new FleetParamsV3();

        ChartOfficerLevels.liftLevelLimits(params);

        assertEquals(0, params.officerLevelLimit);
        assertEquals(0, params.commanderLevelLimit);
    }

    @Test
    void raisedCapSettingsRaiseTheLimits() {
        when(settings.getInt(ChartOfficerLevels.OFFICER_LEVEL_CAP_SETTING)).thenReturn(6);
        FleetParamsV3 params = new FleetParamsV3();
        params.officerLevelBonus = 4;

        ChartOfficerLevels.liftLevelLimits(params);

        assertEquals(10, params.officerLevelLimit);
    }

    @Test
    void eachLevelOverTheCapMakesOneMoreSkillElite() {
        FakeOfficer officer = officerWithCombatSkills(15, 14);
        officer.skills.put("combat_0", 2f);

        int granted = ChartOfficerLevels.grantEliteSkillsOverCap(officer.person, OFFICER_CAP, new Random(3));

        assertEquals(10, granted);
        assertEquals(11, officer.eliteCount());
    }

    @Test
    void onlyNonEliteCombatSkillsCanBecomeElite() {
        FakeOfficer officer = officerWithCombatSkills(15, 2);
        officer.skill("aptitude", 1f, true, true, false);
        officer.skill("player_only", 1f, true, false, true);
        officer.skill("industry", 1f, false, false, false);
        officer.skill("untrained", 0f, true, false, false);
        officer.skills.put("combat_0", 2f);

        int granted = ChartOfficerLevels.grantEliteSkillsOverCap(officer.person, OFFICER_CAP, new Random(3));

        assertEquals(1, granted);
        assertEquals(2f, officer.skills.get("combat_1"));
        assertEquals(1f, officer.skills.get("aptitude"));
        assertEquals(1f, officer.skills.get("player_only"));
        assertEquals(1f, officer.skills.get("industry"));
        assertEquals(0f, officer.skills.get("untrained"));
    }

    @Test
    void aiCoreCaptainsGetNoExtraEliteSkills() {
        FakeOfficer core = officerWithCombatSkills(15, 8);
        when(core.person.isAICore()).thenReturn(true);

        assertEquals(0, ChartOfficerLevels.grantEliteSkillsOverCap(core.person, OFFICER_CAP, new Random(3)));
        assertEquals(0, core.eliteCount());
    }

    @Test
    void officersAtOrBelowTheCapGetNoExtraEliteSkills() {
        FakeOfficer officer = officerWithCombatSkills(OFFICER_CAP, 6);

        assertEquals(0, ChartOfficerLevels.grantEliteSkillsOverCap(officer.person, OFFICER_CAP, new Random(3)));
    }

    @Test
    void fleetCommanderCountsFromTheCommanderCapAndOfficersFromTheOfficerCap() {
        FakeOfficer commander = officerWithCombatSkills(12, 12);
        FakeOfficer officer = officerWithCombatSkills(12, 12);
        CampaignFleetAPI fleet = fleetOf(commander.person, officer.person);

        ChartOfficerLevels.grantFleetEliteSkills(fleet, 5, new Random(3));

        assertEquals(12 - COMMANDER_CAP, commander.eliteCount());
        assertEquals(12 - OFFICER_CAP, officer.eliteCount());
    }

    @Test
    void fleetWithoutLevelBonusIsLeftAsVanillaBuiltIt() {
        FakeOfficer commander = officerWithCombatSkills(12, 12);
        FakeOfficer officer = officerWithCombatSkills(12, 12);

        ChartOfficerLevels.grantFleetEliteSkills(fleetOf(commander.person, officer.person), 0, new Random(3));

        assertEquals(0, commander.eliteCount());
        assertEquals(0, officer.eliteCount());
    }

    @Test
    void stationCommanderCountsFromTheOfficerCap() {
        FakeOfficer commander = officerWithCombatSkills(9, 10);

        ChartOfficerLevels.grantStationEliteSkills(commander.person, new Random(3));

        assertEquals(9 - OFFICER_CAP, commander.eliteCount());
    }

    @Test
    void sameSeedPicksTheSameSkills() {
        FakeOfficer first = officerWithCombatSkills(9, 12);
        FakeOfficer second = officerWithCombatSkills(9, 12);

        ChartOfficerLevels.grantEliteSkillsOverCap(first.person, OFFICER_CAP, new Random(42));
        ChartOfficerLevels.grantEliteSkillsOverCap(second.person, OFFICER_CAP, new Random(42));

        assertEquals(first.skills, second.skills);
    }

    private CampaignFleetAPI fleetOf(PersonAPI commander, PersonAPI officer) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        FleetDataAPI data = mock(FleetDataAPI.class);
        FleetMemberAPI flagship = mock(FleetMemberAPI.class);
        FleetMemberAPI escort = mock(FleetMemberAPI.class);
        FleetMemberAPI unofficered = mock(FleetMemberAPI.class);
        PersonAPI defaultCaptain = mock(PersonAPI.class);
        when(defaultCaptain.isDefault()).thenReturn(true);
        when(flagship.getCaptain()).thenReturn(commander);
        when(escort.getCaptain()).thenReturn(officer);
        when(unofficered.getCaptain()).thenReturn(defaultCaptain);
        when(data.getMembersListCopy()).thenReturn(List.of(flagship, escort, unofficered));
        when(fleet.getFleetData()).thenReturn(data);
        when(fleet.getCommander()).thenReturn(commander);
        return fleet;
    }

    private FakeOfficer officerWithCombatSkills(int level, int combatSkills) {
        FakeOfficer officer = new FakeOfficer(level);
        for (int i = 0; i < combatSkills; i++) {
            officer.skill("combat_" + i, 1f, true, false, false);
        }
        return officer;
    }

    private final class FakeOfficer {
        final PersonAPI person = mock(PersonAPI.class);
        final Map<String, Float> skills = new LinkedHashMap<>();

        FakeOfficer(int level) {
            MutableCharacterStatsAPI stats = mock(MutableCharacterStatsAPI.class);
            when(person.getStats()).thenReturn(stats);
            when(stats.getLevel()).thenReturn(level);
            when(stats.getSkillsCopy()).thenAnswer(invocation -> skillLevels());
            Mockito.doAnswer(invocation -> {
                skills.merge(invocation.getArgument(0), 1f, Float::sum);
                return null;
            }).when(stats).increaseSkill(anyString());
        }

        void skill(String id, float level, boolean combatOfficerSkill, boolean aptitudeEffect, boolean elitePlayerOnly) {
            SkillSpecAPI spec = mock(SkillSpecAPI.class);
            when(spec.getId()).thenReturn(id);
            when(spec.isCombatOfficerSkill()).thenReturn(combatOfficerSkill);
            when(spec.isAptitudeEffect()).thenReturn(aptitudeEffect);
            when(spec.hasTag(Skills.TAG_ELITE_PLAYER_ONLY)).thenReturn(elitePlayerOnly);
            specs.put(id, spec);
            skills.put(id, level);
        }

        long eliteCount() {
            return skills.values().stream().filter(level -> level >= ChartOfficerLevels.ELITE_SKILL_LEVEL).count();
        }

        private List<SkillLevelAPI> skillLevels() {
            List<SkillLevelAPI> levels = new ArrayList<>();
            for (Map.Entry<String, Float> entry : skills.entrySet()) {
                SkillLevelAPI skillLevel = mock(SkillLevelAPI.class);
                when(skillLevel.getLevel()).thenReturn(entry.getValue());
                when(skillLevel.getSkill()).thenReturn(specs.get(entry.getKey()));
                levels.add(skillLevel);
            }
            return levels;
        }
    }
}
