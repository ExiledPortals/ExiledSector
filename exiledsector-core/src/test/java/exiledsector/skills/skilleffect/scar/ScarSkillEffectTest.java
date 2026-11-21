package exiledsector.skills.skilleffect.scar;

import exiledsector.i18n.HeadlessGameText;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScarSkillEffectTest {

    @Test
    void everyScarEffectIsRegisteredUnderItsOwnName() {
        for (ScarSkillEffect effect : ScarSkillEffect.values()) {
            assertSame(effect, SkillEffect.byName(effect.name()));
        }
    }

    @Test
    void everyScarEffectIsNamedScarAndEndsInPercentOrMult() {
        List<String> misnamed = new ArrayList<>();
        for (ScarSkillEffect effect : ScarSkillEffect.values()) {
            String name = effect.name();
            if (!name.startsWith("SCAR_") || !(name.endsWith("_PERCENT") || name.endsWith("_MULT"))) {
                misnamed.add(name);
            }
        }
        assertEquals(List.of(), misnamed);
    }

    @Test
    void percentEffectsAreSummedWithTheStatsOtherSources() {
        List<String> multiplied = new ArrayList<>();
        for (ScarSkillEffect effect : ScarSkillEffect.values()) {
            if (effect.name().endsWith("_PERCENT") && effect.statMode() == StatMode.MULT) {
                multiplied.add(effect.name());
            }
        }
        assertEquals(List.of(), multiplied);
    }

    @Test
    void everyScarEffectHasItsOwnText() throws Exception {
        List<String> undescribed = new ArrayList<>();
        try (HeadlessGameText ignored = HeadlessGameText.install(() -> false)) {
            for (ScarSkillEffect effect : ScarSkillEffect.values()) {
                StyledText description = effect.description(10f);
                String text = description == null ? "" : description.plain();
                if (text.isBlank() || text.contains("effect.") || text.contains("stat.") || text.contains("{")) {
                    undescribed.add(effect.name() + ": " + text);
                }
            }
        }
        assertEquals(List.of(), undescribed);
    }
}
