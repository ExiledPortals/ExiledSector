package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.motes.MoteKind;
import exiledsector.motes.Motes;
import org.lwjgl.util.vector.Vector2f;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

final class CorrosionMoteListener extends ShipCombatListener implements DamageDealtModifier {

    static final String MOTE_DAMAGE_KEY = "exiledSector_corrosionMoteDamage";
    static final float CONTACT_SECONDS_PER_MOTE = 2f;
    static final float MOTE_FLIGHT_SECONDS = 8f;
    private static final float LAUNCH_SPREAD_DEGREES = 15f;

    private final Map<WeaponAPI, Float> contactSecondsByWeapon = new IdentityHashMap<>();
    private final Random random;

    CorrosionMoteListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    CorrosionMoteListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
    }

    float contactSeconds(WeaponAPI weapon) {
        return contactSecondsByWeapon.getOrDefault(weapon, 0f);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit || !(param instanceof BeamAPI beam) || !(target instanceof ShipAPI targetShip) || targetShip.isHulk() || targetShip.isFighter()
                || !CombatQueries.isHostile(ownerShip, targetShip) || !ownerIsAliveNotHulk() || beam.getWeapon() == null) {
            return null;
        }
        WeaponAPI weapon = beam.getWeapon();
        float contact = contactSecondsByWeapon.getOrDefault(weapon, 0f) + (damage.isDps() ? damage.getDpsDuration() : 0f);
        if (contact >= CONTACT_SECONDS_PER_MOTE) {
            contact -= CONTACT_SECONDS_PER_MOTE;
            launchMote(weapon, targetShip);
        }
        contactSecondsByWeapon.put(weapon, contact);
        return null;
    }

    private void launchMote(WeaponAPI weapon, ShipAPI targetShip) {
        float moteDamage = magnitude(MOTE_DAMAGE_KEY);
        if (moteDamage <= 0f) {
            return;
        }
        Vector2f from = weapon.getLocation();
        float facing = Motes.angleDegrees(from, targetShip.getLocation()) + (random.nextFloat() * 2f - 1f) * LAUNCH_SPREAD_DEGREES;
        Motes.launch(new Motes.Launch(MoteKind.STARDUST, ownerShip, from, facing, targetShip, moteDamage, moteDamage, MOTE_FLIGHT_SECONDS));
    }
}
