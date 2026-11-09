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

    private static final String REMOVED_PREFIX = "No longer grants ";
    private static final IntegrationLines.State NOT_INTEGRATED = new IntegrationLines.State(false, 1, true, IntegrationLines.TemplateMark.NONE);

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

    private static List<String> storyPointGreenParts(StyledText text) {
        return text.spans().stream().filter(span -> span.style() == Style.BUILT_IN).map(span -> text.plain().substring(span.start(), span.end())).toList();
    }

    @Test
    void anIntegratedTooltipListsEachDroppedLineInStoryPointGreen() {
        List<DescriptionLine> effects = IntegrationLines.integrated(PENALTY_DROPPING, HullSize.CRUISER).effects();

        String droppedPenalty = REMOVED_PREFIX + FluxSkillEffect.FLUX_CAPACITY_MULT.description(-10f).plain();
        assertEquals(List.of(droppedPenalty), removedTexts(effects));
        DescriptionLine removedLine = effects.stream().filter(line -> line.plain().equals(droppedPenalty)).findFirst().orElseThrow();
        assertEquals(List.of("10%"), storyPointGreenParts(removedLine.text()), removedLine.text().toMarkup());
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
    void aTemplateMarkAddsItsLineToANodeNotYetIntegrated() {
        IntegrationLines.State marked = new IntegrationLines.State(false, 1, false, IntegrationLines.TemplateMark.MARKED);
        IntegrationLines.State integratedAndMarked = new IntegrationLines.State(true, 1, false, IntegrationLines.TemplateMark.MARKED);

        List<String> markedLines = IntegrationLines.describe(PENALTY_DROPPING, HullSize.CRUISER, marked).effects().stream()
                .map(DescriptionLine::plain).toList();
        List<String> integratedLines = IntegrationLines.describe(PENALTY_DROPPING, HullSize.CRUISER, integratedAndMarked).effects().stream()
                .map(DescriptionLine::plain).toList();

        assertTrue(markedLines.contains("The template marks this node for integration."), markedLines.toString());
        assertFalse(integratedLines.contains("The template marks this node for integration."), integratedLines.toString());
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
        assertEquals(List.of("50%"), storyPointGreenParts(preview.get(preview.size() - 1).text()));
        assertEquals(List.of(), removedTexts(IntegrationLines.preview(FRIGATE_PENALTY_DROPPING, HullSize.DESTROYER, NOT_INTEGRATED)));
    }

    @Test
    void anIntegratedTooltipShowsOnlyTheNewValueOfAChangedLineInStoryPointGreen() {
        String changedHull = DefenseSkillEffect.HULL_PERCENT.description(20f).plain();
        DescriptionLine hullLine = IntegrationLines.integrated(PENALTY_DROPPING, HullSize.CRUISER).effects().stream()
                .filter(line -> line.plain().equals(changedHull)).findFirst().orElseThrow();

        List<String> greenParts = storyPointGreenParts(hullLine.text());
        assertEquals(1, greenParts.size(), hullLine.text().toMarkup());
        assertTrue(greenParts.get(0).contains("20"), hullLine.text().toMarkup());
    }

    @Test
    void thePreviewSaysInsteadGrantsForAChangedValueAndSkipsUnchangedLines() {
        SkillType raised = new SkillType.Builder("raised", "Raised", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f), new SkillTypeEffect(DefenseSkillEffect.ARMOR_FLAT, 50f)))
                .integratedEffects(new IntegratedEffects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 20f),
                        new SkillTypeEffect(DefenseSkillEffect.ARMOR_FLAT, 50f)), List.of()))
                .build();

        List<DescriptionLine> preview = IntegrationLines.preview(raised, HullSize.CRUISER, NOT_INTEGRATED);

        assertEquals(2, preview.size(), preview.toString());
        DescriptionLine instead = preview.get(1);
        assertEquals("Instead grants " + DefenseSkillEffect.HULL_PERCENT.description(20f).plain(), instead.plain());
        List<String> greenParts = storyPointGreenParts(instead.text());
        assertEquals(1, greenParts.size(), instead.text().toMarkup());
        assertTrue(greenParts.get(0).contains("20"), instead.text().toMarkup());
    }

    @Test
    void thePreviewSaysInAdditionGrantsForANewLineInStoryPointGreen() {
        SkillType extended = new SkillType.Builder("extended", "Extended", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .integratedEffects(new IntegratedEffects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f),
                        new SkillTypeEffect(DefenseSkillEffect.ARMOR_FLAT, 50f)), List.of()))
                .build();

        List<DescriptionLine> preview = IntegrationLines.preview(extended, HullSize.CRUISER, NOT_INTEGRATED);

        String leadIn = "In addition, grants ";
        assertEquals(2, preview.size(), preview.toString());
        assertEquals(leadIn + DefenseSkillEffect.ARMOR_FLAT.description(50f).plain(), preview.get(1).plain());
        List<String> greenParts = storyPointGreenParts(preview.get(1).text());
        assertEquals(1, greenParts.size(), preview.get(1).text().toMarkup());
        assertTrue(greenParts.get(0).contains("50"), preview.get(1).text().toMarkup());
    }

    @Test
    void aSentenceLineInThePreviewFollowsAColon() {
        String jokeLine = "The crabs eat what the crew won't.";
        SkillType crabs = new SkillType.Builder("crabs", "Crabs", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .integratedEffects(new IntegratedEffects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)), List.of(), List.of(jokeLine)))
                .build();

        List<String> preview = IntegrationLines.preview(crabs, HullSize.CRUISER, NOT_INTEGRATED).stream().map(DescriptionLine::plain).toList();

        assertEquals("In addition, grants: " + jokeLine, preview.get(preview.size() - 1));
    }

    @Test
    void aLineWithoutHighlightedValuesTurnsWhollyStoryPointGreenWhenIntegrated() {
        String jokeLine = "The crabs eat what the crew won't.";
        SkillType crabs = new SkillType.Builder("crabs", "Crabs", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .integratedEffects(new IntegratedEffects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)), List.of(), List.of(jokeLine)))
                .build();

        DescriptionLine preview = IntegrationLines.preview(crabs, HullSize.CRUISER, NOT_INTEGRATED).get(1);

        assertEquals(List.of(jokeLine), storyPointGreenParts(preview.text()), preview.text().toMarkup());
    }

    @Test
    void aSecondCopyOfAnUnchangedLineIsAnAddition() {
        SkillType stacked = new SkillType.Builder("stacked", "Stacked", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .integratedEffects(new IntegratedEffects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f),
                        new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 20f)), List.of()))
                .build();

        List<String> preview = IntegrationLines.preview(stacked, HullSize.CRUISER, NOT_INTEGRATED).stream().map(DescriptionLine::plain).toList();

        assertEquals(List.of("In addition, grants " + DefenseSkillEffect.HULL_PERCENT.description(20f).plain()), preview.subList(1, preview.size()));
    }

    @Test
    void aChangedPercentageIsMarkedTogetherWithItsPercentSign() {
        SkillType capped = new SkillType.Builder("capped", "Capped", "a.png", SkillTier.NOTABLE)
                .effectText(List.of("Repairs hull, up to a maximum of 20% per deployment."))
                .integratedEffects(new IntegratedEffects(List.of(), List.of(), List.of("Repairs hull, up to a maximum of 30% per deployment.")))
                .build();

        DescriptionLine integratedLine = IntegrationLines.integrated(capped, HullSize.CRUISER).effects().get(1);

        assertEquals(List.of("30%"), storyPointGreenParts(integratedLine.text()), integratedLine.text().toMarkup());
    }

    @Test
    void aReplacingLineTurnsOnlyItsValueGreenAndKeepsItsOtherNumbersHighlighted() {
        SkillType striking = new SkillType.Builder("striking", "Striking", "a.png", SkillTier.NOTABLE)
                .effectText(List.of("Strikes enemies within <hl>400</hl> su."))
                .integratedEffects(new IntegratedEffects(List.of(), List.of(), List.of("Burns enemies within <hl>400</hl> su for <good>5%</good> of their armor.")))
                .build();

        DescriptionLine replacing = IntegrationLines.integrated(striking, HullSize.CRUISER).effects().get(1);

        assertEquals(List.of("5%"), storyPointGreenParts(replacing.text()), replacing.text().toMarkup());
        assertTrue(replacing.text().spans().stream().anyMatch(span -> span.style() == Style.HIGHLIGHT), replacing.text().toMarkup());
    }

    @Test
    void aDroppedSentenceKeepsTheColonAfterNoLongerGrants() {
        SkillType sentenceDropping = new SkillType.Builder("ghost", "Ghost", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .effectText(List.of("Ignores asteroids.", "This ship can't retreat."))
                .integratedEffects(new IntegratedEffects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)), List.of(), List.of("Ignores asteroids.")))
                .build();

        List<String> preview = IntegrationLines.preview(sentenceDropping, HullSize.CRUISER, NOT_INTEGRATED).stream().map(DescriptionLine::plain).toList();

        assertEquals(List.of("No longer grants: This ship can't retreat."), preview.subList(1, preview.size()));
    }
}
