package exiledlib.skills.skilleffect.aura;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledlib.skills.skilleffect.SkillEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EscortListenerTest {

    private static final String MANEUVER_BONUS_KEY = "exiledLib_escortManeuverBonusPercent";

    private static final String SPEED_BONUS_KEY = "exiledLib_escortSpeedBonusPercent";

    private static final String WEAPON_RANGE_BONUS_KEY = "exiledLib_escortWeaponRangeBonusPercent";

    private static final String ESCORT_BONUS_MOD_ID = "exiledLib_escortBonus";

    private final List<ShipAPI> ships = new ArrayList<>();

    private final List<Object> gridShips = new ArrayList<>();

    private final List<Object> gridAsteroids = new ArrayList<>();

    private MockedStatic<Global> globalMock;

    private CombatEngineAPI engine;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.getShips()).thenReturn(ships);
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        CollisionGridAPI asteroidGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(engine.getAsteroidGrid()).thenReturn(asteroidGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<>(gridShips).iterator());
        when(asteroidGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<>(gridAsteroids).iterator());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI ship(float x, float y, Map<String, Float> dynamicValues) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        dynamicValues.forEach((key, value) -> when(dynamic.getValue(key, 0f)).thenReturn(value));
        when(ship.getLocation()).thenReturn(new Vector2f(x, y));
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    private static <T> T attachedListener(SkillEffect effect, ShipAPI ship, Class<T> type) {
        effect.applyAfterShipCreation(ship, "mod_id", 0f);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ship).addListener(captor.capture());
        return type.cast(captor.getValue());
    }

    private record EscortStats(MutableStat acceleration, MutableStat deceleration, MutableStat maxTurnRate,
                               MutableStat turnAcceleration, MutableStat maxSpeed, StatBonus ballisticRange, StatBonus energyRange) {
    }

    private static EscortStats escortStats(ShipAPI ship) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        EscortStats escort = new EscortStats(mock(MutableStat.class), mock(MutableStat.class), mock(MutableStat.class),
                mock(MutableStat.class), mock(MutableStat.class), mock(StatBonus.class), mock(StatBonus.class));
        when(stats.getAcceleration()).thenReturn(escort.acceleration());
        when(stats.getDeceleration()).thenReturn(escort.deceleration());
        when(stats.getMaxTurnRate()).thenReturn(escort.maxTurnRate());
        when(stats.getTurnAcceleration()).thenReturn(escort.turnAcceleration());
        when(stats.getMaxSpeed()).thenReturn(escort.maxSpeed());
        when(stats.getBallisticWeaponRangeBonus()).thenReturn(escort.ballisticRange());
        when(stats.getEnergyWeaponRangeBonus()).thenReturn(escort.energyRange());
        return escort;
    }

    private void deploy(ShipAPI... deployed) {
        ships.addAll(List.of(deployed));
        gridShips.addAll(List.of(deployed));
    }

    private ShipAPI escortAt(float x, HullSize size) {
        ShipAPI escort = ship(x, 0f, Map.of(MANEUVER_BONUS_KEY, 20f, SPEED_BONUS_KEY, 10f, WEAPON_RANGE_BONUS_KEY, 15f));
        when(escort.getHullSize()).thenReturn(size);
        when(escort.getShieldCenterEvenIfNoShield()).thenReturn(new Vector2f(x, 0f));
        when(escort.getShieldRadiusEvenIfNoShield()).thenReturn(100f);
        return escort;
    }

    private ShipAPI friendlyAt(float x, HullSize size) {
        ShipAPI friendly = ship(x, 0f, Map.of());
        when(friendly.getHullSize()).thenReturn(size);
        when(friendly.getShieldCenterEvenIfNoShield()).thenReturn(new Vector2f(x, 0f));
        when(friendly.getShieldRadiusEvenIfNoShield()).thenReturn(100f);
        return friendly;
    }

    @Test
    void anEscortCloseToALargerFriendlyShipGetsItsFullBonuses() {
        ShipAPI escort = escortAt(0f, HullSize.FRIGATE);
        EscortStats stats = escortStats(escort);
        deploy(escort, friendlyAt(500f, HullSize.CRUISER));
        AdvanceableListener listener = attachedListener(AuraSkillEffect.ESCORT_MANEUVER_BONUS_PERCENT, escort, AdvanceableListener.class);

        listener.advance(2f);

        verify(stats.acceleration()).modifyPercent(ESCORT_BONUS_MOD_ID, 20f);
        verify(stats.deceleration()).modifyPercent(ESCORT_BONUS_MOD_ID, 20f);
        verify(stats.maxTurnRate()).modifyPercent(ESCORT_BONUS_MOD_ID, 20f);
        verify(stats.turnAcceleration()).modifyPercent(ESCORT_BONUS_MOD_ID, 40f);
        verify(stats.maxSpeed()).modifyPercent(ESCORT_BONUS_MOD_ID, 10f);
        verify(stats.ballisticRange()).modifyPercent(ESCORT_BONUS_MOD_ID, 15f);
        verify(stats.energyRange()).modifyPercent(ESCORT_BONUS_MOD_ID, 15f);
    }

    @Test
    void theBonusesFadeOutOverFiveHundredSuPastTheSevenHundredSuEscortRange() {
        ShipAPI escort = escortAt(0f, HullSize.FRIGATE);
        EscortStats stats = escortStats(escort);
        deploy(escort, friendlyAt(1100f, HullSize.CRUISER));
        AdvanceableListener listener = attachedListener(AuraSkillEffect.ESCORT_MANEUVER_BONUS_PERCENT, escort, AdvanceableListener.class);

        listener.advance(2f);

        verify(stats.acceleration()).modifyPercent(ESCORT_BONUS_MOD_ID, 10f);
        verify(stats.maxSpeed()).modifyPercent(ESCORT_BONUS_MOD_ID, 5f);
    }

    @Test
    void aDestroyerEscortingACapitalGetsDoubleBonuses() {
        ShipAPI escort = escortAt(0f, HullSize.DESTROYER);
        when(escort.isDestroyer()).thenReturn(true);
        EscortStats stats = escortStats(escort);
        ShipAPI capital = friendlyAt(500f, HullSize.CAPITAL_SHIP);
        when(capital.isCapital()).thenReturn(true);
        deploy(escort, capital);
        AdvanceableListener listener = attachedListener(AuraSkillEffect.ESCORT_MANEUVER_BONUS_PERCENT, escort, AdvanceableListener.class);

        listener.advance(2f);

        verify(stats.acceleration()).modifyPercent(ESCORT_BONUS_MOD_ID, 40f);
    }

    @Test
    void anEscortTakesTheLargerShipThatGivesTheBiggestBonusRatherThanTheNearestOne() {
        ShipAPI escort = escortAt(0f, HullSize.DESTROYER);
        when(escort.isDestroyer()).thenReturn(true);
        EscortStats stats = escortStats(escort);
        ShipAPI capital = friendlyAt(1100f, HullSize.CAPITAL_SHIP);
        when(capital.isCapital()).thenReturn(true);
        deploy(escort, friendlyAt(1000f, HullSize.CRUISER), capital);
        AdvanceableListener listener = attachedListener(AuraSkillEffect.ESCORT_MANEUVER_BONUS_PERCENT, escort, AdvanceableListener.class);

        listener.advance(2f);

        verify(stats.acceleration()).modifyPercent(ESCORT_BONUS_MOD_ID, 20f);
    }

    @Test
    void withoutALargerFriendlyShipTheBonusesAreRemoved() {
        ShipAPI escort = escortAt(0f, HullSize.CRUISER);
        EscortStats stats = escortStats(escort);
        ShipAPI enemyCapital = friendlyAt(300f, HullSize.CAPITAL_SHIP);
        when(enemyCapital.getOwner()).thenReturn(1);
        deploy(escort, friendlyAt(300f, HullSize.FRIGATE), enemyCapital);
        AdvanceableListener listener = attachedListener(AuraSkillEffect.ESCORT_MANEUVER_BONUS_PERCENT, escort, AdvanceableListener.class);

        listener.advance(2f);

        verify(stats.acceleration()).unmodify(ESCORT_BONUS_MOD_ID);
        verify(stats.maxSpeed()).unmodify(ESCORT_BONUS_MOD_ID);
        verify(stats.ballisticRange()).unmodify(ESCORT_BONUS_MOD_ID);
        verify(stats.acceleration(), never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void theEscortOnlyReevaluatesAboutOnceASecond() {
        ShipAPI escort = escortAt(0f, HullSize.FRIGATE);
        EscortStats stats = escortStats(escort);
        deploy(escort, friendlyAt(500f, HullSize.CRUISER));
        AdvanceableListener listener = attachedListener(AuraSkillEffect.ESCORT_MANEUVER_BONUS_PERCENT, escort, AdvanceableListener.class);

        listener.advance(0.5f);

        verify(stats.acceleration(), never()).modifyPercent(anyString(), anyFloat());
        verify(stats.acceleration(), never()).unmodify(anyString());
    }

    private static List<Float> recordedRangePercents(EscortStats stats) {
        List<Float> percents = new ArrayList<>();
        doAnswer(call -> percents.add(call.getArgument(1))).when(stats.energyRange()).modifyPercent(eq(ESCORT_BONUS_MOD_ID), anyFloat());
        return percents;
    }

    private static void advanceFrames(AdvanceableListener listener, int frames) {
        for (int frame = 0; frame < frames; frame++) {
            listener.advance(0.1f);
        }
    }

    private static void assertPercents(List<Float> expected, List<Float> actual) {
        assertEquals(expected.size(), actual.size(), "percents " + actual);
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i), 0.01f, "percents " + actual);
        }
    }

    @Test
    void theRangeBonusGrowsOverHalfASecondSoBeamsLengthenSmoothly() {
        ShipAPI escort = escortAt(0f, HullSize.FRIGATE);
        EscortStats stats = escortStats(escort);
        List<Float> percents = recordedRangePercents(stats);
        deploy(escort, friendlyAt(500f, HullSize.CRUISER));
        AdvanceableListener listener = attachedListener(AuraSkillEffect.ESCORT_MANEUVER_BONUS_PERCENT, escort, AdvanceableListener.class);

        advanceFrames(listener, 17);

        assertPercents(List.of(3f, 6f, 9f, 12f, 15f), percents);
    }

    @Test
    void theRangeBonusShrinksOverHalfASecondBeforeItIsRemoved() {
        ShipAPI escort = escortAt(0f, HullSize.FRIGATE);
        EscortStats stats = escortStats(escort);
        List<Float> percents = recordedRangePercents(stats);
        deploy(escort, friendlyAt(500f, HullSize.CRUISER));
        AdvanceableListener listener = attachedListener(AuraSkillEffect.ESCORT_MANEUVER_BONUS_PERCENT, escort, AdvanceableListener.class);
        advanceFrames(listener, 17);
        percents.clear();
        gridShips.clear();
        gridShips.add(escort);

        advanceFrames(listener, 9);

        assertPercents(List.of(12f, 9f, 6f, 3f), percents);
        verify(stats.energyRange(), times(1)).unmodify(ESCORT_BONUS_MOD_ID);
    }
}
