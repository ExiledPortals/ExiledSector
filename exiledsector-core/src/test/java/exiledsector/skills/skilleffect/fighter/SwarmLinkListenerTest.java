package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SwarmLinkListenerTest {

    private final List<Object> gridShips = new ArrayList<>();
    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<>(gridShips).iterator());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI craft(int owner, float x) {
        ShipAPI craft = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(craft.getOwner()).thenReturn(owner);
        when(craft.isFighter()).thenReturn(true);
        when(craft.isAlive()).thenReturn(true);
        when(craft.getLocation()).thenReturn(new Vector2f(x, 0f));
        return craft;
    }

    private static ShipAPI carrier(float percent) {
        ShipAPI carrier = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(carrier.getMutableStats().getDynamic().getValue(SwarmLinkListener.DAMAGE_PERCENT_PER_STRIKECRAFT_KEY, 0f)).thenReturn(percent);
        return carrier;
    }

    @Test
    void everyNearbyAlliedStrikecraftAddsDamageUpToTheCap() {
        assertEquals(6f, SwarmLinkListener.damagePercent(2f, 3, 0f));
        assertEquals(SwarmLinkListener.MAX_DAMAGE_PERCENT, SwarmLinkListener.damagePercent(2f, 15, 0f));
    }

    @Test
    void aMaximumBonusRaisesTheCap() {
        assertEquals(SwarmLinkListener.MAX_DAMAGE_PERCENT + 10f, SwarmLinkListener.damagePercent(2f, 25, 10f));
        assertEquals(SwarmLinkListener.MAX_DAMAGE_PERCENT, SwarmLinkListener.damagePercent(2f, 25, -10f));
    }

    @Test
    void theCarriersMaximumBonusReachesItsStrikecraft() {
        ShipAPI fighter = craft(0, 0f);
        gridShips.add(fighter);
        for (int i = 0; i < 20; i++) {
            gridShips.add(craft(0, 100f));
        }
        ShipAPI carrier = carrier(2f);
        when(carrier.getMutableStats().getDynamic().getValue(SwarmLinkListener.MAX_DAMAGE_BONUS_PERCENT_KEY, 0f)).thenReturn(10f);
        SwarmLinkListener listener = new SwarmLinkListener(fighter, carrier);

        listener.advance(SwarmLinkListener.SCAN_INTERVAL_SECONDS);

        verify(fighter.getMutableStats().getBallisticWeaponDamageMult()).modifyPercent(SwarmLinkListener.DAMAGE_MOD_ID,
                SwarmLinkListener.MAX_DAMAGE_PERCENT + 10f);
    }

    @Test
    void onlyAlliedStrikecraftWithinRangeCountNeverDronesShipsOrEnemies() {
        ShipAPI fighter = craft(0, 0f);
        gridShips.add(fighter);
        gridShips.add(craft(0, 400f));
        gridShips.add(craft(0, 450f));
        gridShips.add(craft(0, 600f));
        gridShips.add(craft(1, 100f));
        ShipAPI drone = craft(0, 100f);
        when(drone.isDrone()).thenReturn(true);
        gridShips.add(drone);
        ShipAPI ship = craft(0, 100f);
        when(ship.isFighter()).thenReturn(false);
        gridShips.add(ship);
        SwarmLinkListener listener = new SwarmLinkListener(fighter, carrier(2f));

        listener.advance(SwarmLinkListener.SCAN_INTERVAL_SECONDS);

        verify(fighter.getMutableStats().getBallisticWeaponDamageMult()).modifyPercent(SwarmLinkListener.DAMAGE_MOD_ID, 4f);
        verify(fighter.getMutableStats().getEnergyWeaponDamageMult()).modifyPercent(SwarmLinkListener.DAMAGE_MOD_ID, 4f);
        verify(fighter.getMutableStats().getMissileWeaponDamageMult()).modifyPercent(SwarmLinkListener.DAMAGE_MOD_ID, 4f);
    }
}
