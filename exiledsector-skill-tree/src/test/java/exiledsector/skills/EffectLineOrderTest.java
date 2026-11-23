package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.skilleffect.defense.DefenseSkillEffect;
import exiledsector.skills.skilleffect.flux.FluxSkillEffect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EffectLineOrderTest {

    private static final SkillType MIXED = new SkillType.Builder("mixed", "Mixed", "a.png", SkillTier.NOTABLE)
            .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f), new SkillTypeEffect(FluxSkillEffect.FLUX_CAPACITY_MULT, -10f)))
            .hullSizeEffects(List.of(new HullSizeSkillEffect(DefenseSkillEffect.ARMOR_FLAT, 50f, 50f, 50f, 50f),
                    new HullSizeSkillEffect(FluxSkillEffect.FLUX_DISSIPATION_MULT, -5f, -5f, -5f, -5f)))
            .build();

    @Test
    void hullSizedUpsidesJoinTheUpsidesAndHullSizedDownsidesFollowTheDownsides() {
        List<String> lines = SkillNode.describeEffects(MIXED, HullSize.CRUISER).stream().map(DescriptionLine::plain).toList();

        assertEquals(List.of(DefenseSkillEffect.HULL_PERCENT.description(10f).plain(), DefenseSkillEffect.ARMOR_FLAT.description(50f).plain(),
                FluxSkillEffect.FLUX_CAPACITY_MULT.description(-10f).plain(), FluxSkillEffect.FLUX_DISSIPATION_MULT.description(-5f).plain()), lines);
    }
}
