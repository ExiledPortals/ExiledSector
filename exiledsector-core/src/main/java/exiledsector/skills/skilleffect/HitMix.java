package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static exiledsector.skills.skilleffect.HitConversions.EMP;
import static exiledsector.skills.skilleffect.HitConversions.PORTIONS;
import static exiledsector.skills.skilleffect.HitConversions.portionOf;

public final class HitMix {

    public interface Resolver {
        void resolve(Hit hit);
    }

    public record EmpOnlyResult(float emp, float empMult, Map<DamageType, Float> movedDamage) {
    }

    private static final DamageType[] DAMAGE_TYPES = DamageType.values();
    private static final Map<DamageAPI, DamageType> RETYPED_BEAMS = new WeakHashMap<>();

    public static final class Hit {

        private final Object param;
        private final CombatEntityAPI target;
        private final DamageAPI damage;
        private final Vector2f point;
        private final boolean shieldHit;
        private final DamageType originalType;
        private final TypeShares originalShares;
        private final float originalDamage;
        private final float originalEmp;
        private final boolean convertible;
        private final Set<ShipAPI> resolvedFor = Collections.newSetFromMap(new IdentityHashMap<>());
        private final HitConversions conversions = new HitConversions();
        private final HitScaling scaling = new HitScaling();
        private TypeShares shares;
        private float appliedDamageScale = 1f;
        private float appliedEmpScale = 1f;

        private Hit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit, DamageType originalType,
                    TypeShares originalShares, float originalDamage, float originalEmp, boolean convertible) {
            this.param = param;
            this.target = target;
            this.damage = damage;
            this.point = point;
            this.shieldHit = shieldHit;
            this.originalType = originalType;
            this.originalShares = originalShares;
            this.originalDamage = originalDamage;
            this.originalEmp = originalEmp;
            this.convertible = convertible;
            this.shares = originalShares;
        }

        static Hit of(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit, boolean convertible) {
            DamageType type = param instanceof BeamAPI ? RETYPED_BEAMS.getOrDefault(damage, damage.getType()) : damage.getType();
            return new Hit(param, target, damage, point, shieldHit, type, TypeShares.single(type), damage.getBaseDamage(), damage.getFluxComponent(),
                    convertible);
        }

        static Hit inheriting(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit, TypeShares inherited) {
            return new Hit(param, target, damage, point, shieldHit, inherited.largest(), inherited, damage.getBaseDamage(), damage.getFluxComponent(),
                    false);
        }

        private static Hit empOnly(Object param, ShipAPI target, Vector2f point, boolean shieldHit, float emp) {
            return new Hit(param, target, null, point, shieldHit, DamageType.ENERGY, TypeShares.single(DamageType.ENERGY), 0f, emp, true);
        }

        public Object param() {
            return param;
        }

        public CombatEntityAPI target() {
            return target;
        }

        public Vector2f point() {
            return point;
        }

        public boolean shieldHit() {
            return shieldHit;
        }

        public DamageType originalType() {
            return originalType;
        }

        public TypeShares originalShares() {
            return originalShares;
        }

        public float originalEmp() {
            return originalEmp;
        }

        public TypeShares shares() {
            return shares;
        }

        DamageAPI damage() {
            return damage;
        }

        public void convert(DamageType fromType, DamageType toType, float share) {
            for (DamageType type : DAMAGE_TYPES) {
                if (fromType == null || type == fromType) {
                    conversions.convert(portionOf(type), portionOf(toType), share);
                }
            }
        }

        public void convertToEmp(DamageType fromType, float share) {
            for (DamageType type : DAMAGE_TYPES) {
                if (fromType == null || type == fromType) {
                    conversions.convert(portionOf(type), EMP, share);
                }
            }
        }

        public void convertEmp(DamageType toType, float share) {
            conversions.convert(EMP, portionOf(toType), share);
        }

