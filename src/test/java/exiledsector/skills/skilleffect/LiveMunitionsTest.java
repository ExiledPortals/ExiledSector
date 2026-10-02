package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.AmmoTrackerAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiveMunitionsTest {

    private static final String MOD_ID = "exiledSector_liveMunitions_ship";

    private MockedStatic<Global> globalMock;
    private CombatEngineAPI engine;
    private CargoAPI cargo;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.getCustomData()).thenReturn(new HashMap<>());
        when(engine.isInCampaign()).thenReturn(true);
        cargo = mock(CargoAPI.class);
        when(cargo.getCrew()).thenReturn(2);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getCargo()).thenReturn(cargo);
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPlayerFleet()).thenReturn(fleet);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
        globalMock.when(Global::getSector).thenReturn(sector);
        LiveMunitionsCrew.drainPendingDeaths();
    }

    @AfterEach
    void tearDown() {
        LiveMunitionsCrew.drainPendingDeaths();
        globalMock.close();
    }

    private record Armed(ShipAPI ship, WeaponAPI launcher, MutableStat damage, StatBonus speed, HeartlessStacks stacks) {
    }

    private static Armed armed(float deathChancePerStack, int stackCount) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getId()).thenReturn("ship");
        when(ship.isAlive()).thenReturn(true);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        Map.of(LiveMunitionsListener.CREW_DEATH_CHANCE_PERCENT_PER_STACK_KEY, deathChancePerStack,
                LiveMunitionsListener.MISSILE_DAMAGE_PERCENT_PER_STACK_KEY, 3f,
                LiveMunitionsListener.MISSILE_SPEED_PERCENT_PER_STACK_KEY, 3f)
                .forEach((key, value) -> when(dynamic.getValue(key, 0f)).thenReturn(value));
        MutableStat damage = mock(MutableStat.class);
        StatBonus speed = mock(StatBonus.class);
        when(stats.getMissileWeaponDamageMult()).thenReturn(damage);
        when(stats.getMissileMaxSpeedBonus()).thenReturn(speed);

        WeaponAPI launcher = mock(WeaponAPI.class);
        when(launcher.getType()).thenReturn(WeaponAPI.WeaponType.MISSILE);
        when(launcher.usesAmmo()).thenReturn(true);
        when(launcher.getAmmo()).thenReturn(10);
        when(ship.getAllWeapons()).thenReturn(List.of(launcher));

        HeartlessStacks stacks = new HeartlessStacks(ship, new Random(1));
        when(ship.getListeners(HeartlessStacks.class)).thenReturn(List.of(stacks));
        for (int i = 0; i < stackCount; i++) {
            stacks.gain();
        }
        return new Armed(ship, launcher, damage, speed, stacks);
    }

    @Test
    void eachStackAddsMissileDamageAndSpeedWhileTheFleetHasCrew() {
        Armed armed = armed(3f, 2);
        LiveMunitionsListener listener = new LiveMunitionsListener(armed.ship(), new Random(1));

        listener.advance(0.016f);

        verify(armed.damage()).modifyPercent(MOD_ID, 6f);
        verify(armed.speed()).modifyPercent(MOD_ID, 6f);
    }

    @Test
    void missilesFiredKillCrewAtTheStackChanceUntilNobodyIsLeftThenTheBonusesStop() {
        Armed armed = armed(50f, 2);
        LiveMunitionsListener listener = new LiveMunitionsListener(armed.ship(), new Random(1));
        listener.advance(0.016f);

        when(armed.launcher().getAmmo()).thenReturn(5);
        listener.advance(0.016f);

        assertEquals(2, LiveMunitionsCrew.drainPendingDeaths());
        verify(armed.damage()).unmodify(MOD_ID);
        verify(armed.speed()).unmodify(MOD_ID);
    }

    @Test
    void aMissileFiredInTheSameFrameAsAReloadStillCounts() {
        Armed armed = armed(50f, 2);
        AmmoTrackerAPI tracker = mock(AmmoTrackerAPI.class);
        when(tracker.getReloadSize()).thenReturn(3f);
        when(tracker.getReloadProgress()).thenReturn(0.9f);
        when(armed.launcher().getAmmoTracker()).thenReturn(tracker);
        when(armed.launcher().getMaxAmmo()).thenReturn(10);
        when(armed.launcher().getAmmo()).thenReturn(5);
        LiveMunitionsListener listener = new LiveMunitionsListener(armed.ship(), new Random(1));
        listener.advance(0.016f);

        when(tracker.getReloadProgress()).thenReturn(0.1f);
        when(armed.launcher().getAmmo()).thenReturn(7);
        listener.advance(0.016f);

        assertEquals(1, LiveMunitionsCrew.drainPendingDeaths());
    }

    @Test
    void withoutStacksNobodyDiesAndNothingIsBoosted() {
        Armed armed = armed(100f, 0);
        LiveMunitionsListener listener = new LiveMunitionsListener(armed.ship(), new Random(1));
        listener.advance(0.016f);

        when(armed.launcher().getAmmo()).thenReturn(4);
        listener.advance(0.016f);

        assertEquals(0, LiveMunitionsCrew.drainPendingDeaths());
        verify(armed.damage(), never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void shipsOutsideThePlayersFleetNeverSpendItsCrew() {
        Armed armed = armed(100f, 1);
        when(armed.ship().getOwner()).thenReturn(1);
        LiveMunitionsListener listener = new LiveMunitionsListener(armed.ship(), new Random(1));
        listener.advance(0.016f);

        when(armed.launcher().getAmmo()).thenReturn(0);
        listener.advance(0.016f);

        assertEquals(0, LiveMunitionsCrew.drainPendingDeaths());
        verify(armed.damage(), never()).unmodify(MOD_ID);
    }

    @Test
    void simulatorDeathsAreNeverChargedToTheFleet() {
        when(engine.isInCampaignSim()).thenReturn(true);
        Armed armed = armed(100f, 1);
        LiveMunitionsListener listener = new LiveMunitionsListener(armed.ship(), new Random(1));
        listener.advance(0.016f);

        when(armed.launcher().getAmmo()).thenReturn(9);
        listener.advance(0.016f);

        assertEquals(0, LiveMunitionsCrew.drainPendingDeaths());
    }

    @Test
    void theCrewPoolIsCountedOncePerBattleAndSharedByEveryShip() {
        LiveMunitionsCrew first = LiveMunitionsCrew.forCurrentCombat();
        LiveMunitionsCrew second = LiveMunitionsCrew.forCurrentCombat();

        first.sacrifice();
        second.sacrifice();
        second.sacrifice();

        assertSame(first, second);
        assertFalse(first.hasCrew());
        assertEquals(2, LiveMunitionsCrew.drainPendingDeaths());
    }
}
