package exiledsector.skills.skilleffect.stationmodule;

import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.SkillEffectNames;
import exiledsector.skills.skilleffect.aura.AuraSkillEffect;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        Set<String> unclassified = new TreeSet<>();
        for (String name : effectsUsedInSkillTypes()) {
            if (!ModuleShares.isClassified(SkillEffect.byName(name))) {
                unclassified.add(name);
            }
        }

        assertTrue(unclassified.isEmpty(), "Classify these effects for modules in ModuleShares: " + unclassified);
    }

    @Test
    void theListedSiblingsOfAnUnlistedListenerEffectNeverDisagree() {
        Map<Class<?>, Set<ModuleShare>> listedShares = ModuleShares.listedSharesByListenerType();
        Set<String> ambiguous = new TreeSet<>();
        for (String name : SkillEffectNames.all()) {
            Class<?> listenerType = ModuleShares.listenerTypeOf(SkillEffect.byName(name));
            Set<ModuleShare> siblingShares = listenerType == null ? Set.of() : listedShares.getOrDefault(listenerType, Set.of());
            if (!ModuleShares.classifiedNames().contains(name) && siblingShares.size() > 1) {
                ambiguous.add(name + " " + siblingShares);
            }
        }

        assertTrue(ambiguous.isEmpty(), "The listed effects sharing these effects' listeners disagree, so list them in ModuleShares: " + ambiguous);
    }

    @Test
    void anUnlistedListenerEffectTakesItsListedSiblingsShare() {
        assertFalse(ModuleShares.classifiedNames().contains(AuraSkillEffect.SMALLER_ALLY_DAMAGE_TAKEN_RANGE_FLAT.name()));
        assertEquals(AuraSkillEffect.SMALLER_ALLY_DAMAGE_TAKEN_MULT.moduleShare(), AuraSkillEffect.SMALLER_ALLY_DAMAGE_TAKEN_RANGE_FLAT.moduleShare());
    }

    @Test
    void everyClassifiedNameIsARegisteredEffect() {
        Set<String> unknown = new TreeSet<>(ModuleShares.classifiedNames());
        unknown.removeAll(SkillEffectNames.all());

        assertTrue(unknown.isEmpty(), "ModuleShares lists effects that don't exist: " + unknown);
    }

    @Test
    void nothingIsSharedWithoutARebuildWhenItsStatIsFixedAtBuild() {
        Set<String> sharedLive = new TreeSet<>();
        for (String name : SkillEffectNames.all()) {
            if (fixedAtBuild(name) && SkillEffect.byName(name).moduleShare() == ModuleShare.SHARE) {
                sharedLive.add(name);
            }
        }

        assertTrue(sharedLive.isEmpty(), "These effects change stats fixed at ship build; make them SHARE_REBUILD or exclude them: " + sharedLive);
    }

    private static boolean fixedAtBuild(String name) {
        return name.startsWith("ARMOR_") || name.startsWith("HULL_") || name.startsWith("SHIELD_ARC_") || name.endsWith("_AMMO_PERCENT")
                || name.contains("FIGHTER_BAYS") || name.contains("DURABILITY");
    }

    @Test
    void sharedEffectsNeedNoCombatTickBecauseTheShareOnlyCallsApply() {
        Set<String> advancing = new TreeSet<>();
        for (String name : SkillEffectNames.all()) {
            SkillEffect effect = SkillEffect.byName(name);
            if (effect.moduleShare() == ModuleShare.SHARE && effect.advancesInCombat()) {
                advancing.add(name);
            }
        }

        assertTrue(advancing.isEmpty(), "These SHARE effects need a combat tick the share never runs: " + advancing);
    }

    @Test
    void effectsLeftOutOfTheTableAreExcluded() {
        for (String name : SkillEffectNames.all()) {
            if (!ModuleShares.isClassified(SkillEffect.byName(name))) {
                assertEquals(ModuleShare.EXCLUDE, SkillEffect.byName(name).moduleShare(), name);
            }
        }
    }
}
