package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class FlagshipPaceListenerTest {

    private static final float EPSILON = 1e-4f;

    private static ShipAPI ship(int owner, PersonAPI captain, float hullTopSpeed) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getCaptain()).thenReturn(captain);
        when(ship.isAlive()).thenReturn(true);
        when(ship.isHulk()).thenReturn(false);
        when(ship.isFighter()).thenReturn(false);
        when(ship.getHullSize()).thenReturn(ShipAPI.HullSize.CRUISER);
        when(ship.getHullSpec().getEngineSpec().getMaxSpeed()).thenReturn(hullTopSpeed);
        return ship;
    }

    private static ShipAPI escort(PersonAPI commander, MutableStat topSpeed) {
        ShipAPI escort = ship(0, mock(PersonAPI.class), 60f);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getFleetCommanderForStats()).thenReturn(commander);
        when(escort.getMutableStats().getFleetMember()).thenReturn(member);
        when(escort.getMutableStats().getDynamic().getValue(FlagshipPaceListener.FLAGSHIP_SPEED_SHARE_PERCENT_KEY, 0f)).thenReturn(50f);
        when(escort.getMutableStats().getMaxSpeed()).thenReturn(topSpeed);
        return escort;
    }

    private static CombatEngineAPI engine(List<ShipAPI> ships) {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getShips()).thenReturn(ships);
        return engine;
    }

    @Test
    void addsHalfTheDeployedFlagshipsHullTopSpeedUntilItIsGone() {
        PersonAPI commander = mock(PersonAPI.class);
        MutableStat topSpeed = new MutableStat(60f);
        ShipAPI escort = escort(commander, topSpeed);
        ShipAPI flagship = ship(0, commander, 30f);
        List<ShipAPI> ships = new ArrayList<>(List.of(escort, flagship, ship(1, commander, 300f)));
        FlagshipPaceListener listener = new FlagshipPaceListener(escort);

        CombatEngineAPI engine = engine(ships);

        try (MockedStatic<Global> global = mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            listener.advance(FlagshipPaceListener.SCAN_INTERVAL_SECONDS);
            assertEquals(75f, topSpeed.getModifiedValue(), EPSILON);

            ships.remove(flagship);
            listener.advance(FlagshipPaceListener.SCAN_INTERVAL_SECONDS);
            assertEquals(60f, topSpeed.getModifiedValue(), EPSILON);
        }
    }

    @Test
    void capsTheBonusByTheEscortsHullSize() {
        PersonAPI commander = mock(PersonAPI.class);
        MutableStat topSpeed = new MutableStat(60f);
        ShipAPI escort = escort(commander, topSpeed);
        when(escort.getHullSize()).thenReturn(ShipAPI.HullSize.CAPITAL_SHIP);
        ShipAPI flagship = ship(0, commander, 100f);

        CombatEngineAPI engine = engine(List.of(escort, flagship));

        try (MockedStatic<Global> global = mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            new FlagshipPaceListener(escort).advance(FlagshipPaceListener.SCAN_INTERVAL_SECONDS);
            assertEquals(75f, topSpeed.getModifiedValue(), EPSILON);
        }
    }

    @Test
    void theFlagshipItselfGainsNothing() {
        PersonAPI commander = mock(PersonAPI.class);
        MutableStat topSpeed = new MutableStat(60f);
        ShipAPI flagship = escort(commander, topSpeed);
        when(flagship.getCaptain()).thenReturn(commander);

        CombatEngineAPI engine = engine(List.of(flagship));

        try (MockedStatic<Global> global = mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            new FlagshipPaceListener(flagship).advance(FlagshipPaceListener.SCAN_INTERVAL_SECONDS);
            assertEquals(60f, topSpeed.getModifiedValue(), EPSILON);
        }
    }
}
