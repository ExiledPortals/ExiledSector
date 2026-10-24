package exiledlib.skills.skilleffect.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledlib.compat.CombatGlow;
import exiledlib.skills.skilleffect.CombatQueries;
import exiledlib.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

final class DeathExplosionListener extends ShipCombatListener implements LethalHitArbiter.Reactor {

    static final String FUEL_DAMAGE_PERCENT_KEY = "exiledLib_explodeOnDeathFuelDamagePercent";

    private static final float RADIUS_MULT = 4f;
    private static final float MIN_RADIUS = 300f;
    private static final Color BLAST_COLOR = new Color(255, 200, 120, 40);
    private static final Color CORE_COLOR = new Color(255, 255, 255, 60);
    private static final CombatGlow.Burst BLAST_GLOW = new CombatGlow.Burst(new Color(255, 200, 120), 1.5f, 1.5f, 1.5f, 1.2f, 0.15f, 1.2f,
            false);

    private boolean exploded;

    DeathExplosionListener(ShipAPI ownerShip) {
        super(ownerShip);
        LethalHitArbiter.addReactor(ownerShip, this);
    }

    // Intended: detonates once on the first lethal blow even if a Phase Anchor dive or similar then saves the ship.
    @Override
    public void reactToLethalHit(ShipAPI ship, float damageAmount) {
        if (!exploded) {
            exploded = true;
            detonate();
        }
    }

    private void detonate() {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f loc = ownerShip.getLocation();
        float radius = Math.max(MIN_RADIUS, ownerShip.getCollisionRadius() * RADIUS_MULT);
        float damage = fuelDamage();

        // Intended: the blast hits every nearby ship, friendlies included.
        List<ShipAPI> nearby = CombatQueries.shipsMatching(other -> other != ownerShip && !other.isHulk() && !other.isShuttlePod()
                && CombatQueries.withinRadius(other.getLocation(), loc, radius));
        for (ShipAPI other : nearby) {
            float distance = Vector2f.sub(other.getLocation(), loc, null).length();
            float dealt = damage * (radius - distance) / radius;
            if (dealt <= 0f) {
                continue;
            }
            engine.applyDamage(other, other.getLocation(), dealt, DamageType.HIGH_EXPLOSIVE, 0f, true, false, ownerShip);
        }

        engine.spawnExplosion(loc, new Vector2f(), BLAST_COLOR, radius, 1.2f);
        engine.spawnExplosion(loc, new Vector2f(), CORE_COLOR, radius * 0.5f, 0.6f);
        BLAST_GLOW.play(loc, null, new Vector2f(), radius);
    }

    private float fuelDamage() {
        FleetMemberAPI member = ownerShip.getMutableStats().getFleetMember();
        if (member == null) {
            return 0f;
        }
        return member.getFuelCapacity() * magnitude(FUEL_DAMAGE_PERCENT_KEY) / 100f;
    }
}
