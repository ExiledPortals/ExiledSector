package exiledsector.skills.npc;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import exiledsector.ModSettings;
import exiledsector.compat.CompatTarget;
import exiledsector.compat.EmergentThreatsCompat;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.HullForms;
import exiledsector.skills.InstalledHullMods;
import exiledsector.skills.ResearchStationNodes;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class NpcStationSubsystem {

    static final String ADAPTIVE_PREFIX = "vice_adaptive";
    static final String ABYSSAL_ENTROPY_PROJECTOR = "vice_adaptive_entropy_projector_abyssal";
    static final String ENTROPY_PROJECTOR = "vice_adaptive_entropy_projector";
    static final String METASTATIC_GROWTH = "vice_adaptive_metastatic_growth";
    static final String FLIGHT_COMMAND = "vice_adaptive_flight_command";
    static final String GRAVITY_DRIVE = "vice_adaptive_gravity_drive";
    static final String NEURAL_NET = "vice_adaptive_neural_net";
    static final String PHASE_COILS = "vice_adaptive_phase_coils";
    static final String REACTOR_CHAMBER = "vice_adaptive_reactor_chamber";
    static final String TRAJECTORY_ANALYZER = "vice_adaptive_trajectory_analyzer";
    static final String FLUX_DISSIPATOR = "vice_adaptive_flux_dissipator";
    static final String ENTROPY_ARRESTER = "vice_adaptive_entropy_arrester";
    static final String EMITTER_DIODES = "vice_adaptive_emitter_diodes";
    static final String PULSE_RESONATOR = "vice_adaptive_pulse_resonator";
    static final String TACTICAL_CORE = "vice_adaptive_tactical_core";
    static final String TEMPORAL_SHELL = "vice_adaptive_temporal_shell";
    static final String THRUSTER_CONTROL = "vice_adaptive_thruster_control";

    static final String VOLANTIAN_MAKER = "Volantian Remnant Conversion";
    private static final Set<String> SUBSYSTEM_MAKERS = Set.of("Remnant", "Remnant Mess Object", "Dustkeeper Proxies",
            "Dustkeeper Contingency", "Abyssal", "Seraph", VOLANTIAN_MAKER, "XIV Remnant");
    private static final Set<String> ABYSSAL_MAKERS = Set.of("Abyssal", "Seraph");
    private static final Set<String> IX_MAKERS = Set.of("IX Battlegroup", "IX Auxiliary", "Trinity Worlds");
    private static final Set<String> AI_INTERFACE_HULL_MODS = Set.of("ix_converted_hull", "ix_semi_automated", "tw_subsystem_interface",
            "vice_subsystem_interface");
    private static final Set<String> STATION_HULL_MODS = Set.of("supercomputer", "vastbulk");
    private static final Set<String> GUNSHIP_HULL_MODS = Set.of("vice_convert_shuttle_attack", "vice_convert_shuttle_bomber",
            "ix_carrier_gunships");
    private static final Set<String> PHASE_HULL_MODS = Set.of("phase_anchor", "adaptive_coils");
    private static final Set<String> OPTICS_HULL_MODS = Set.of("advancedoptics", "high_scatter_amp", "vice_attuned_emitter_diodes",
            "ix_laser_collimator");
    private static final Set<String> ENGINE_HULL_MODS = Set.of("auxiliarythrusters", "unstable_injector");
    private static final String COHERER = "coherer";
    private static final String LARGE_STATION_HULL_MOD = "vast_bulk";
    private static final String CONVERTED_HULL = "ix_converted_hull";
    private static final String ENHANCED_CONTROL_NODE = "tw_enhanced_control_node";
    private static final String CHARGE_TRANSFER_EMITTER = "cte_tw";
    private static final String DAWN_BEAM_TAG = "ix_dawn";
    private static final String ARCHAIC_MISSILE_TAG = "archaic_r";
    private static final String ARCHAIC_ONE_TAG = "archaic1";
    private static final String ARCHAIC_TWO_TAG = "archaic2";
    private static final int ARCHAIC_HEAVY_MISSILES = 5;
    private static final int DAWN_BEAM_WEIGHT = 2;
    private static final int FORCING_WEAPON_COUNT = 3;
    private static final int FORCING_MISSILE_COUNT = 4;
    static final String SYNTHESIS_FLEET_FLAG = "$xo_synthesis_fleet";

    private NpcStationSubsystem() {
    }

    public static void grant(ShipSkillData shipData, FleetMemberAPI member, MemoryAPI fleetMemory, Random random) {
        if (shipData == null || ResearchStationNodes.station() == null) {
            return;
        }
        boolean synthesisFleet = fleetMemory != null && fleetMemory.is(SYNTHESIS_FLEET_FLAG, true);
        String hullModId = hullModId(Loadout.of(member, synthesisFleet, treeHullModIds(shipData)), EtSettings.current(), random,
                id -> subsystemFor(id) != null);
        SkillNode subsystem = subsystemFor(hullModId);
        if (subsystem != null) {
            restore(shipData, subsystem.getId());
        }
    }

    private static Set<String> treeHullModIds(ShipSkillData shipData) {
        Set<String> hullModIds = new HashSet<>();
        for (AllocatedNode allocated : AllocatedNode.of(shipData)) {
            if (allocated.effectiveType() != null) {
                hullModIds.addAll(allocated.effectiveType().getPhantomHullModIds());
            }
        }
        return hullModIds;
    }

    public static void restore(ShipSkillData shipData, String subsystemNodeId) {
        SkillNode station = ResearchStationNodes.station();
        SkillNode subsystem = SkillTree.get(subsystemNodeId);
        if (station == null || !ResearchStationNodes.isSubsystem(subsystem)) {
            return;
        }
        if (!shipData.isAllocated(station.getId())) {
            ShipTrees.allocateGranted(shipData, station);
        }
        shipData.integrate(station.getId());
        ResearchStationNodes.select(shipData, subsystem);
    }

    static SkillNode subsystemFor(String hullModId) {
        if (hullModId == null) {
            return null;
        }
        for (SkillNode subsystem : ResearchStationNodes.subsystems()) {
            for (SkillType form : HullForms.withForms(subsystem.getType())) {
                if (form.getPhantomHullModIds().contains(hullModId)) {
                    return subsystem;
                }
            }
        }
        return null;
    }

    static String hullModId(Loadout loadout, EtSettings settings, Random random, Predicate<String> hasNode) {
        if (!settings.addsSubsystems() || !loadout.isEligible()) {
            return null;
        }
        if (loadout.synthesisFleet() && !settings.synthesisEnabled()) {
            return null;
        }
        if (loadout.fittedAdaptiveHullModId() != null) {
            if (VOLANTIAN_MAKER.equals(loadout.manufacturer()) && !settings.volantianEnabled()) {
                return null;
            }
            return loadout.adoptsFittedHullMod() ? loadout.fittedAdaptiveHullModId() : null;
        }
        return pick(loadout, random, hasNode);
    }

    static String pick(Loadout loadout, Random random, Predicate<String> hasNode) {
        Set<String> hullMods = loadout.hullModIds();
        if (ABYSSAL_MAKERS.contains(loadout.manufacturer())) {
            return ABYSSAL_ENTROPY_PROJECTOR;
        }
        if ("Remnant Mess Object".equals(loadout.manufacturer())) {
            return METASTATIC_GROWTH;
        }
        if (hasAny(hullMods, GUNSHIP_HULL_MODS)) {
            return FLIGHT_COMMAND;
        }
        if (hullMods.contains(CONVERTED_HULL) && hullMods.contains(ENHANCED_CONTROL_NODE)) {
            return GRAVITY_DRIVE;
        }
        if (hullMods.contains(LARGE_STATION_HULL_MOD)) {
            return NEURAL_NET;
        }
        if (loadout.shieldless() && !hasAny(hullMods, PHASE_HULL_MODS)) {
            return PHASE_COILS;
        }
        if (loadout.chargeTransferEmitter()) {
            return ENTROPY_PROJECTOR;
        }
        List<String> candidates = new ArrayList<>(List.of(ENTROPY_PROJECTOR, NEURAL_NET, PHASE_COILS, REACTOR_CHAMBER, TRAJECTORY_ANALYZER));
        if (IX_MAKERS.contains(loadout.manufacturer())) {
            candidates.add(FLUX_DISSIPATOR);
            candidates.add(ENTROPY_ARRESTER);
        }
        boolean hasOptics = hasAny(hullMods, OPTICS_HULL_MODS);
        if (loadout.beamCount() > FORCING_WEAPON_COUNT && !hasOptics) {
            return EMITTER_DIODES;
        }
        if (loadout.beamCount() >= 1 && !hasOptics) {
            candidates.add(EMITTER_DIODES);
        }
        boolean hasCoherer = hullMods.contains(COHERER);
        if (loadout.pulseCount() > FORCING_WEAPON_COUNT && !hasCoherer) {
            return PULSE_RESONATOR;
        }
        if (loadout.pulseCount() >= 1 && loadout.beamCount() <= FORCING_WEAPON_COUNT && !hasCoherer) {
            candidates.add(PULSE_RESONATOR);
        }
        if (loadout.missileCount() > FORCING_MISSILE_COUNT) {
            return TRAJECTORY_ANALYZER;
        }
        if (loadout.captainless()) {
            candidates.add(TACTICAL_CORE);
        }
        if (!loadout.shieldless()) {
            candidates.add(TEMPORAL_SHELL);
        }
        if (!hasAny(hullMods, ENGINE_HULL_MODS)) {
            candidates.add(THRUSTER_CONTROL);
        }
        candidates.removeIf(hasNode.negate());
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    private static boolean hasAny(Set<String> hullMods, Set<String> wanted) {
        for (String hullMod : wanted) {
            if (hullMods.contains(hullMod)) {
                return true;
            }
        }
        return false;
    }

    record Loadout(String manufacturer, Set<String> hullModIds, boolean fighter, boolean module, boolean shieldless, boolean synthesisFleet,
                   boolean captainless, int beamCount, int pulseCount, int missileCount, boolean chargeTransferEmitter,
                   String fittedAdaptiveHullModId, boolean adoptsFittedHullMod) {

        static Loadout of(FleetMemberAPI member, boolean synthesisFleet, Set<String> treeHullModIds) {
            ShipVariantAPI variant = member.getVariant();
            ShipHullSpecAPI hullSpec = variant.getHullSpec();
            Set<String> hullModIds = new HashSet<>(variant.getHullMods());
            String fittedAdaptive = null;
            for (String hullModId : hullModIds) {
                if (hullModId.startsWith(ADAPTIVE_PREFIX)) {
                    fittedAdaptive = hullModId;
                    break;
                }
            }
            boolean adoptsFitted = fittedAdaptive != null && (NpcHullMods.of(variant).removable().contains(fittedAdaptive)
                    || InstalledHullMods.isInstalledBySkillTree(variant, fittedAdaptive));
            hullModIds.addAll(treeHullModIds);
            ShieldType shieldType = hullSpec.getShieldType();
            int beams = 0;
            int pulses = 0;
            int missiles = 0;
            boolean chargeTransferEmitter = false;
            for (String slotId : variant.getNonBuiltInWeaponSlots()) {
                WeaponSpecAPI weapon = variant.getWeaponSpec(slotId);
                if (weapon == null) {
                    continue;
                }
                chargeTransferEmitter |= CHARGE_TRANSFER_EMITTER.equals(weapon.getWeaponId());
                if (weapon.getType() == WeaponType.ENERGY && !weapon.getAIHints().contains(AIHints.PD)) {
                    if (!weapon.isBeam()) {
                        pulses++;
                    } else {
                        beams += weapon.hasTag(DAWN_BEAM_TAG) ? -DAWN_BEAM_WEIGHT : 1;
                    }
                }
                missiles += missileWeight(weapon);
            }
            boolean captainless = member.getCaptain() == null || member.getCaptain().isDefault();
            boolean module = hullSpec.getSuppliesPerMonth() == 0 && hullSpec.getEngineSpec().getMaxSpeed() == 0;
            String manufacturer = hullSpec.getManufacturer() == null ? "" : hullSpec.getManufacturer();
            return new Loadout(manufacturer, hullModIds, variant.isFighter(), module,
                    shieldType == null || shieldType == ShieldType.NONE || shieldType == ShieldType.PHASE, synthesisFleet, captainless, beams,
                    pulses, missiles, chargeTransferEmitter, fittedAdaptive, adoptsFitted);
        }

        private static int missileWeight(WeaponSpecAPI weapon) {
            if (weapon.hasTag(ARCHAIC_MISSILE_TAG)) {
                if (weapon.hasTag(ARCHAIC_ONE_TAG)) {
                    return 1;
                }
                return weapon.hasTag(ARCHAIC_TWO_TAG) ? 2 : ARCHAIC_HEAVY_MISSILES;
            }
            return weapon.getType() == WeaponType.MISSILE ? 1 : 0;
        }

        boolean isEligible() {
            if (fighter || module || hasAny(hullModIds, STATION_HULL_MODS)) {
                return false;
            }
            return synthesisFleet || hasAny(hullModIds, AI_INTERFACE_HULL_MODS) || SUBSYSTEM_MAKERS.contains(manufacturer);
        }
    }

    record EtSettings(boolean addsSubsystems, boolean synthesisEnabled, boolean volantianEnabled) {

        static final EtSettings DEFAULTS = new EtSettings(true, true, true);
        private static final String CHALLENGING = "Challenging";

        static EtSettings current() {
            if (!CompatTarget.isModEnabled(EmergentThreatsCompat.MOD_ID)) {
                return DEFAULTS;
            }
            String modId = EmergentThreatsCompat.MOD_ID;
            boolean challenging = CHALLENGING.equals(ModSettings.otherModStringOr(modId, "vice_remnantDifficulty", CHALLENGING));
            boolean abyssalEnabled = ModSettings.otherModBooleanOr(modId, "vice_abyssalEnabled", true);
            boolean messEnabled = ModSettings.otherModBooleanOr(modId, "vice_messEnabled", true);
            return new EtSettings(challenging && abyssalEnabled && messEnabled, ModSettings.otherModBooleanOr(modId, "vice_synthesisEnabled", true),
                    ModSettings.otherModBooleanOr(modId, "vice_vriEnabled", true));
        }
    }
}
