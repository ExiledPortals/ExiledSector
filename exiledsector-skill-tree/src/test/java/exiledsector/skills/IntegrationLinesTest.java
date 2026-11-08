package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.defense.DefenseSkillEffect;
import exiledsector.skills.skilleffect.flux.FluxSkillEffect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationLinesTest {

    private static final String REMOVED_PREFIX = "No longer: ";
    private static final IntegrationLines.State NOT_INTEGRATED = new IntegrationLines.State(false, 1, true);

    private static final SkillType PENALTY_DROPPING = new SkillType.Builder("bulkheads", "Bulkheads", "a.png", SkillTier.NOTABLE)
            .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f), new SkillTypeEffect(FluxSkillEffect.FLUX_CAPACITY_MULT, -10f)))
            .integratedEffects(new IntegratedEffects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 20f)), List.of()))
            .build();

    private static final SkillType FRIGATE_PENALTY_DROPPING = new SkillType.Builder("plating", "Plating", "a.png", SkillTier.NOTABLE)
            .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
            .hullSizeEffects(List.of(new HullSizeSkillEffect(DefenseSkillEffect.WEAPON_DAMAGE_TAKEN_PERCENT, 50f, 0f, 0f, 0f)))
            .integratedEffects(new IntegratedEffects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)), List.of()))
            .build();

    private static List<String> removedTexts(List<DescriptionLine> lines) {
        return lines.stream().map(DescriptionLine::plain).filter(plain -> plain.startsWith(REMOVED_PREFIX)).toList();
    }

    private static boolean isAllStoryPointGreen(StyledText text) {
        return text.spans().stream().allMatch(span -> span.style() == Style.BUILT_IN)
                && text.spans().stream().mapToInt(span -> span.end() - span.start()).sum() == text.plain().length();
    }

    @Test
    void anIntegratedTooltipListsEachDroppedLineInStoryPointGreen() {
        List<DescriptionLine> effects = IntegrationLines.integrated(PENALTY_DROPPING, HullSize.CRUISER).effects();

        String droppedPenalty = REMOVED_PREFIX + FluxSkillEffect.FLUX_CAPACITY_MULT.description(-10f).plain();
        assertEquals(List.of(droppedPenalty), removedTexts(effects));
        DescriptionLine removedLine = effects.stream().filter(line -> line.plain().equals(droppedPenalty)).findFirst().orElseThrow();
        assertTrue(isAllStoryPointGreen(removedLine.text()), removedLine.text().toMarkup());
        assertFalse(removedLine.lowerIsBetter());
    }

    @Test
    void integratedEffectTextShowsOnlyOnTheIntegratedNodeInStoryPointGreen() {
        String jokeLine = "The crabs eat what the crew won't.";
        SkillType crabs = new SkillType.Builder("crabs", "Crabs", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .integratedEffects(new IntegratedEffects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)), List.of(), List.of(jokeLine)))
                .build();

        List<DescriptionLine> normal = IntegrationLines.describe(crabs, HullSize.CRUISER, NOT_INTEGRATED).effects();
        List<DescriptionLine> integrated = IntegrationLines.integrated(crabs, HullSize.CRUISER).effects();

        assertTrue(normal.stream().noneMatch(line -> line.plain().equals(jokeLine)), normal.toString());
        DescriptionLine integratedJoke = integrated.stream().filter(line -> line.plain().equals(jokeLine)).findFirst().orElseThrow();
        assertTrue(isAllStoryPointGreen(integratedJoke.text()), integratedJoke.text().toMarkup());
        assertEquals(List.of(), removedTexts(integrated));
    }

    @Test
    void aLineWhoseValueChangedIsNotListedAsDropped() {
        List<DescriptionLine> effects = IntegrationLines.integrated(PENALTY_DROPPING, HullSize.CRUISER).effects();

        String changedHull = DefenseSkillEffect.HULL_PERCENT.description(10f).plain();
        assertFalse(removedTexts(effects).contains(REMOVED_PREFIX + changedHull), effects.toString());
    }

    @Test
    void aLineReplacedByANewlyWordedOneIsNotListedAsDropped() {
        SkillType reworded = new SkillType.Builder("reworded", "Reworded", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .integratedEffects(new IntegratedEffects(List.of(new SkillTypeEffect(DefenseSkillEffect.ARMOR_FLAT, 50f)), List.of()))
                .build();

        assertEquals(List.of(), removedTexts(IntegrationLines.integrated(reworded, HullSize.CRUISER).effects()));
    }

    @Test
    void droppedLinesBeyondTheNewlyWordedOnesAreStillListed() {
        SkillType rewordedAndDropped = new SkillType.Builder("rewordedAndDropped", "Reworded", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f), new SkillTypeEffect(FluxSkillEffect.FLUX_CAPACITY_MULT, -10f)))
                .integratedEffects(new IntegratedEffects(List.of(new SkillTypeEffect(DefenseSkillEffect.ARMOR_FLAT, 50f)), List.of()))
                .build();

        assertEquals(List.of(REMOVED_PREFIX + FluxSkillEffect.FLUX_CAPACITY_MULT.description(-10f).plain()),
                removedTexts(IntegrationLines.integrated(rewordedAndDropped, HullSize.CRUISER).effects()));
    }

    @Test
    void aPenaltyOnlyOneHullSizeHasIsListedAsDroppedOnlyForThatHullSize() {
        String droppedPenalty = REMOVED_PREFIX + DefenseSkillEffect.WEAPON_DAMAGE_TAKEN_PERCENT.description(50f).plain();

        assertEquals(List.of(droppedPenalty), removedTexts(IntegrationLines.integrated(FRIGATE_PENALTY_DROPPING, HullSize.FRIGATE).effects()));
        assertEquals(List.of(), removedTexts(IntegrationLines.integrated(FRIGATE_PENALTY_DROPPING, HullSize.CRUISER).effects()));
    }

    @Test
    void thePreviewOfANotYetIntegratedNodeListsTheDroppedLinesToo() {
        List<DescriptionLine> preview = IntegrationLines.preview(FRIGATE_PENALTY_DROPPING, HullSize.FRIGATE, NOT_INTEGRATED);

        String droppedPenalty = REMOVED_PREFIX + DefenseSkillEffect.WEAPON_DAMAGE_TAKEN_PERCENT.description(50f).plain();
        assertEquals(List.of(droppedPenalty), removedTexts(preview));
        assertTrue(isAllStoryPointGreen(preview.get(preview.size() - 1).text()));
        assertEquals(List.of(), removedTexts(IntegrationLines.preview(FRIGATE_PENALTY_DROPPING, HullSize.DESTROYER, NOT_INTEGRATED)));
    }
}
