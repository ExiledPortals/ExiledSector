package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

final class MissileEjectionListener extends ShipCombatListener implements DamageTakenModifier {

    static final String CHANCE_PERCENT_KEY = "exiledSector_missileEjectionChancePercent";
    static final String EJECTION_MOD_ID = "exiledSector_missileEjection";
    private static final Color HORIZON_COLOR = new Color(120, 80, 255, 255);
    private static final float HORIZON_SIZE_PER_RADIUS = 3f;
    private static final float HORIZON_END_SIZE_MULT = 0.2f;
    private static final float HORIZON_RAMP_UP_SHARE = 0.1f;
    private static final float HORIZON_FULL_BRIGHTNESS_SHARE = 0.3f;
    private static final float HORIZON_SECONDS = 0.6f;

    private final Random random;

    MissileEjectionListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    MissileEjectionListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit || target != ownerShip || !(param instanceof MissileAPI missile) || missile.didDamage()) {
            return null;
        }
        float chancePercent = magnitude(CHANCE_PERCENT_KEY);
        if (chancePercent <= 0f || random.nextFloat() * 100f >= chancePercent) {
            return null;
        }
        missile.setDidDamage(true);
        damage.getModifier().modifyMult(EJECTION_MOD_ID, 0f);
        showEjection(missile);
        return EJECTION_MOD_ID;
    }

    private static void showEjection(MissileAPI missile) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || missile.getLocation() == null) {
            return;
        }
        engine.addNegativeSwirlyNebulaParticle(new Vector2f(missile.getLocation()), new Vector2f(),
                Math.max(1f, missile.getCollisionRadius()) * HORIZON_SIZE_PER_RADIUS, HORIZON_END_SIZE_MULT, HORIZON_RAMP_UP_SHARE,
                HORIZON_FULL_BRIGHTNESS_SHARE, HORIZON_SECONDS, HORIZON_COLOR);
    }
}
