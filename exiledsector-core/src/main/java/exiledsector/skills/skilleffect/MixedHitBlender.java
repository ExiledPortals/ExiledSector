package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.input.InputEventAPI;
import exiledsector.persistence.CombatData;
import exiledsector.skills.skilleffect.DamageTypeMults.Mults;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MixedHitBlender implements DamageDealtModifier, DamageTakenModifier {

    static final DamageType CARRIER = DamageType.OTHER;
    static final String CUSTOM_DATA_KEY = "exiledSector_mixedHitBlender";
    static final String FRAME_RESET_KEY = "exiledSector_mixedHitFrameReset";
    static final String TYPE_SCALE_MOD_ID = "exiledSector_damageTypeScale";

    private record OpenHit(DamageAPI damage, CombatEntityAPI target, DamageType ownType, TypeShares shares, Mults previousBlend, float frameTime,
                           int sideDamageDepth) {

        private boolean blended() {
            return shares != null;
        }
    }

    private static final class HitKey {

        private DamageAPI damage;
        private CombatEntityAPI target;

        private HitKey(DamageAPI damage, CombatEntityAPI target) {
            this.damage = damage;
            this.target = target;
        }

        private HitKey pointAt(DamageAPI hitDamage, CombatEntityAPI hitTarget) {
            damage = hitDamage;
            target = hitTarget;
            return this;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof HitKey key && key.damage == damage && key.target == target;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(damage) + System.identityHashCode(target);
        }
    }

    private final Deque<OpenHit> openHits = new ArrayDeque<>();
    private final Map<HitKey, HitMix.Hit> hitsThisFrame = new HashMap<>();
    private final HitKey lookupKey = new HitKey(null, null);
    private float hitsFrameTime = Float.NaN;
    private HitDamageRestorer restorer;

    static MixedHitBlender existing() {
        CombatEngineAPI engine = Global.getCombatEngine();
        Map<String, Object> customData = engine == null ? null : engine.getCustomData();
        return customData != null && customData.get(CUSTOM_DATA_KEY) instanceof MixedHitBlender blender ? blender : null;
    }

    static MixedHitBlender ensureInCombat() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.getListenerManager() == null || engine.getCustomData() == null) {
            return null;
        }
        return CombatData.getOrCreate(engine, CUSTOM_DATA_KEY, MixedHitBlender.class, () -> startIn(engine));
    }

    private static MixedHitBlender startIn(CombatEngineAPI engine) {
        DamageTypeMults.restoreDefaults();
        MixedHitBlender blender = new MixedHitBlender();
        engine.getListenerManager().addListener(blender);
        blender.restorer = SkillEffectSupport.ensureEngineListener(HitDamageRestorer.class, HitDamageRestorer::new);
        blender.restorer.attach(blender);
        CombatData.getOrCreatePlugin(engine, FRAME_RESET_KEY, FrameReset.class, FrameReset::new);
        return blender;
    }

    HitMix.Hit recordedHit(DamageAPI damage, CombatEntityAPI target) {
        Map<HitKey, HitMix.Hit> hits = hitsThisFrame();
        if (hits.isEmpty()) {
            return null;
        }
        HitMix.Hit hit = hits.get(lookupKey.pointAt(damage, target));
        lookupKey.pointAt(null, null);
        return hit;
    }

    void record(HitMix.Hit hit) {
        hitsThisFrame().put(new HitKey(hit.damage(), hit.target()), hit);
    }

    TypeShares openSharesOf(DamageAPI damage) {
        for (OpenHit open : openHits) {
            if (open.damage() == damage && open.blended()) {
                return open.shares();
            }
        }
        return null;
    }

    TypeShares inheritedShares(DamageAPI damage) {
        if (openHits.isEmpty() || damage == null || damage.getType() != CARRIER) {
            return null;
        }
        for (OpenHit open : openHits) {
            if (open.blended()) {
                return open.sideDamageDepth() == HitDamageRestorer.sideDamageDepth() ? open.shares() : null;
            }
        }
        return null;
    }

    boolean isOpen(DamageAPI damage) {
        return !openHits.isEmpty() && openSharesOf(damage) != null;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        dropStaleHits(now());
        if (damage == null) {
            return null;
        }
        if (restorer != null) {
            restorer.hitStarted(damage, target);
        }
        ShipAPI attacker = CombatQueries.shipBehind(param);
        HitMix.Hit hit = attacker == null ? null : HitMix.resolve(attacker, param, target, damage, point, shieldHit);
        return hit == null ? null : scaleToCurrentPool(hit, damage, target);
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        float now = now();
        dropStaleHits(now);
        if (damage == null) {
            return null;
        }
        HitMix.Hit hit = hitFor(param, target, damage, point, shieldHit);
        TypeShares shares = hit == null ? null : hit.shares();
        boolean shipTarget = isShipTarget(target);
        if (shares == null || !shares.mixed()) {
            if (shipTarget && !openHits.isEmpty()) {
                openHits.push(new OpenHit(damage, target, damage.getType(), null, null, now, HitDamageRestorer.sideDamageDepth()));
            }
            return hit == null ? null : scaleToCurrentPool(hit, damage, target);
        }
        float genericPool = hit.genericPool();
        if (!shipTarget) {
            DamageType largest = shares.largest();
            if (damage.getType() != largest && !(param instanceof BeamAPI)) {
                HitDamageRestorer.retypeForThisHit(damage, largest, target);
            }
            return scaleModifier(damage, shares.weighted(type -> hit.damageScale(type, genericPool)));
        }
        scaleEmp(hit, damage, target, genericPool);
        open(damage, target, shares, HitBlend.of(shares, ((ShipAPI) target).getMutableStats(), type -> hit.damageScale(type, genericPool)), now);
        return null;
    }

    private static String scaleToCurrentPool(HitMix.Hit hit, DamageAPI damage, CombatEntityAPI target) {
        float genericPool = hit.genericPool();
        boolean mixed = hit.shares().mixed();
        if (!isShipTarget(target)) {
            return mixed ? null : scaleModifier(damage, hit.damageScale(hit.shares().largest(), genericPool));
        }
        scaleEmp(hit, damage, target, genericPool);
        if (!mixed) {
            scaleBase(damage, hit.damageScaleStillToApply(genericPool), target);
        }
        return null;
    }

    private static boolean isShipTarget(CombatEntityAPI target) {
        return target instanceof ShipAPI targetShip && targetShip.getMutableStats() != null;
    }

    private HitMix.Hit hitFor(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        HitMix.Hit recorded = recordedHit(damage, target);
        if (recorded != null) {
            return recorded;
        }
        TypeShares inherited = inheritedShares(damage);
        if (inherited == null) {
            return null;
        }
        HitMix.Hit hit = HitMix.Hit.inheriting(param, target, damage, point, shieldHit, inherited);
        record(hit);
        return hit;
    }

    private static void scaleEmp(HitMix.Hit hit, DamageAPI damage, CombatEntityAPI target, float genericPool) {
        float empScale = hit.empScaleStillToApply(genericPool);
        if (empScale != 1f && damage.getFluxComponent() > 0f) {
            HitDamageRestorer.scaleEmpForThisHit(damage, empScale, target);
        }
    }

    private static void scaleBase(DamageAPI damage, float typeScale, CombatEntityAPI target) {
        if (typeScale != 1f) {
            HitDamageRestorer.scaleDamageForThisHit(damage, typeScale, target);
        }
    }

    private static String scaleModifier(DamageAPI damage, float typeScale) {
        if (typeScale == 1f) {
            damage.getModifier().unmodifyMult(TYPE_SCALE_MOD_ID);
            return null;
        }
        damage.getModifier().modifyMult(TYPE_SCALE_MOD_ID, typeScale);
        return TYPE_SCALE_MOD_ID;
    }

    private void open(DamageAPI damage, CombatEntityAPI target, TypeShares shares, Mults blend, float now) {
        openHits.push(new OpenHit(damage, target, damage.getType(), shares, DamageTypeMults.current(CARRIER), now, HitDamageRestorer.sideDamageDepth()));
        if (damage.getType() != CARRIER) {
            damage.setType(CARRIER);
        }
        DamageTypeMults.set(CARRIER, blend);
    }

    void closeHit(CombatEntityAPI target, ApplyDamageResultAPI result) {
        OpenHit top = openHits.peek();
        if (top == null || top.target() != target) {
            return;
        }
        DamageType reportedType = result == null ? null : result.getType();
        if (reportedType != null && reportedType != top.damage().getType()) {
            return;
        }
        openHits.pop();
        restore(top);
    }

    private static void restore(OpenHit hit) {
        if (!hit.blended()) {
            return;
        }
        if (hit.damage().getType() != hit.ownType()) {
            hit.damage().setType(hit.ownType());
        }
        DamageTypeMults.set(CARRIER, hit.previousBlend());
    }

    private void dropStaleHits(float now) {
        OpenHit top = openHits.peek();
        if (top != null && top.frameTime() != now) {
            resetAll();
        }
    }

    private void endRestorerFrame() {
        if (restorer != null) {
            restorer.endFrame();
        }
    }

    void hitLanded(DamageAPI damage, CombatEntityAPI target) {
        HitMix.Hit hit = recordedHit(damage, target);
        if (hit != null) {
            hit.forgetAppliedScales();
        }
    }

    void resetAll() {
        while (!openHits.isEmpty()) {
            restore(openHits.pop());
        }
        DamageTypeMults.restoreDefaults();
    }

    private Map<HitKey, HitMix.Hit> hitsThisFrame() {
        float now = now();
        if (now != hitsFrameTime) {
            hitsThisFrame.clear();
            hitsFrameTime = now;
        }
        return hitsThisFrame;
    }

    private static float now() {
        CombatEngineAPI engine = Global.getCombatEngine();
        return engine == null ? 0f : engine.getTotalElapsedTime(false);
    }

    static final class FrameReset extends BaseEveryFrameCombatPlugin {

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            MixedHitBlender blender = existing();
            if (blender != null) {
                blender.resetAll();
                blender.endRestorerFrame();
            } else {
                DamageTypeMults.restoreDefaults();
            }
        }
    }
}
