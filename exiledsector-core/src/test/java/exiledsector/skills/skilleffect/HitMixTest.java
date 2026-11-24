package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static exiledsector.skills.skilleffect.MixedHitCombat.damage;
import static exiledsector.skills.skilleffect.MixedHitCombat.mix;
import static exiledsector.skills.skilleffect.MixedHitCombat.ship;
import static exiledsector.skills.skilleffect.MixedHitCombat.trackedDamage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HitMixTest {

    private static final float TOLERANCE = 1e-4f;

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();

    @RegisterExtension
    final DamageTypeMultsExtension typeMults = new DamageTypeMultsExtension();

    private final List<String> calls = new ArrayList<>();
    private MixedHitCombat mixedHits;

    @BeforeEach
    void setUp() {
        mixedHits = new MixedHitCombat(combat.engine());
    }

    private HitMix.Resolver resolver(String name) {
        return hit -> calls.add(name);
    }

    private static ShipAPI attackerWith(HitMix.Resolver... resolvers) {
        ShipAPI attacker = mock(ShipAPI.class);
        when(attacker.hasListenerOfClass(HitMix.Resolver.class)).thenReturn(true);
        when(attacker.getListeners(HitMix.Resolver.class)).thenReturn(List.of(resolvers));
        return attacker;
    }

    @Test
    void everyResolverOnTheAttackerRunsOncePerHit() {
        HitMix.Resolver first = resolver("first");
        ShipAPI attacker = attackerWith(first, resolver("second"));
        ShipAPI target = ship();
        DamageAPI damage = damage(DamageType.KINETIC);

        HitMix.Hit hit = HitMix.resolve(attacker, first, null, target, damage, null, false);
        HitMix.Hit again = HitMix.resolve(attacker, null, target, damage, null, false);

        assertSame(hit, again);
        assertEquals(List.of("first", "second"), calls);
    }

    @Test
    void aResolverPassedInRunsAfterTheAttackersOwn() {
        ShipAPI attacker = attackerWith(resolver("own"));

        HitMix.resolve(attacker, resolver("passed"), null, ship(), damage(DamageType.KINETIC), null, false);

        assertEquals(List.of("own", "passed"), calls);
    }

    @Test
    void aPartialConversionIsSeenByEveryReaderOfTheHit() {
        ShipAPI attacker = attackerWith(hit -> hit.convert(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f));
        ShipAPI target = ship();
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 0f);

        TypeShares shares = HitMix.shares(attacker, null, target, damage, null, false);

        assertEquals(mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f), shares);
        assertEquals(shares, HitMix.recorded(damage, target));
        assertEquals(DamageType.KINETIC, damage.getType());
        assertEquals(100f, damage.getBaseDamage(), TOLERANCE);
    }

    @Test
    void theHitRemembersWhatItWasBeforeAnyConversion() {
        ShipAPI attacker = attackerWith(hit -> hit.convert(DamageType.KINETIC, DamageType.ENERGY, 1f));
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 30f);

        HitMix.Hit hit = HitMix.resolve(attacker, null, ship(), damage, null, false);

        assertEquals(DamageType.KINETIC, hit.originalType());
        assertEquals(TypeShares.single(DamageType.KINETIC), hit.originalShares());
        assertEquals(30f, hit.originalEmp(), TOLERANCE);
        assertEquals(TypeShares.single(DamageType.ENERGY), hit.shares());
    }

    @Test
    void conversionsFromSeveralResolversAreAllTakenFromTheOriginalSplit() {
        ShipAPI attacker = attackerWith(hit -> hit.convert(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.5f),
                hit -> hit.convert(DamageType.HIGH_EXPLOSIVE, DamageType.ENERGY, 0.5f));

        TypeShares shares = HitMix.shares(attacker, null, ship(), trackedDamage(DamageType.KINETIC, 100f, 0f), null, false);

        assertEquals(0.5f, shares.share(DamageType.KINETIC), TOLERANCE);
        assertEquals(0.5f, shares.share(DamageType.HIGH_EXPLOSIVE), TOLERANCE);
        assertEquals(0f, shares.share(DamageType.ENERGY), TOLERANCE);
    }

    @Test
    void theOrderResolversRunInDoesNotChangeTheConvertedHit() {
        HitMix.Resolver toEnergy = hit -> hit.convert(DamageType.KINETIC, DamageType.ENERGY, 0.7f);
        HitMix.Resolver toFragmentation = hit -> hit.convert(DamageType.KINETIC, DamageType.FRAGMENTATION, 0.6f);

        TypeShares forwards = HitMix.shares(attackerWith(toEnergy, toFragmentation), null, ship(), trackedDamage(DamageType.KINETIC, 100f, 0f), null,
                false);
        TypeShares backwards = HitMix.shares(attackerWith(toFragmentation, toEnergy), null, ship(), trackedDamage(DamageType.KINETIC, 100f, 0f), null,
                false);

        assertEquals(0.7f / 1.3f, forwards.share(DamageType.ENERGY), TOLERANCE);
        assertEquals(0.6f / 1.3f, forwards.share(DamageType.FRAGMENTATION), TOLERANCE);
        assertEquals(0f, forwards.share(DamageType.KINETIC), TOLERANCE);
        for (DamageType type : DamageType.values()) {
            assertEquals(forwards.share(type), backwards.share(type), TOLERANCE, type.name());
        }
    }

    @Test
    void aConversionFromEveryTypeTakesTheShareOfEachType() {
        ShipAPI attacker = attackerWith(hit -> hit.convert(null, DamageType.ENERGY, 0.5f));

        TypeShares shares = HitMix.shares(attacker, null, ship(), trackedDamage(DamageType.KINETIC, 100f, 0f), null, false);

        assertEquals(mix(DamageType.KINETIC, DamageType.ENERGY, 0.5f), shares);
    }

    @Test
    void aWholeConversionRetypesTheHitUntilItsReport() {
        ShipAPI attacker = attackerWith(hit -> hit.convert(DamageType.FRAGMENTATION, DamageType.HIGH_EXPLOSIVE, 1f));
        ShipAPI target = ship();
        DamageAPI damage = trackedDamage(DamageType.FRAGMENTATION, 100f, 0f);

        HitMix.Hit hit = HitMix.resolve(attacker, null, target, damage, null, false);

        assertEquals(TypeShares.single(DamageType.HIGH_EXPLOSIVE), hit.shares());
        assertEquals(DamageType.HIGH_EXPLOSIVE, damage.getType());

        mixedHits.report(target, DamageType.HIGH_EXPLOSIVE);

        assertEquals(DamageType.FRAGMENTATION, damage.getType());
    }

    @Test
    void damageConvertedToEmpMovesIntoTheHitsEmpUntilItsReport() {
        ShipAPI attacker = attackerWith(hit -> hit.convertToEmp(DamageType.KINETIC, 0.5f));
        ShipAPI target = ship();
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 10f);

        HitMix.Hit hit = HitMix.resolve(attacker, null, target, damage, null, false);

        assertEquals(TypeShares.single(DamageType.KINETIC), hit.shares());
        assertEquals(50f, damage.getBaseDamage(), TOLERANCE);
        assertEquals(60f, damage.getFluxComponent(), TOLERANCE);

        mixedHits.report(target, DamageType.KINETIC);

        assertEquals(100f, damage.getBaseDamage(), TOLERANCE);
        assertEquals(10f, damage.getFluxComponent(), TOLERANCE);
    }

    @Test
    void empMovedFromTheDamageIsNotConvertedOnByAnEmpConversion() {
        ShipAPI attacker = attackerWith(hit -> hit.convertToEmp(DamageType.KINETIC, 0.5f), hit -> hit.convertEmp(DamageType.ENERGY, 0.5f));
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 20f);

        HitMix.Hit hit = HitMix.resolve(attacker, null, ship(), damage, null, false);

        assertEquals(60f, damage.getBaseDamage(), TOLERANCE);
        assertEquals(60f, damage.getFluxComponent(), TOLERANCE);
        assertEquals(50f / 60f, hit.shares().share(DamageType.KINETIC), TOLERANCE);
        assertEquals(10f / 60f, hit.shares().share(DamageType.ENERGY), TOLERANCE);
    }

    @Test
    void empGainedAsExtraIsTakenFromTheOriginalDamageOnTopOfAWholeConversion() {
        ShipAPI attacker = attackerWith(hit -> hit.convert(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 1f), hit -> hit.gainEmp(1.5f));
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 0f);

        HitMix.Hit hit = HitMix.resolve(attacker, null, ship(), damage, null, false);

        assertEquals(TypeShares.single(DamageType.HIGH_EXPLOSIVE), hit.shares());
        assertEquals(100f, damage.getBaseDamage(), TOLERANCE);
        assertEquals(150f, damage.getFluxComponent(), TOLERANCE);
    }

    @Test
    void aHitOffAShipTakesItsLargestShareWithoutMovingItsDamage() {
        ShipAPI attacker = attackerWith(hit -> hit.convert(DamageType.KINETIC, DamageType.ENERGY, 1f), hit -> hit.convertToEmp(null, 0.5f));
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 0f);

        HitMix.resolve(attacker, null, mock(CombatEntityAPI.class), damage, null, false);

        assertEquals(DamageType.ENERGY, damage.getType());
        assertEquals(100f, damage.getBaseDamage(), TOLERANCE);
        assertEquals(0f, damage.getFluxComponent(), TOLERANCE);
    }

    @Test
    void aHitAppliedAsAlreadyConvertedRunsItsResolversButKeepsItsOwnSplit() {
        ShipAPI attacker = attackerWith(hit -> {
            calls.add("resolved");
            hit.convert(DamageType.HIGH_EXPLOSIVE, DamageType.ENERGY, 1f);
        });
        Object landingArc = new Object();
        DamageAPI landing = trackedDamage(DamageType.HIGH_EXPLOSIVE, 100f, 0f);
        DamageAPI later = trackedDamage(DamageType.HIGH_EXPLOSIVE, 100f, 0f);
        List<HitMix.Hit> landed = new ArrayList<>();

        HitMix.applyConverted(landingArc, () -> landed.add(HitMix.resolve(attacker, landingArc, ship(), landing, null, false)));
        HitMix.Hit laterHit = HitMix.resolve(attacker, landingArc, ship(), later, null, false);

        assertEquals(List.of("resolved", "resolved"), calls);
        assertEquals(TypeShares.single(DamageType.HIGH_EXPLOSIVE), landed.get(0).shares());
        assertEquals(DamageType.HIGH_EXPLOSIVE, landing.getType());
        assertEquals(TypeShares.single(DamageType.ENERGY), laterHit.shares());
        assertEquals(DamageType.ENERGY, later.getType());
    }

    @Test
    void aHitOfAnotherParamInsideAnAlreadyConvertedHitIsStillConverted() {
        ShipAPI attacker = attackerWith(hit -> hit.convert(DamageType.HIGH_EXPLOSIVE, DamageType.ENERGY, 1f));
        DamageAPI inner = trackedDamage(DamageType.HIGH_EXPLOSIVE, 100f, 0f);
        List<HitMix.Hit> landed = new ArrayList<>();

        HitMix.applyConverted(new Object(), () -> landed.add(HitMix.resolve(attacker, new Object(), ship(), inner, null, false)));

        assertEquals(TypeShares.single(DamageType.ENERGY), landed.get(0).shares());
    }

    @Test
    void aCarrierHitInheritingAnOpenMixIsNeverConverted() {
        ShipAPI target = ship();
        DamageAPI outer = damage(DamageType.KINETIC);
        DamageAPI inner = trackedDamage(MixedHitBlender.CARRIER, 100f, 10f);
        MixedHitBlender blender = mixedHits.blender();
        TypeShares outerMix = mix(DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE, 0.4f);
        blender.record(HitMix.Hit.inheriting(null, target, outer, null, false, outerMix));
        blender.modifyDamageTaken(null, target, outer, null, false);
        ShipAPI attacker = attackerWith(hit -> {
            hit.convert(null, DamageType.ENERGY, 1f);
            hit.gainEmp(1f);
        });

        HitMix.Hit hit = HitMix.resolve(attacker, null, target, inner, null, false);

        assertEquals(outerMix, hit.shares());
        assertEquals(MixedHitBlender.CARRIER, inner.getType());
        assertEquals(100f, inner.getBaseDamage(), TOLERANCE);
        assertEquals(10f, inner.getFluxComponent(), TOLERANCE);
    }

    @Test
    void aBeamRetypedOffAShipStartsItsNextHitFromItsOwnType() {
        BeamAPI beam = mock(BeamAPI.class);
        CombatEntityAPI missile = mock(CombatEntityAPI.class);
        DamageAPI beamDamage = trackedDamage(DamageType.ENERGY, 100f, 0f);
        boolean[] converting = {true};
        ShipAPI attacker = attackerWith(hit -> {
            if (converting[0]) {
                hit.convert(DamageType.ENERGY, DamageType.HIGH_EXPLOSIVE, 0.7f);
            }
        });

        HitMix.resolve(attacker, beam, missile, beamDamage, null, false);
        DamageType retyped = beamDamage.getType();
        mixedHits.nextFrame();
        converting[0] = false;
        HitMix.Hit next = HitMix.resolve(attacker, beam, missile, beamDamage, null, false);

        assertEquals(DamageType.HIGH_EXPLOSIVE, retyped);
        assertEquals(DamageType.ENERGY, next.originalType());
        assertEquals(DamageType.ENERGY, beamDamage.getType());
    }

    @Test
    void aBeamRetypedOffAShipKeepsItsOwnTypeAfterAShipHitThatChangesItsAmounts() {
        BeamAPI beam = mock(BeamAPI.class);
        CombatEntityAPI missile = mock(CombatEntityAPI.class);
        ShipAPI target = ship();
        DamageAPI beamDamage = trackedDamage(DamageType.ENERGY, 100f, 0f);
        ShipAPI attacker = attackerWith(hit -> {
            hit.convert(DamageType.ENERGY, DamageType.HIGH_EXPLOSIVE, 1f);
            if (hit.target() == target) {
                hit.gainEmp(0.5f);
            }
        });

        HitMix.resolve(attacker, beam, missile, beamDamage, null, false);
        mixedHits.nextFrame();
        HitMix.resolve(attacker, beam, target, beamDamage, null, false);
        DamageType duringShipHit = beamDamage.getType();
        mixedHits.report(target, DamageType.HIGH_EXPLOSIVE);
        mixedHits.nextFrame();
        HitMix.Hit next = HitMix.resolve(attacker, beam, missile, beamDamage, null, false);

        assertEquals(DamageType.HIGH_EXPLOSIVE, duringShipHit);
        assertEquals(DamageType.ENERGY, next.originalType());
    }

    @Test
    void scalingRequestsPoolWithTheHitsGenericDamageIncreases() {
        ShipAPI attacker = attackerWith(hit -> {
            hit.increase(DamageType.KINETIC, 50f);
            hit.more(DamageType.KINETIC, 2f);
            hit.increaseEmp(100f);
            hit.moreEmp(0.5f);
        });
        DamageAPI damage = trackedDamage(DamageType.KINETIC, 100f, 0f);
        damage.getModifier().modifyPercent("generic", 50f);

        HitMix.Hit hit = HitMix.resolve(attacker, null, ship(), damage, null, false);
        float genericPool = hit.genericPool();

        assertEquals(1.5f, genericPool, TOLERANCE);
        assertEquals(2f / 1.5f * 2f, hit.damageScale(DamageType.KINETIC, genericPool), TOLERANCE);
        assertEquals(1f, hit.damageScale(DamageType.ENERGY, genericPool), TOLERANCE);
        assertEquals(2.5f / 1.5f * 0.5f, hit.empScale(genericPool), TOLERANCE);
    }

    @Test
    void anEmpOnlyHitConvertsAndScalesItsEmp() {
        ShipAPI dealer = attackerWith(hit -> {
            hit.convertEmp(DamageType.ENERGY, 0.25f);
            hit.increaseEmp(50f);
        });

        HitMix.EmpOnlyResult result = HitMix.resolveEmpOnly(dealer, null, ship(), null, false, 100f);

        assertEquals(75f * 1.5f, result.emp(), TOLERANCE);
        assertEquals(1.5f, result.empMult(), TOLERANCE);
        assertEquals(Map.of(DamageType.ENERGY, 25f), result.movedDamage());
    }

    @Test
    void aHitNoResolverTouchedIsItsOwnType() {
        DamageAPI damage = damage(DamageType.ENERGY);

        assertEquals(TypeShares.single(DamageType.ENERGY), HitMix.shares(mock(ShipAPI.class), null, ship(), damage, null, false));
    }

    @Test
    void aNewFrameForgetsLastFramesHits() {
        ShipAPI attacker = attackerWith(resolver("only"));
        ShipAPI target = ship();
        DamageAPI damage = damage(DamageType.KINETIC);

        HitMix.resolve(attacker, null, target, damage, null, false);
        mixedHits.nextFrame();
        HitMix.resolve(attacker, null, target, damage, null, false);

        assertEquals(List.of("only", "only"), calls);
    }
}
