package exiledlib.skills.skilleffect.aura;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledlib.compat.CombatGlow;
import exiledlib.skills.skilleffect.render.Fade;
import exiledlib.skills.skilleffect.render.SpriteCache;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;

final class RepairDroneRenderer extends BaseCombatLayeredRenderingPlugin {

    static final int DRONES_PER_COORDINATOR = 2;
    static final String SPRITE_PATH = "graphics/fx/na_repair_drone.png";
    static final float DRONE_SIZE = 20f;
    static final float SPARK_INTERVAL_SECONDS = 0.4f;
    private static final float RENDER_RADIUS = 1_000_000f;
    private static final float DOCK_RADIUS_SHARE = 0.5f;
    private static final Color SPARK_COLOR = new Color(255, 200, 120);
    private static final float SPARK_SIZE = 10f;
    private static final float SPARK_SECONDS = 0.3f;
    private static final float SPARK_LIGHT_SIZE = 50f;
    private static final float SPARK_LIGHT_INTENSITY = 0.5f;
    static final float ORPHAN_FADE_SECONDS = 0.6f;

    private final RepairCoordinatorField field;
    private final Map<RepairCoordinatorListener, Swarm> swarms = new IdentityHashMap<>();
    private final SpriteCache spriteCache = new SpriteCache(RepairDroneRenderer.class);

    private static final class Swarm {
        private final RepairDrone[] drones = new RepairDrone[DRONES_PER_COORDINATOR];
        private float secondsSinceSpark;
        private float presence = 1f;

        Swarm() {
            for (int index = 0; index < drones.length; index++) {
                drones[index] = new RepairDrone((float) (Math.PI * 2 * index / drones.length));
            }
        }

        private boolean fadedAway(float amount) {
            presence = Fade.step(presence, false, amount / ORPHAN_FADE_SECONDS);
            return presence <= 0f;
        }
    }

    RepairDroneRenderer(RepairCoordinatorField field) {
        super(CombatEngineLayers.FIGHTERS_LAYER);
        this.field = field;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.FIGHTERS_LAYER);
    }

    @Override
    public float getRenderRadius() {
        return RENDER_RADIUS;
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || amount <= 0f) {
            return;
        }
        if (swarms.size() > field.sources().size()) {
            swarms.entrySet().removeIf(entry -> !field.sources().contains(entry.getKey()) && entry.getValue().fadedAway(amount));
        }
        for (RepairCoordinatorListener source : field.sources()) {
            ShipAPI ship = source.source();
            Vector2f workPoint = source.workPoint();
            Swarm swarm = swarms.computeIfAbsent(source, ignored -> new Swarm());
            boolean anyWorking = false;
            for (RepairDrone drone : swarm.drones) {
                drone.advance(amount, ship.getLocation(), ship.getCollisionRadius() * DOCK_RADIUS_SHARE, workPoint);
                anyWorking |= drone.isWorking();
            }
            swarm.secondsSinceSpark += amount;
            if (anyWorking && workPoint != null && swarm.secondsSinceSpark >= SPARK_INTERVAL_SECONDS) {
                swarm.secondsSinceSpark = 0f;
                engine.addHitParticle(new Vector2f(workPoint), new Vector2f(), SPARK_SIZE, 1f, SPARK_SECONDS, SPARK_COLOR);
                CombatGlow.flash(workPoint, null, SPARK_COLOR, SPARK_LIGHT_SIZE, SPARK_LIGHT_INTENSITY, SPARK_SECONDS);
            }
        }
    }

    int swarmCount() {
        return swarms.size();
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (swarms.isEmpty()) {
            return;
        }
        SpriteAPI sprite = spriteCache.sprite(SPRITE_PATH);
        if (sprite == null) {
            return;
        }
        sprite.setSize(DRONE_SIZE, DRONE_SIZE);
        for (Swarm swarm : swarms.values()) {
            sprite.setAlphaMult(viewport.getAlphaMult() * swarm.presence);
            for (RepairDrone drone : swarm.drones) {
                Vector2f position = drone.position();
                if (drone.isDocked() || !viewport.isNearViewport(position, DRONE_SIZE)) {
                    continue;
                }
                sprite.setAngle(drone.facingDeg() - 90f);
                sprite.renderAtCenter(position.x, position.y);
            }
        }
    }
}
