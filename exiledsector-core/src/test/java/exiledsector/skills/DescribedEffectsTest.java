package exiledsector.skills;

import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.SkillEffectNames;
import exiledsector.skills.skilleffect.aura.AuraSkillEffect;
import exiledsector.skills.skilleffect.weapon.WeaponSkillEffect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DescribedEffectsTest {

    private static List<String> plainLines(SkillTypeEffect... effects) {
        return DescribedEffects.of(List.of(effects), null).stream().map(line -> line.text().plain()).toList();
    }

    @Test
    void aParentLineShowsTheValueItsCompanionRaisesAndTheCompanionDropsOut() {
        List<String> lines = plainLines(new SkillTypeEffect(AuraSkillEffect.SMALLER_ALLY_DAMAGE_TAKEN_MULT, 10f),
                new SkillTypeEffect(AuraSkillEffect.SMALLER_ALLY_DAMAGE_TAKEN_RANGE_FLAT, 400f));

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("within 1200 su"), lines.get(0));
    }

    @Test
    void aCompanionWithoutItsParentKeepsItsOwnLine() {
        List<String> lines = plainLines(new SkillTypeEffect(WeaponSkillEffect.PIRATE_FRENZY_SECONDS_FLAT, 5f));

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("+5"), lines.get(0));
    }

    @Test
    void everyCompanionSharesALineWithItsParents() {
        for (String name : SkillEffectNames.all()) {
            SkillEffect effect = SkillEffect.byName(name);
            for (SkillEffect parent : effect.refines()) {
                List<String> lines = plainLines(new SkillTypeEffect(parent, 10f), new SkillTypeEffect(effect, 5f));
                assertEquals(1, lines.size(), effect.name() + " should fold into " + parent.name());
            }
        }
    }
}
