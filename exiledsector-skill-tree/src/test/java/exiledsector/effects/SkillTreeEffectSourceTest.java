package exiledsector.effects;

import exiledsector.skills.CombatGate;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.defense.DefenseSkillEffect;
import exiledsector.items.RadiusLineOrigin;
import exiledsector.items.SubroutineOrigin;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillTreeEffectSourceTest {

    private static AllocatedNode allocated(String id) {
        SkillType type = new SkillType.Builder(id, id, "a.png", SkillTier.SMALL).build();
        return new AllocatedNode(new SkillNode(id, type, List.of(), 0f, 0f), type);
    }

    private static EffectContribution contribution(Object origin, float hullPercent, CombatGate gate) {
        return new EffectContribution(origin, "prefix_", List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, hullPercent)), null, null, true, true,
                gate);
    }

    @Test
    void subroutineAndRadiusLinesFoldBackIntoTheirNodeAndGatedOnesStayOut() {
        AllocatedNode socket = allocated("socket");
        AllocatedNode covered = allocated("covered");
        AllocatedNode gated = allocated("gated");

        List<SkillTreeEffectSource.NodeEffects> nodeEffects = SkillTreeEffectSource.nodeEffectsOf(List.of(
                contribution(socket, 1f, null),
                contribution(covered, 2f, null),
                contribution(new SubroutineOrigin(socket), 10f, null),
                contribution(new RadiusLineOrigin(covered, "socket"), 20f, null),
                contribution(new RadiusLineOrigin(gated, "socket"), 30f, CombatGate.LOW_HULL)));

        assertEquals(List.of(socket, covered), nodeEffects.stream().map(SkillTreeEffectSource.NodeEffects::node).toList());
        assertEquals(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 1f), new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)),
                nodeEffects.get(0).effects());
        assertEquals(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 2f), new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 20f)),
                nodeEffects.get(1).effects());
    }
}
