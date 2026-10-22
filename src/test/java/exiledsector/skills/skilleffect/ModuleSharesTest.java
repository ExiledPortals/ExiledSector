package exiledsector.skills.skilleffect;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleSharesTest {

    private static Set<String> effectsUsedInSkillTypes() throws IOException {
        String json = Files.readString(Path.of("data/skilltrees/skill_types.json"), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("\"effect\"\\s*:\\s*\"([A-Z0-9_]+)\"").matcher(json);
        Set<String> names = new TreeSet<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    @Test
    void everyEffectUsedInSkillTypesHasAModuleShareValue() throws IOException {
        Set<String> unclassified = new TreeSet<>(effectsUsedInSkillTypes());
        unclassified.removeAll(ModuleShares.classifiedNames());

        assertTrue(unclassified.isEmpty(), "Classify these effects for modules in ModuleShares: " + unclassified);
    }

    @Test
    void everyClassifiedNameIsARegisteredEffect() {
        Set<String> unknown = new TreeSet<>(ModuleShares.classifiedNames());
        unknown.removeAll(SkillEffectRegistry.names());

        assertTrue(unknown.isEmpty(), "ModuleShares lists effects that don't exist: " + unknown);
    }

    @Test
    void nothingIsSharedWithoutARebuildWhenItsStatIsFixedAtBuild() {
        Pattern fixedAtBuild = Pattern.compile("^(ARMOR|HULL|SHIELD_ARC)_.*|.*_AMMO_PERCENT|.*FIGHTER_BAYS.*|.*DURABILITY.*");
        Set<String> sharedLive = new TreeSet<>();
        for (String name : ModuleShares.classifiedNames()) {
            if (fixedAtBuild.matcher(name).matches() && SkillEffect.byName(name).moduleShare() == ModuleShare.SHARE) {
                sharedLive.add(name);
            }
        }

        assertTrue(sharedLive.isEmpty(), "These effects change stats fixed at ship build; make them SHARE_REBUILD or exclude them: " + sharedLive);
    }

    @Test
    void sharedEffectsNeedNoCombatTickBecauseTheShareOnlyCallsApply() {
        Set<String> advancing = new TreeSet<>();
        for (String name : ModuleShares.classifiedNames()) {
            SkillEffect effect = SkillEffect.byName(name);
            if (effect.moduleShare() == ModuleShare.SHARE && effect.advancesInCombat()) {
                advancing.add(name);
            }
        }

        assertTrue(advancing.isEmpty(), "These SHARE effects need a combat tick the share never runs: " + advancing);
    }

    @Test
    void effectsLeftOutOfTheTableAreExcluded() {
        for (String name : SkillEffectRegistry.names()) {
            if (!ModuleShares.classifiedNames().contains(name)) {
                assertEquals(ModuleShare.EXCLUDE, SkillEffect.byName(name).moduleShare(), name);
            }
        }
    }
}
