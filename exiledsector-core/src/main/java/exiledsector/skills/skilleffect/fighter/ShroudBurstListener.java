package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.ShipExplosionFlareVisual;
import com.fs.starfarer.api.impl.combat.ShipExplosionFlareVisual.ShipExplosionFlareParams;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

final class ShroudBurstListener extends StrikecraftDeathBurst {

    static final String DAMAGE_KEY = "exiledSector_strikecraftDeathShroudBurstDamage";
    static final float BURST_RADIUS = 150f;
    private static final int SWIRL_COUNT = 11;
    private static final float SWIRL_START_SIZE = 15f;
    private static final float SWIRL_GROWTH = 1.25f;
    private static final float SWIRL_SCATTER = 0.5f;
    private static final float SWIRL_SIZE_MULT = 4f;
    private static final float SWIRL_ENDURANCE = 3f;
    private static final float SWIRL_SECONDS = 0.5f;
    private static final Color FLASH_COLOR = new Color(255, 50, 100, 255);
    private static final float FLASH_SIZE_PER_ROOT_RADIUS = 30f;
    private static final float FLASH_SECONDS = 1.5f;
    private static final float FLARE_SIZE_PER_ROOT_RADIUS = 60f * 475f / 994f;
    private static final float FLARE_WIDTH_MULT = 4f;
    private static final float FLARE_HEIGHT_MULT = 1.6f;
    private static final float FLARE_FADE_IN = 0.1f;
    private static final float FLARE_FADE_OUT = 2f;
    private static final String SOUND_ID = "abyssal_glare_explosion";
    private static final float SOUND_VOLUME = 0.5f;

    ShroudBurstListener(ShipAPI fighter, ShipAPI carrier) {
        super(fighter, carrier, DAMAGE_KEY, DamageType.ENERGY, BURST_RADIUS);
    }

    @Override
    void showBurst(CombatEngineAPI engine, ShipAPI fighter, Vector2f burstPoint) {
        Vector2f velocity = new Vector2f(fighter.getVelocity());
        addSwirls(engine, burstPoint, velocity, false);
        addSwirls(engine, burstPoint, velocity, true);
        addFlash(engine, burstPoint, velocity, (float) Math.sqrt(fighter.getCollisionRadius()));
        Global.getSoundPlayer().playSound(SOUND_ID, 1f, SOUND_VOLUME, burstPoint, velocity);
    }

    private static void addSwirls(CombatEngineAPI engine, Vector2f burstPoint, Vector2f velocity, boolean dark) {
        float swirlSize = SWIRL_START_SIZE;
        for (int i = 0; i < SWIRL_COUNT; i++) {
            Vector2f swirlPoint = Misc.getPointWithinRadius(burstPoint, swirlSize * SWIRL_SCATTER);
            float particleSize = swirlSize * SWIRL_SIZE_MULT * (0.5f + (float) Math.random() * 0.5f);
            float seconds = SWIRL_SECONDS + (float) Math.random() * SWIRL_SECONDS;
            if (dark) {
                engine.addNegativeSwirlyNebulaParticle(swirlPoint, velocity, particleSize, SWIRL_ENDURANCE, 0f, 0f, seconds,
                        DwellerShroud.SHROUD_COLOR);
            } else {
                engine.addSwirlyNebulaParticle(swirlPoint, velocity, particleSize, SWIRL_ENDURANCE, 0f, 0f, seconds,
                        DwellerShroud.SHROUD_COLOR, false);
            }
            swirlSize *= SWIRL_GROWTH;
        }
    }

    private static void addFlash(CombatEngineAPI engine, Vector2f burstPoint, Vector2f velocity, float rootRadius) {
        ShipExplosionFlareParams flare = new ShipExplosionFlareParams();
        float flareSize = rootRadius * FLARE_SIZE_PER_ROOT_RADIUS;
        flare.flareWidth = flareSize * FLARE_WIDTH_MULT;
        flare.flareHeight = flareSize * FLARE_HEIGHT_MULT;
        flare.color = FLASH_COLOR;
        flare.fadeIn = FLARE_FADE_IN;
        flare.fadeOut = FLARE_FADE_OUT;
        CombatEntityAPI flareEntity = engine.addLayeredRenderingPlugin(new ShipExplosionFlareVisual(flare));
        flareEntity.getLocation().set(burstPoint);
        flareEntity.getVelocity().set(velocity);
        float flashSize = rootRadius * FLASH_SIZE_PER_ROOT_RADIUS;
        engine.addHitParticle(burstPoint, velocity, flashSize, 1f, FLASH_SECONDS, FLASH_COLOR);
        engine.addHitParticle(burstPoint, velocity, flashSize * 0.5f, 1f, FLASH_SECONDS, FLASH_COLOR);
    }
}
