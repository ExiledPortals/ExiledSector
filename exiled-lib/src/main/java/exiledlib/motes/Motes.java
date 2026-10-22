package exiledlib.motes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Motes {

    public static final int MAX_LIVE_MOTES_PER_SOURCE = 20;
    static final String LAUNCH_SOUND = "mote_attractor_launch_mote";
    private static final float LAUNCH_SOUND_VOLUME = 0.25f;
    private static final int EMP_IMMUNE_RESISTANCE = 10000;
    private static final float FLARE_PROOF_ECCM_CHANCE = 1f;
    private static final String CUSTOM_DATA_KEY = "exiledSector_liveMotesBySource";
    private static final Random SPIN_DIRECTION = new Random();

    private Motes() {
    }

    public record Launch(MoteKind kind, ShipAPI source, Vector2f from, float facing, CombatEntityAPI target,
                         float damage, float emp, float flightSeconds) {
    }

    public static MissileAPI launch(Launch launch) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || launch.source() == null || !launch.source().isAlive()) {
            return null;
        }
        List<MissileAPI> liveMotes = liveMotesOf(engine, launch.source());
        liveMotes.removeIf(mote -> !engine.isMissileAlive(mote));
        if (liveMotes.size() >= MAX_LIVE_MOTES_PER_SOURCE) {
            return null;
        }
        String weaponId = launch.kind().launcherWeaponId();
        if (!(engine.spawnProjectile(launch.source(), null, weaponId, new Vector2f(launch.from()), launch.facing(),
                launch.source().getVelocity()) instanceof MissileAPI mote)) {
            return null;
        }
        mote.setWeaponSpec(weaponId);
        mote.setMissileAI(new MoteHomingAI(mote, launch.target(), SPIN_DIRECTION.nextBoolean()));
        mote.getActiveLayers().remove(CombatEngineLayers.FF_INDICATORS_LAYER);
        mote.setEmpResistance(EMP_IMMUNE_RESISTANCE);
        mote.setEccmChanceOverride(FLARE_PROOF_ECCM_CHANCE);
        mote.setMaxFlightTime(launch.flightSeconds());
        mote.getDamage().setDamage(launch.damage());
        mote.getDamage().setFluxComponent(launch.emp());
        engine.applyDamageModifiersToSpawnedProjectileWithNullWeapon(launch.source(), WeaponAPI.WeaponType.ENERGY, false, mote.getDamage());
        liveMotes.add(mote);
        if (Global.getSoundPlayer() != null) {
            Global.getSoundPlayer().playSound(LAUNCH_SOUND, 1f, LAUNCH_SOUND_VOLUME, mote.getLocation(), mote.getVelocity());
        }
        return mote;
    }

    @SuppressWarnings("unchecked") // one map type is stored under our key in the engine's untyped custom data
    private static List<MissileAPI> liveMotesOf(CombatEngineAPI engine, ShipAPI source) {
        Map<ShipAPI, List<MissileAPI>> bySource = (Map<ShipAPI, List<MissileAPI>>) engine.getCustomData()
                .computeIfAbsent(CUSTOM_DATA_KEY, ignored -> new IdentityHashMap<ShipAPI, List<MissileAPI>>());
        return bySource.computeIfAbsent(source, ignored -> new ArrayList<>());
    }
}
