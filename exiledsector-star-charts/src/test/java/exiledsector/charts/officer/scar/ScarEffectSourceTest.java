package exiledsector.charts.officer.scar;

import exiledsector.skills.EffectContribution;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.scar.ScarSkillEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScarEffectSourceTest {

    @BeforeEach
    void loadScars() throws Exception {
        ScarFixtures.loadShipped();
    }

    @Test
    void effectsScaleWithStacks() {
        List<SkillTypeEffect> effects = ScarEffectSource.effects(ScarTable.row("flak_scarred"), 3, ScarFixtures.SHIELDED_WARSHIP);
        assertEquals(List.of(new SkillTypeEffect(ScarSkillEffect.SCAR_PD_DAMAGE_TO_MISSILES_PERCENT, 45f),
                new SkillTypeEffect(ScarSkillEffect.SCAR_MISSILE_DAMAGE_TAKEN_PERCENT, -30f)), effects);
    }

    @Test
    void onlyEffectsFittingTheHullApply() {
        ScarRow kinetic = ScarTable.row("kinetic_scarred");
        assertEquals(List.of(new SkillTypeEffect(ScarSkillEffect.SCAR_SHIELD_DAMAGE_TAKEN_PERCENT, -8f)),
                ScarEffectSource.effects(kinetic, 1, ScarFixtures.SHIELDED_WARSHIP));
        assertEquals(List.of(new SkillTypeEffect(ScarSkillEffect.SCAR_KINETIC_DAMAGE_TAKEN_PERCENT, -10f)),
                ScarEffectSource.effects(kinetic, 1, ScarFixtures.UNSHIELDED_WARSHIP));
    }

    @Test
    void shieldShapeDecidesWhichBackstabbedEffectApplies() {
        ScarRow backstabbed = ScarTable.row("backstabbed");
        assertEquals(List.of(new SkillTypeEffect(ScarSkillEffect.SCAR_SHIELD_ARC_PERCENT, 15f)),
                ScarEffectSource.effects(backstabbed, 1, ScarFixtures.SHIELDED_WARSHIP));
        assertEquals(List.of(new SkillTypeEffect(ScarSkillEffect.SCAR_SHIELD_TURN_RATE_PERCENT, 20f)),
                ScarEffectSource.effects(backstabbed, 1, ScarFixtures.OMNI_CARRIER));
        assertEquals(List.of(new SkillTypeEffect(ScarSkillEffect.SCAR_MANEUVERABILITY_PERCENT, 15f)),
                ScarEffectSource.effects(backstabbed, 1, ScarFixtures.PHASE_SHIP));
    }

    @Test
    void wingEffectsNeedFighterBays() {
        ScarRow waspStung = ScarTable.row("wasp_stung");
        assertEquals(1, ScarEffectSource.effects(waspStung, 1, ScarFixtures.SHIELDED_WARSHIP).size());
        assertEquals(2, ScarEffectSource.effects(waspStung, 1, ScarFixtures.OMNI_CARRIER).size());
    }

    @Test
    void noStacksUnknownScarsOrHullGiveNoEffects() {
        assertTrue(ScarEffectSource.effects(ScarTable.row("flak_scarred"), 0, ScarFixtures.SHIELDED_WARSHIP).isEmpty());
        assertTrue(ScarEffectSource.effects(null, 2, ScarFixtures.SHIELDED_WARSHIP).isEmpty());
        assertTrue(ScarEffectSource.effects(ScarTable.row("flak_scarred"), 2, null).isEmpty());
    }

    @Test
    void eachScarIsItsOwnContributionAndEffectlessScarsAddNone() {
        Map<String, Integer> scars = new LinkedHashMap<>();
        scars.put("blast_scarred", 2);
        scars.put("humbled", 1);
        scars.put("rammed", 1);
        scars.put("no_such_scar", 1);
        List<EffectContribution> contributions = ScarEffectSource.contributions(scars, ScarFixtures.SHIELDED_WARSHIP);
        assertEquals(2, contributions.size());
        assertEquals(new ScarOrigin("blast_scarred"), contributions.get(0).origin());
        assertEquals(ScarEffectSource.MOD_ID_PREFIX + "blast_scarred_", contributions.get(0).modIdPrefix());
        assertEquals(16f, contributions.get(0).effects().get(0).magnitude());
        assertEquals(new ScarOrigin("rammed"), contributions.get(1).origin());
    }
}
