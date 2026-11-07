package exiledsector.skills.skilleffect;

import exiledsector.i18n.HeadlessGameText;
import exiledsector.i18n.StyledText;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EffectNamingTest {

    private static final Pattern MULT_NAME = Pattern.compile("_MULT(_|$)");
    private static final Pattern PERCENT_NAME = Pattern.compile("_PERCENT(_|$)");
    private static final Pattern ADDITIVE_WORDING = Pattern.compile("\\d%\\s+(increased|reduced)\\b");
    private static final Pattern MULTIPLICATIVE_WORDING = Pattern.compile("\\d%\\s+(more|less)\\b");

    @Test
    void effectSuffixesMatchTheirTooltipWording() throws Exception {
        List<String> misnamed = new ArrayList<>();
        try (HeadlessGameText ignored = HeadlessGameText.install(() -> true)) {
            for (String name : new TreeSet<>(SkillEffectNames.all())) {
                StyledText description = SkillEffect.byName(name).description(10f);
                if (description == null) {
                    continue;
                }
                String text = description.plain();
                if (MULT_NAME.matcher(name).find() && ADDITIVE_WORDING.matcher(text).find()) {
                    misnamed.add(name + " says increased/reduced, so it should end in _PERCENT: " + text);
                } else if (PERCENT_NAME.matcher(name).find() && MULTIPLICATIVE_WORDING.matcher(text).find()) {
                    misnamed.add(name + " says more/less, so it should end in _MULT: " + text);
                }
            }
        }

        assertEquals(List.of(), misnamed, "Name an effect _MULT when it applies as its own multiplier (more/less) and _PERCENT"
                + " when it is summed with the stat's other sources (increased/reduced). See docs/tooltip_wording.md.");
    }
}
