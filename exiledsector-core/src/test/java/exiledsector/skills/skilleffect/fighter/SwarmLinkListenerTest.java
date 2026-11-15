package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.CombatQueries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SwarmLinkListenerTest {

    private final List<ShipAPI> ships = new ArrayList<>();
    private final SwarmLinkCensus census = new SwarmLinkCensus();
    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = combat.engine();
        when(engine.getShips()).thenAnswer(invocation -> new ArrayList<>(ships));
    }

    private static ShipAPI craft(int owner, float x) {
        ShipAPI craft = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(craft.getOwner()).thenReturn(owner);
        when(craft.isFighter()).thenReturn(true);
        when(craft.isDrone()).thenReturn(false);
        when(craft.isHulk()).thenReturn(false);
        when(craft.isAlive()).thenReturn(true);
        when(craft.getVariant()).thenReturn(mock(ShipVariantAPI.class));
        when(craft.getLocation()).thenReturn(new Vector2f(x, 0f));
        return craft;
    }

    private static ShipAPI carrier(float percent) {
        ShipAPI carrier = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(carrier.getMutableStats().getDynamic().getValue(SwarmLinkListener.DAMAGE_PERCENT_PER_STRIKECRAFT_KEY, 0f)).thenReturn(percent);
        return carrier;
    }

    private void link(ShipAPI fighter, ShipAPI carrier) {
        census.add(new SwarmLinkListener(fighter, carrier));
    }

    private static void verifyDamagePercent(ShipAPI fighter, float percent) {
        verify(fighter.getMutableStats().getBallisticWeaponDamageMult()).modifyPercent(SwarmLinkListener.DAMAGE_MOD_ID, percent);
    }

    @Test
    void everyNearbyAlliedStrikecraftAddsDamageUpToTheCap() {
        float cap = SwarmLinkListener.maxDamagePercent(0f);
        assertEquals(6f, SwarmLinkListener.damagePercent(2f, 3, cap));
        assertEquals(SwarmLinkListener.MAX_DAMAGE_PERCENT, SwarmLinkListener.damagePercent(2f, 15, cap));
    }

    @Test
    void aMaximumBonusRaisesTheCap() {
        assertEquals(SwarmLinkListener.MAX_DAMAGE_PERCENT + 10f, SwarmLinkListener.damagePercent(2f, 25, SwarmLinkListener.maxDamagePercent(10f)));
    }

    @Test
    void countingStopsOnlyOnceMoreStrikecraftCouldNotRaiseTheBonus() {
        assertEquals(11, SwarmLinkListener.strikecraftNeededToReachCap(2f, 20f));
        assertEquals(8, SwarmLinkListener.strikecraftNeededToReachCap(3f, 20f));
        assertEquals(0, SwarmLinkListener.strikecraftNeededToReachCap(0f, 20f));
        assertEquals(0, SwarmLinkListener.strikecraftNeededToReachCap(2f, 0f));
        int needed = SwarmLinkListener.strikecraftNeededToReachCap(3f, 20f);
        assertEquals(SwarmLinkListener.damagePercent(3f, 50, 20f), SwarmLinkListener.damagePercent(3f, needed, 20f));
    }

    @Test
    void theCarriersMaximumBonusReachesItsStrikecraft() {
        ShipAPI fighter = craft(0, 0f);
        ships.add(fighter);
        for (int i = 0; i < 20; i++) {
            ships.add(craft(0, 100f));
        }
        ShipAPI carrier = carrier(2f);
        when(carrier.getMutableStats().getDynamic().getValue(SwarmLinkListener.MAX_DAMAGE_FLAT_KEY, 0f)).thenReturn(10f);
        link(fighter, carrier);

        census.refresh(ships);

        verifyDamagePercent(fighter, SwarmLinkListener.MAX_DAMAGE_PERCENT + 10f);
    }

    @Test
    void onlyAlliedStrikecraftWithinRangeCountNeverDronesShipsEnemiesWrecksOrAfterimages() {
        ShipAPI fighter = craft(0, 0f);
        ships.add(fighter);
        ships.add(craft(0, 400f));
        ships.add(craft(0, 450f));
        ships.add(craft(0, 600f));
        ships.add(craft(1, 100f));
        ShipAPI drone = craft(0, 100f);
        when(drone.isDrone()).thenReturn(true);
        ships.add(drone);
        ShipAPI ship = craft(0, 100f);
        when(ship.isFighter()).thenReturn(false);
        ships.add(ship);
        ShipAPI wreck = craft(0, 100f);
        when(wreck.isHulk()).thenReturn(true);
        ships.add(wreck);
        ShipAPI afterimage = craft(0, 100f);
        when(afterimage.getVariant().hasTag(CombatQueries.AFTERIMAGE_TAG)).thenReturn(true);
        ships.add(afterimage);
        link(fighter, carrier(2f));

        census.refresh(ships);

        verifyDamagePercent(fighter, 4f);
        verify(fighter.getMutableStats().getEnergyWeaponDamageMult()).modifyPercent(SwarmLinkListener.DAMAGE_MOD_ID, 4f);
        verify(fighter.getMutableStats().getMissileWeaponDamageMult()).modifyPercent(SwarmLinkListener.DAMAGE_MOD_ID, 4f);
    }

    @Test
    void everyLinkedFighterCountsTheSwarmAroundItself() {
        ShipAPI left = craft(0, 0f);
        ShipAPI middle = craft(0, 400f);
        ShipAPI right = craft(0, 800f);
        ships.addAll(List.of(left, middle, right));
        link(left, carrier(2f));
        link(middle, carrier(2f));
        link(right, carrier(2f));

        census.refresh(ships);

        verifyDamagePercent(left, 2f);
        verifyDamagePercent(middle, 4f);
        verifyDamagePercent(right, 2f);
    }

    @Test
    void aDestroyedFighterLosesItsBonusAndLeavesTheCensus() {
        ShipAPI fighter = craft(0, 0f);
        ships.addAll(List.of(fighter, craft(0, 100f)));
        link(fighter, carrier(2f));
        census.refresh(ships);
        verifyDamagePercent(fighter, 2f);

        when(fighter.isAlive()).thenReturn(false);
        when(fighter.isHulk()).thenReturn(true);
        census.refresh(ships);
        verify(fighter.getMutableStats().getBallisticWeaponDamageMult()).unmodify(SwarmLinkListener.DAMAGE_MOD_ID);

        when(fighter.isAlive()).thenReturn(true);
        when(fighter.isHulk()).thenReturn(false);
        census.refresh(ships);
        verify(fighter.getMutableStats().getBallisticWeaponDamageMult(), times(1)).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void aFighterOutOfPlayKeepsItsBonusUntilItReturns() {
        ShipAPI fighter = craft(0, 0f);
        link(fighter, carrier(2f));
        ships.add(craft(0, 100f));

        census.refresh(ships);
        verify(fighter.getMutableStats().getBallisticWeaponDamageMult(), never()).modifyPercent(anyString(), anyFloat());

        ships.add(fighter);
        census.refresh(ships);
        verifyDamagePercent(fighter, 2f);
    }

    @Test
    void theCensusRefreshesRightAfterANewFighterLinksThenEveryHalfSecond() {
        ShipAPI fighter = craft(0, 0f);
        ships.addAll(List.of(fighter, craft(0, 100f)));
        link(fighter, carrier(2f));

        census.advance(0.01f, List.of());
        verifyDamagePercent(fighter, 2f);

        ships.add(craft(0, 200f));
        census.advance(SwarmLinkCensus.REFRESH_INTERVAL_SECONDS / 2f, List.of());
        verify(fighter.getMutableStats().getBallisticWeaponDamageMult(), never()).modifyPercent(anyString(), eq(4f));

        census.advance(SwarmLinkCensus.REFRESH_INTERVAL_SECONDS / 2f, List.of());
        verifyDamagePercent(fighter, 4f);
    }
}