        public void gainEmp(float shareOfDamage) {
            for (DamageType type : DAMAGE_TYPES) {
                conversions.gain(portionOf(type), EMP, shareOfDamage);
            }
        }

        public void increase(DamageType type, float percent) {
            scaling.increase(portionOf(type), percent);
        }

        public void more(DamageType type, float mult) {
            scaling.more(portionOf(type), mult);
        }

        public void increaseEmp(float percent) {
            scaling.increase(EMP, percent);
        }

        public void moreEmp(float mult) {
            scaling.more(EMP, mult);
        }

        float genericPool() {
            return damage == null ? 1f : HitScaling.genericPool(damage.getModifier());
        }

        float damageScale(DamageType type, float genericPool) {
            return scaling.factor(portionOf(type), genericPool);
        }

        float empScale(float genericPool) {
            return scaling.factor(EMP, genericPool);
        }

        void forgetAppliedScales() {
            appliedDamageScale = 1f;
            appliedEmpScale = 1f;
        }

        float damageScaleStillToApply(float genericPool) {
            float scale = damageScale(shares.largest(), genericPool);
            if (appliedDamageScale == 0f) {
                return 1f;
            }
            float stillToApply = scale / appliedDamageScale;
            appliedDamageScale = scale;
            return stillToApply;
        }

        float empScaleStillToApply(float genericPool) {
            float scale = empScale(genericPool);
            if (appliedEmpScale == 0f) {
                return 1f;
            }
            float stillToApply = scale / appliedEmpScale;
            appliedEmpScale = scale;
            return stillToApply;
        }

        boolean resolvedFor(ShipAPI attacker) {
            return resolvedFor.contains(attacker);
        }

        void markResolvedFor(ShipAPI attacker) {
            if (attacker != null) {
                resolvedFor.add(attacker);
            }
        }

        private float[] originalPortions() {
            float[] portions = new float[PORTIONS];
            for (DamageType type : DAMAGE_TYPES) {
                portions[portionOf(type)] = originalDamage * originalShares.share(type);
            }
            portions[EMP] = originalEmp;
            return portions;
        }

        private void applyConversions() {
            if (!convertible) {
                return;
            }
            if (param instanceof BeamAPI && target instanceof ShipAPI && RETYPED_BEAMS.containsKey(damage)) {
                retypeBeam(originalType);
            }
            if (!conversions.isEmpty()) {
                float[] converted = conversions.applyTo(originalPortions());
                TypeShares convertedShares = TypeShares.ofAmounts(converted);
                shares = convertedShares == null ? originalShares : convertedShares;
                if (target instanceof ShipAPI) {
                    writeAmounts(converted);
                }
            }
            writeType();
        }

        private void writeAmounts(float[] converted) {
            float convertedDamage = 0f;
            for (DamageType type : DAMAGE_TYPES) {
                convertedDamage += converted[portionOf(type)];
            }
            float convertedEmp = converted[EMP];
            if (convertedDamage != damage.getBaseDamage() || convertedEmp != damage.getFluxComponent()) {
                HitDamageRestorer.setForThisHit(damage, convertedDamage, convertedEmp, target);
                forgetAppliedScales();
            }
        }

        private void writeType() {
            boolean beamOffShip = param instanceof BeamAPI && !(target instanceof ShipAPI);
            if (beamOffShip) {
                retypeBeam(shares.largest());
                return;
            }
            if (!shares.mixed() && damage.getType() != shares.largest()) {
                HitDamageRestorer.retypeForThisHit(damage, shares.largest(), target);
            }
        }

