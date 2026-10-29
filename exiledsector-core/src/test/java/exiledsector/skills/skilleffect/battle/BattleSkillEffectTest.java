package exiledsector.skills.skilleffect.battle;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BattleSkillEffectTest {

    private static final float EPSILON = 1e-4f;

    private MockedStatic<Global> globalMock;

    private SettingsAPI settings;

    private MutableShipStatsAPI stats;

    private DynamicStatsAPI dynamic;

    private ShipAPI ship;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
        stats = mock(MutableShipStatsAPI.class);
        dynamic = mock(DynamicStatsAPI.class);
        ship = mock(ShipAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(ship.getMutableStats()).thenReturn(stats);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private StatBonus realDynamicMod(String key) {
        StatBonus bonus = new StatBonus();
        when(dynamic.getMod(key)).thenReturn(bonus);
        return bonus;
    }

    @Test
    void objectiveCaptureRateIsAMultiplierOnTheVanillaStat() {
        MutableStat captureRate = new MutableStat(1f);
        when(dynamic.getStat(Stats.SHIP_OBJECTIVE_CAP_RATE_MULT)).thenReturn(captureRate);

        BattleSkillEffect.OBJECTIVE_CAPTURE_RATE_MULT.apply(stats, "mod_id", 50f);

        assertEquals(1.5f, captureRate.getModifiedValue(), EPSILON);
    }

    private float commandPointRecoveryFor(FleetMemberAPI member, PersonAPI captain) {
        StatBonus commandPointRate = realDynamicMod("command_point_rate_flat");
        when(stats.getFleetMember()).thenReturn(member);
        when(ship.getCaptain()).thenReturn(captain);
        BattleSkillEffect.COMMAND_POINT_RECOVERY_WHILE_FLAGSHIP.advanceInCombat(ship, "mod_id", 250f);
        return commandPointRate.getFlatBonus();
    }

    @Test
    void anNpcShipCaptainedByItsFleetsCommanderForStatsCountsAsTheFlagship() {
        PersonAPI admiral = mock(PersonAPI.class);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getFleetCommanderForStats()).thenReturn(admiral);

        assertEquals(2.5f, commandPointRecoveryFor(member, admiral), EPSILON);
    }

    @Test
    void theFleetCommanderIsUsedWhenThereIsNoCommanderForStats() {
        PersonAPI admiral = mock(PersonAPI.class);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getFleetCommander()).thenReturn(admiral);

        assertEquals(2.5f, commandPointRecoveryFor(member, admiral), EPSILON);
    }

    @Test
    void aShipCaptainedBySomeoneElseOrWithNoFleetMemberIsNotTheFlagship() {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        PersonAPI admiral = mock(PersonAPI.class);
        when(member.getFleetCommanderForStats()).thenReturn(admiral);

        assertEquals(0f, commandPointRecoveryFor(member, mock(PersonAPI.class)), EPSILON);
        assertEquals(0f, commandPointRecoveryFor(null, mock(PersonAPI.class)), EPSILON);
    }
}
