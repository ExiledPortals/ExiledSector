package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class FlareBaysListenerTest {

    private MockSector mockSector;
    private CombatEngineAPI engine;
    private CollisionGridAPI missileGrid;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        engine = mock(CombatEngineAPI.class);
        missileGrid = mock(CollisionGridAPI.class);
        when(engine.getMissileGrid()).thenReturn(missileGrid);
        mockSector.global.when(Global::getCombatEngine).thenReturn(engine);
        mockSector.global.when(Global::getSoundPlayer).thenReturn(mock(SoundPlayerAPI.class));
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private static ShipAPI ship(int owner, float flaresPerBurst) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic().getValue(FlareBaysListener.FLARES_PER_BURST_KEY, 0f)).thenReturn(flaresPerBurst);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        when(ship.getVelocity()).thenReturn(new Vector2f());
        return ship;
    }

    private static MissileAPI missileHoming(ShipAPI target, int owner) {
        MissileAPI missile = mock(MissileAPI.class);
        MissileAIPlugin guidance = mock(MissileAIPlugin.class, withSettings().extraInterfaces(GuidedMissileAI.class));
        when(((GuidedMissileAI) guidance).getTarget()).thenReturn(target);
        when(missile.getMissileAI()).thenReturn(guidance);
        when(missile.getOwner()).thenReturn(owner);
        return missile;
    }

    @Test
    void onlyHostileMissilesGuidedOntoTheShipTriggerFlares() {
        ShipAPI ship = ship(0, 6f);
        ShipAPI ally = ship(0, 0f);
        MissileAPI hostileFlare = missileHoming(ship, 1);
        when(hostileFlare.isFlare()).thenReturn(true);

        assertTrue(FlareBaysListener.homesOn(missileHoming(ship, 1), ship));
        assertFalse(FlareBaysListener.homesOn(missileHoming(ally, 1), ship));
        assertFalse(FlareBaysListener.homesOn(missileHoming(ship, 0), ship));
        assertFalse(FlareBaysListener.homesOn(hostileFlare, ship));
        assertFalse(FlareBaysListener.homesOn(ship, ship));
    }

    @Test
    void theBurstSizeIsRoundedAndCapped() {
        assertEquals(6, FlareBaysListener.flaresPerBurst(6.2f));
        assertEquals(0, FlareBaysListener.flaresPerBurst(-3f));
        assertEquals(20, FlareBaysListener.flaresPerBurst(500f));
    }

    @Test
    void aHomingMissileFiresOneBurstThenTheBaysCoolDown() {
        ShipAPI ship = ship(0, 3f);
        MissileAPI missile = missileHoming(ship, 1);
        when(missileGrid.getCheckIterator(any(Vector2f.class), anyFloat(), anyFloat()))
                .thenAnswer(call -> List.<Object>of(missile).iterator());
        FlareBaysListener listener = new FlareBaysListener(ship, new Random(1));

        listener.advance(0.3f);
        listener.advance(0.1f);
        listener.advance(0.1f);
        for (int i = 0; i < 10; i++) {
            listener.advance(0.5f);
        }

        verify(engine, times(3)).spawnProjectile(eq(ship), isNull(), eq(FlareBaysListener.FLARE_WEAPON_ID), any(Vector2f.class), anyFloat(),
                any(Vector2f.class));
    }

    @Test
    void aPhasedShipFiresNoFlares() {
        ShipAPI ship = ship(0, 3f);
        when(ship.isPhased()).thenReturn(true);
        MissileAPI missile = missileHoming(ship, 1);
        when(missileGrid.getCheckIterator(any(Vector2f.class), anyFloat(), anyFloat()))
                .thenAnswer(call -> List.<Object>of(missile).iterator());
        FlareBaysListener listener = new FlareBaysListener(ship, new Random(1));

        listener.advance(0.5f);

        verify(engine, never()).spawnProjectile(any(), any(), any(), any(), anyFloat(), any());
    }
}