        private void retypeBeam(DamageType type) {
            if (type == originalType) {
                RETYPED_BEAMS.remove(damage);
            } else {
                RETYPED_BEAMS.put(damage, originalType);
            }
            if (damage.getType() != type) {
                damage.setType(type);
            }
        }
    }

    private static Object paramAlreadyConverted;

    private HitMix() {
    }

    public static void applyConverted(Object param, Runnable applyHit) {
        Object outerParam = paramAlreadyConverted;
        paramAlreadyConverted = param;
        try {
            applyHit.run();
        } finally {
            paramAlreadyConverted = outerParam;
        }
    }

    public static Hit resolve(ShipAPI attacker, Resolver resolving, Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point,
                              boolean shieldHit) {
        if (damage == null) {
            return null;
        }
        MixedHitBlender blender = MixedHitBlender.existing();
        Hit hit = blender == null ? null : blender.recordedHit(damage, target);
        if (hit != null && (attacker == null || hit.resolvedFor(attacker))) {
            return hit;
        }
        List<Resolver> resolvers = resolversOf(attacker, resolving);
        if (resolvers.isEmpty()) {
            return hit;
        }
        if (hit == null) {
            blender = blender == null ? MixedHitBlender.ensureInCombat() : blender;
            TypeShares inherited = blender == null ? null : blender.inheritedShares(damage);
            boolean alreadyConverted = param != null && param == paramAlreadyConverted;
            hit = inherited == null ? Hit.of(param, target, damage, point, shieldHit, !alreadyConverted)
                    : Hit.inheriting(param, target, damage, point, shieldHit, inherited);
            if (blender != null) {
                blender.record(hit);
            }
        }
        hit.markResolvedFor(attacker);
        for (Resolver resolver : resolvers) {
            resolver.resolve(hit);
        }
        hit.applyConversions();
        return hit;
    }

    public static Hit resolve(ShipAPI attacker, Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        return resolve(attacker, null, param, target, damage, point, shieldHit);
    }

    public static EmpOnlyResult resolveEmpOnly(ShipAPI dealer, Object param, ShipAPI target, Vector2f point, boolean shieldHit, float emp) {
        Hit hit = Hit.empOnly(param, target, point, shieldHit, emp);
        for (Resolver resolver : resolversOf(dealer, null)) {
            resolver.resolve(hit);
        }
        float[] converted = hit.conversions.applyTo(hit.originalPortions());
        Map<DamageType, Float> movedDamage = new EnumMap<>(DamageType.class);
        for (DamageType type : DAMAGE_TYPES) {
            if (converted[portionOf(type)] > 0f) {
                movedDamage.put(type, converted[portionOf(type)]);
            }
        }
        float empMult = hit.empScale(1f);
        return new EmpOnlyResult(converted[EMP] * empMult, empMult, movedDamage);
    }

    public static TypeShares shares(ShipAPI attacker, Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        Hit hit = resolve(attacker, param, target, damage, point, shieldHit);
        return hit == null ? recorded(damage, target) : hit.shares();
    }

    public static TypeShares shares(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        return shares(CombatQueries.attackingShip(null, param), param, target, damage, point, shieldHit);
    }

    public static TypeShares recorded(DamageAPI damage, CombatEntityAPI target) {
        if (damage == null) {
            return TypeShares.single(null);
        }
        MixedHitBlender blender = MixedHitBlender.existing();
        Hit hit = blender == null ? null : blender.recordedHit(damage, target);
        if (hit != null) {
            return hit.shares();
        }
        TypeShares open = blender == null ? null : blender.openSharesOf(damage);
        return open != null ? open : TypeShares.single(damage.getType());
    }

    private static List<Resolver> resolversOf(ShipAPI attacker, Resolver resolving) {
        List<Resolver> onShip = attacker == null || !attacker.hasListenerOfClass(Resolver.class) ? null : attacker.getListeners(Resolver.class);
        boolean noneOnShip = onShip == null || onShip.isEmpty();
        if (noneOnShip) {
            return resolving == null ? List.of() : List.of(resolving);
        }
        List<Resolver> resolvers = new ArrayList<>(onShip);
        if (resolving != null && !resolvers.contains(resolving)) {
            resolvers.add(resolving);
        }
        return resolvers;
    }
}
