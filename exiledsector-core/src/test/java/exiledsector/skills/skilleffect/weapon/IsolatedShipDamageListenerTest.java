package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IsolatedShipDamageListenerTest {

    private static final int PLAYER = 0;
    private static final int ENEMY = 1;
    private static final String MOD_ID = "exiledSector_isolatedShipDamage_hunter";

    private final List<Object> gridShips = new ArrayList<>();
    private final float[] elapsedSeconds = {0f};
    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(engine.getTotalElapsedTime(false)).thenAnswer(call -> elapsedSeconds[0]);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(call -> new ArrayList<>(gridShips).iterator());
        mockSector = new MockSector();
        mockSector.global.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private ShipAPI ship(String id, int owner, float x) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.isAlive()).thenReturn(true);
        when(ship.isHulk()).thenReturn(false);
        when(ship.isFighter()).thenReturn(false);
        when(ship.getParentStation()).thenReturn(null);
        gridShips.add(ship);
        return ship;
    }

    private IsolatedShipDamageListener listener(float percent) {
        ShipAPI hunter = ship("hunter", PLAYER, -5000f);
        when(hunter.getMutableStats().getDynamic().getValue(IsolatedShipDamageListener.DAMAGE_PERCENT_KEY, 0f)).thenReturn(percent);
        return new IsolatedShipDamageListener(hunter);
    }

    private static String hit(IsolatedShipDamageListener listener, ShipAPI target, DamageAPI damage) {
        return listener.modifyDamageDealt(null, target, damage, new Vector2f(), false);
    }

    @Test
    void hitsOnAnEnemyShipWithNoAllyNearbyDealTheBonusDamage() {
        IsolatedShipDamageListener listener = listener(10f);
        ShipAPI straggler = ship("straggler", ENEMY, 0f);
        ship("distant escort", ENEMY, 1500f);
        ship("hunter's wingman", PLAYER, 300f);
        DamageAPI damage = mock(DamageAPI.class, RETURNS_DEEP_STUBS);

        assertEquals(MOD_ID, hit(listener, straggler, damage));
        verify(damage.getModifier()).modifyPercent(MOD_ID, 10f);
    }

    @Test
    void anEscortWithinRangeProtectsItsShipButStrikecraftAndWrecksDont() {
        IsolatedShipDamageListener listener = listener(10f);
        ShipAPI escorted = ship("escorted", ENEMY, 0f);
        ship("escort", ENEMY, 900f);
        ShipAPI alone = ship("alone", ENEMY, 10000f);
        ShipAPI fighter = ship("fighter", ENEMY, 10200f);
        when(fighter.isFighter()).thenReturn(true);
        ShipAPI wreck = ship("wreck", ENEMY, 9800f);
        when(wreck.isHulk()).thenReturn(true);

        assertNull(hit(listener, escorted, mock(DamageAPI.class, RETURNS_DEEP_STUBS)));
        assertEquals(MOD_ID, hit(listener, alone, mock(DamageAPI.class, RETURNS_DEEP_STUBS)));
    }

    @Test
    void strikecraftTargetsGetNoBonus() {
        IsolatedShipDamageListener listener = listener(10f);
        ShipAPI fighter = ship("fighter", ENEMY, 0f);
        when(fighter.isFighter()).thenReturn(true);

        assertNull(hit(listener, fighter, mock(DamageAPI.class, RETURNS_DEEP_STUBS)));
    }

    @Test
    void isolationIsRecheckedOnlyAfterAQuarterSecond() {
        IsolatedShipDamageListener listener = listener(10f);
        ShipAPI target = ship("target", ENEMY, 0f);
        assertEquals(MOD_ID, hit(listener, target, mock(DamageAPI.class, RETURNS_DEEP_STUBS)));

        ship("arriving escort", ENEMY, 500f);
        elapsedSeconds[0] = IsolatedShipDamageListener.RECHECK_SECONDS / 2f;
        assertEquals(MOD_ID, hit(listener, target, mock(DamageAPI.class, RETURNS_DEEP_STUBS)));

        elapsedSeconds[0] = IsolatedShipDamageListener.RECHECK_SECONDS;
        assertNull(hit(listener, target, mock(DamageAPI.class, RETURNS_DEEP_STUBS)));
    }
}
