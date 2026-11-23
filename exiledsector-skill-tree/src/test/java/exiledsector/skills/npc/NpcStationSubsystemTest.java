package exiledsector.skills.npc;

import exiledsector.skills.npc.NpcStationSubsystem.EtSettings;
import exiledsector.skills.npc.NpcStationSubsystem.Loadout;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import static exiledsector.skills.npc.NpcStationSubsystem.ABYSSAL_ENTROPY_PROJECTOR;
import static exiledsector.skills.npc.NpcStationSubsystem.EMITTER_DIODES;
import static exiledsector.skills.npc.NpcStationSubsystem.ENTROPY_PROJECTOR;
import static exiledsector.skills.npc.NpcStationSubsystem.FLUX_DISSIPATOR;
import static exiledsector.skills.npc.NpcStationSubsystem.METASTATIC_GROWTH;
import static exiledsector.skills.npc.NpcStationSubsystem.PHASE_COILS;
import static exiledsector.skills.npc.NpcStationSubsystem.PULSE_RESONATOR;
import static exiledsector.skills.npc.NpcStationSubsystem.TACTICAL_CORE;
import static exiledsector.skills.npc.NpcStationSubsystem.TEMPORAL_SHELL;
import static exiledsector.skills.npc.NpcStationSubsystem.THRUSTER_CONTROL;
import static exiledsector.skills.npc.NpcStationSubsystem.TRAJECTORY_ANALYZER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcStationSubsystemTest {

    private static final Predicate<String> EVERY_NODE = id -> true;
    private static final Predicate<String> NO_ENTROPY_PROJECTOR = id -> !ENTROPY_PROJECTOR.equals(id);
    private static final int ROLLS = 400;

    private static Loadout remnant(Set<String> hullMods, boolean shieldless, boolean captainless, int beams, int pulses, int missiles) {
        return new Loadout("Remnant", hullMods, false, false, shieldless, false, captainless, beams, pulses, missiles, false, null, false);
    }

    private static Loadout shielded(int beams, int pulses, int missiles) {
        return remnant(Set.of(), false, false, beams, pulses, missiles);
    }

    private static Loadout fitted(String hullModId, boolean removable) {
        return new Loadout("Remnant", Set.of(hullModId), false, false, false, false, false, 0, 0, 0, false, hullModId, removable);
    }

    private static Set<String> rolls(Loadout loadout, Predicate<String> hasNode) {
        Random random = new Random(7);
        Set<String> picks = new HashSet<>();
        for (int roll = 0; roll < ROLLS; roll++) {
            picks.add(NpcStationSubsystem.pick(loadout, random, hasNode));
        }
        return picks;
    }

    @Test
    void factionSpecificHullsGetTheirOwnSubsystem() {
        Loadout abyssal = new Loadout("Abyssal", Set.of(), false, false, false, false, false, 0, 0, 0, false, null, false);
        Loadout mess = new Loadout("Remnant Mess Object", Set.of(), false, false, false, false, false, 0, 0, 0, false, null, false);

        assertEquals(ABYSSAL_ENTROPY_PROJECTOR, NpcStationSubsystem.pick(abyssal, new Random(1), EVERY_NODE));
        assertEquals(METASTATIC_GROWTH, NpcStationSubsystem.pick(mess, new Random(1), EVERY_NODE));
    }

    @Test
    void shieldlessHullsGetPhaseCoilsUnlessTheyAlreadyHaveAPhaseMod() {
        assertEquals(PHASE_COILS, NpcStationSubsystem.pick(remnant(Set.of(), true, false, 0, 0, 0), new Random(1), EVERY_NODE));
        assertFalse(rolls(remnant(Set.of("phase_anchor"), true, false, 0, 0, 0), EVERY_NODE).contains(TEMPORAL_SHELL));
    }

    @Test
    void heavyLoadoutsForceTheirWeaponSubsystem() {
        assertEquals(EMITTER_DIODES, NpcStationSubsystem.pick(shielded(4, 0, 0), new Random(1), EVERY_NODE));
        assertEquals(PULSE_RESONATOR, NpcStationSubsystem.pick(shielded(0, 4, 0), new Random(1), EVERY_NODE));
        assertEquals(TRAJECTORY_ANALYZER, NpcStationSubsystem.pick(shielded(0, 0, 5), new Random(1), EVERY_NODE));
    }

    @Test
    void opticsStopBeamsFromForcingEmitterDiodes() {
        Loadout withOptics = remnant(Set.of("advancedoptics"), false, false, 4, 0, 0);

        assertFalse(rolls(withOptics, EVERY_NODE).contains(EMITTER_DIODES));
    }

    @Test
    void otherwiseTheShipRollsFromItsLoadoutCandidates() {
        Set<String> plain = rolls(shielded(0, 0, 0), EVERY_NODE);
        Set<String> captainlessWithBeams = rolls(remnant(Set.of("auxiliarythrusters"), false, true, 2, 1, 0), EVERY_NODE);

        assertTrue(plain.containsAll(List.of(TEMPORAL_SHELL, THRUSTER_CONTROL)));
        assertFalse(plain.contains(TACTICAL_CORE));
        assertFalse(plain.contains(FLUX_DISSIPATOR));
        assertTrue(captainlessWithBeams.containsAll(List.of(TACTICAL_CORE, EMITTER_DIODES, PULSE_RESONATOR)));
        assertFalse(captainlessWithBeams.contains(THRUSTER_CONTROL));
    }

    @Test
    void subsystemsWithoutANodeAreLeftOutOfTheRoll() {
        assertFalse(rolls(shielded(0, 0, 0), NO_ENTROPY_PROJECTOR).contains(ENTROPY_PROJECTOR));
        assertTrue(rolls(shielded(0, 0, 0), EVERY_NODE).contains(ENTROPY_PROJECTOR));
    }

    @Test
    void onlyRemnantStyleHullsAreEligible() {
        Loadout hegemony = new Loadout("Low Tech", Set.of(), false, false, false, false, false, 0, 0, 0, false, null, false);
        Loadout converted = new Loadout("Low Tech", Set.of("ix_converted_hull"), false, false, false, false, false, 0, 0, 0, false, null, false);
        Loadout station = remnant(Set.of("vastbulk"), false, false, 0, 0, 0);
        Loadout module = new Loadout("Remnant", Set.of(), false, true, false, false, false, 0, 0, 0, false, null, false);

        assertNull(NpcStationSubsystem.hullModId(hegemony, EtSettings.DEFAULTS, new Random(1), EVERY_NODE));
        assertTrue(converted.isEligible());
        assertFalse(station.isEligible());
        assertFalse(module.isEligible());
    }

    @Test
    void aSubsystemAlreadyFittedIsAdoptedOnlyWhenItCanBeReplaced() {
        assertEquals(TEMPORAL_SHELL, NpcStationSubsystem.hullModId(fitted(TEMPORAL_SHELL, true), EtSettings.DEFAULTS, new Random(1), EVERY_NODE));
        assertNull(NpcStationSubsystem.hullModId(fitted(TEMPORAL_SHELL, false), EtSettings.DEFAULTS, new Random(1), EVERY_NODE));
    }

    @Test
    void emergentThreatsSettingsThatAddNoSubsystemsAreHonoured() {
        EtSettings easy = new EtSettings(false, true, true);
        EtSettings noSynthesis = new EtSettings(true, false, true);
        Loadout synthesisShip = new Loadout("Low Tech", Set.of(), false, false, false, true, false, 0, 0, 0, false, null, false);

        assertNull(NpcStationSubsystem.hullModId(shielded(4, 0, 0), easy, new Random(1), EVERY_NODE));
        assertEquals(EMITTER_DIODES, NpcStationSubsystem.hullModId(remnant(Set.of(), false, false, 4, 0, 0), EtSettings.DEFAULTS,
                new Random(1), EVERY_NODE));
        assertNull(NpcStationSubsystem.hullModId(synthesisShip, noSynthesis, new Random(1), EVERY_NODE));
    }
}
