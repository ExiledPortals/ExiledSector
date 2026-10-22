package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.util.Misc;
import exiledlib.motes.MoteKind;
import exiledlib.motes.Motes;
import org.lwjgl.util.vector.Vector2f;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class CorrosionMoteListener extends ShipCombatListener implements DamageDealtModifier {

    static final String MOTE_DAMAGE_KEY = "exiledSector_corrosionMoteDamage";
    static final float CONTACT_SECONDS_PER_MOTE = 2f;
    static final float MOTE_FLIGHT_SECONDS = 8f;
    private static final float LAUNCH_SPREAD_DEGREES = 15f;

    private final Map<WeaponAPI, BarrelContact> contactByWeapon = new IdentityHashMap<>();
    private final Random random;

    CorrosionMoteListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    CorrosionMoteListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
    }

    private static final class BarrelContact {
        private BeamAPI[] beams = new BeamAPI[0];
        private float[] seconds = new float[0];

        private int barrelOf(WeaponAPI weapon, BeamAPI beam) {
            for (int barrel = 0; barrel < beams.length; barrel++) {
                if (beams[barrel] == beam) {
                    return barrel;
                }
            }
            List<BeamAPI> weaponBeams = weapon.getBeams();
            int barrel = weaponBeams == null ? 0 : Math.max(0, weaponBeams.indexOf(beam));
            if (barrel >= beams.length) {
                beams = Arrays.copyOf(beams, barrel + 1);
                seconds = Arrays.copyOf(seconds, barrel + 1);
            }
            beams[barrel] = beam;
            return barrel;
        }
    }

    float contactSeconds(WeaponAPI weapon, int barrel) {
        BarrelContact contact = contactByWeapon.get(weapon);
        return contact == null || barrel >= contact.seconds.length ? 0f : contact.seconds[barrel];
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; motes are launched as a side effect instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit || !(param instanceof BeamAPI beam) || !(target instanceof ShipAPI targetShip) || targetShip.isHulk() || targetShip.isFighter()
                || !CombatQueries.isHostile(ownerShip, targetShip) || !ownerIsAliveNotHulk() || beam.getWeapon() == null) {
            return null;
        }
        WeaponAPI weapon = beam.getWeapon();
        BarrelContact barrelContact = contactByWeapon.computeIfAbsent(weapon, ignored -> new BarrelContact());
        int barrel = barrelContact.barrelOf(weapon, beam);
        float contact = barrelContact.seconds[barrel] + (damage.isDps() ? damage.getDpsDuration() : 0f);
        if (contact >= CONTACT_SECONDS_PER_MOTE) {
            contact -= CONTACT_SECONDS_PER_MOTE;
            launchMote(weapon, targetShip);
        }
        barrelContact.seconds[barrel] = contact;
        return null;
    }

    private void launchMote(WeaponAPI weapon, ShipAPI targetShip) {
        float moteDamage = magnitude(MOTE_DAMAGE_KEY);
        if (moteDamage <= 0f) {
            return;
        }
        Vector2f from = weapon.getLocation();
        float facing = Misc.getAngleInDegreesStrict(from, targetShip.getLocation()) + (random.nextFloat() * 2f - 1f) * LAUNCH_SPREAD_DEGREES;
        Motes.launch(new Motes.Launch(MoteKind.STARDUST, ownerShip, from, facing, targetShip, moteDamage, moteDamage, MOTE_FLIGHT_SECONDS));
    }
}
