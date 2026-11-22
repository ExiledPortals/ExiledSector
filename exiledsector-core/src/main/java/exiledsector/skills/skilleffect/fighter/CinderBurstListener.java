package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

final class CinderBurstListener extends StrikecraftDeathBurst {

    static final String DAMAGE_KEY = "exiledSector_strikecraftDeathBurstDamage";
    static final float BURST_RADIUS = 150f;
    private static final Color BURST_COLOR = new Color(255, 120, 40, 140);
    private static final float BURST_VISUAL_SECONDS = 0.6f;

    CinderBurstListener(ShipAPI fighter, ShipAPI carrier) {
        super(fighter, carrier, DAMAGE_KEY, DamageType.FRAGMENTATION, BURST_RADIUS);
    }

    @Override
    void showBurst(CombatEngineAPI engine, ShipAPI fighter, Vector2f burstPoint) {
        engine.spawnExplosion(burstPoint, new Vector2f(fighter.getVelocity()), BURST_COLOR, BURST_RADIUS, BURST_VISUAL_SECONDS);
    }
}
