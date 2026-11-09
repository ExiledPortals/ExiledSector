package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class FlagshipGuardTest {

    private static final float EPSILON = 1e-4f;
    private static final String MOD_ID = "guard";

    private final MutableStat hullDamageTaken = new MutableStat(1f);
    private final MutableStat armorDamageTaken = new MutableStat(1f);
    private final MutableStat shieldDamageTaken = new MutableStat(1f);

    private static ShipAPI ship(int owner, PersonAPI captain, float x) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getCaptain()).thenReturn(captain);
        when(ship.isAlive()).thenReturn(true);
        when(ship.isHulk()).thenReturn(false);
        when(ship.isFighter()).thenReturn(false);
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        return ship;
    }

    private ShipAPI guard(PersonAPI commander) {
        ShipAPI guard = ship(0, mock(PersonAPI.class), 0f);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getFleetCommanderForStats()).thenReturn(commander);
        when(guard.getMutableStats().getFleetMember()).thenReturn(member);
        when(guard.getMutableStats().getHullDamageTakenMult()).thenReturn(hullDamageTaken);
        when(guard.getMutableStats().getArmorDamageTakenMult()).thenReturn(armorDamageTaken);
        when(guard.getMutableStats().getShieldDamageTakenMult()).thenReturn(shieldDamageTaken);
        return guard;
    }

    private static void guardFor(ShipAPI guard) {
        DefenseSkillEffect.DAMAGE_TAKEN_NEAR_FLAGSHIP_PERCENT.advanceInCombat(guard, MOD_ID, -10f);
    }

    private void assertDamageTaken(float expected) {
        assertEquals(expected, hullDamageTaken.getModifiedValue(), EPSILON);
        assertEquals(expected, armorDamageTaken.getModifiedValue(), EPSILON);
        assertEquals(expected, shieldDamageTaken.getModifiedValue(), EPSILON);
    }

    @Test
    void reducesDamageTakenOnlyWhileWithinRangeOfTheDeployedFlagship() {
        PersonAPI commander = mock(PersonAPI.class);
        ShipAPI guard = guard(commander);
        ShipAPI flagship = ship(0, commander, FlagshipGuard.RANGE - 100f);
        List<ShipAPI> ships = new ArrayList<>(List.of(guard, flagship, ship(1, commander, 50f)));
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getShips()).thenReturn(ships);

        try (MockedStatic<Global> global = mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            guardFor(guard);
            assertDamageTaken(0.9f);

            when(flagship.getLocation()).thenReturn(new Vector2f(FlagshipGuard.RANGE + 100f, 0f));
            guardFor(guard);
            assertDamageTaken(1f);

            when(flagship.getLocation()).thenReturn(new Vector2f(0f, 0f));
            ships.remove(flagship);
            guardFor(guard);
            assertDamageTaken(1f);
        }
    }

    @Test
    void theFlagshipItselfGainsNothing() {
        PersonAPI commander = mock(PersonAPI.class);
        ShipAPI flagship = guard(commander);
        when(flagship.getCaptain()).thenReturn(commander);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getShips()).thenReturn(List.of(flagship));

        try (MockedStatic<Global> global = mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            guardFor(flagship);
            assertDamageTaken(1f);
        }
    }
}
