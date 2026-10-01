package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.skills.tags.WeaponKind;

import java.util.EnumSet;
import java.util.List;

import static exiledsector.skills.tags.WeaponKind.BALLISTIC;
import static exiledsector.skills.tags.WeaponKind.BEAM;
import static exiledsector.skills.tags.WeaponKind.ENERGY;
import static exiledsector.skills.tags.WeaponKind.MISSILE;
import static exiledsector.skills.tags.WeaponKind.NON_BEAM_ENERGY;
import static exiledsector.skills.tags.WeaponKind.OFFENSIVE_BEAM;

public final class NpcArchetypes {

    public record Archetype(String name, ShipProfile profile) {
    }

    public static final List<Archetype> ALL = List.of(
            archetype("frigate_energy_shielded", HullSize.FRIGATE, ShieldType.FRONT, 0, ENERGY, NON_BEAM_ENERGY, MISSILE),
            archetype("frigate_phase", HullSize.FRIGATE, ShieldType.PHASE, 0, ENERGY, NON_BEAM_ENERGY),
            archetype("frigate_phase_ballistic", HullSize.FRIGATE, ShieldType.PHASE, 0, BALLISTIC, MISSILE),
            archetype("destroyer_lowtech", HullSize.DESTROYER, ShieldType.FRONT, 0, BALLISTIC, MISSILE),
            archetype("destroyer_shieldless", HullSize.DESTROYER, ShieldType.NONE, 0, BALLISTIC, MISSILE),
            archetype("destroyer_carrier", HullSize.DESTROYER, ShieldType.FRONT, 2, BALLISTIC, MISSILE),
            archetype("cruiser_midline", HullSize.CRUISER, ShieldType.FRONT, 0, BALLISTIC, ENERGY, NON_BEAM_ENERGY, MISSILE),
            archetype("cruiser_hightech_beam", HullSize.CRUISER, ShieldType.OMNI, 0, ENERGY, BEAM, OFFENSIVE_BEAM, NON_BEAM_ENERGY,
                    MISSILE),
            archetype("cruiser_phase", HullSize.CRUISER, ShieldType.PHASE, 0, ENERGY, NON_BEAM_ENERGY, MISSILE),
            archetype("capital_battleship", HullSize.CAPITAL_SHIP, ShieldType.FRONT, 0, BALLISTIC, ENERGY, BEAM, MISSILE),
            archetype("capital_carrier", HullSize.CAPITAL_SHIP, ShieldType.FRONT, 4, BALLISTIC, ENERGY, NON_BEAM_ENERGY, MISSILE));

    private NpcArchetypes() {
    }

    private static Archetype archetype(String name, HullSize hullSize, ShieldType shieldType, int fighterBays,
                                       WeaponKind first, WeaponKind... rest) {
        return new Archetype(name, new ShipProfile(hullSize, shieldType, fighterBays, EnumSet.of(first, rest), false, 0f, shieldType == ShieldType.PHASE));
    }
}
