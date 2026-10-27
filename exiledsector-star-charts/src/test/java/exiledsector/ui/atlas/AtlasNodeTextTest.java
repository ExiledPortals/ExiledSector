package exiledsector.ui.atlas;

import exiledlib.i18n.Style;
import exiledlib.i18n.StyledText;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.relay.RelayChain;
import exiledsector.charts.anomaly.relay.RelayNetwork;
import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.skirmish.FactionSkirmish;
import exiledsector.charts.anomaly.skirmish.SkirmishEntry;
import exiledsector.charts.anomaly.skirmish.SkirmishSide;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasDevTools;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasMarkerRule;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRules;
import exiledsector.charts.atlas.AtlasSpecial;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.ui.socket.BenchModeSupport;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasNodeTextTest {

    private static final long SEED = 0xa71a5L;

    private static AtlasGenerator generator(AtlasState state) {
        List<AtlasRegion> regions = List.of(new AtlasRegion(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of()));
        List<AtlasSpecial> specials = List.of(
                new AtlasSpecial("bastion", AtlasSpecial.Kind.BASTION, AtlasSpecial.Placement.FIXED, 0f, 1f, 0f, 3, 25f, "", "", ""));
        AtlasRules rules = new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 1f), AtlasMarkerRule.NEVER, AtlasMarkerRule.NEVER,
                AtlasMarkerRule.NEVER, specials, List.of());
        return new AtlasGenerator(SEED, rules, AtlasUnlocks.NONE, state);
    }

    @Test
    void aReachableBastionNamesItsChartRequirementBeforeInvitingAClick() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell bastion = AtlasDevTools.nearestOpenSite(generator, state, "bastion");

        assertEquals(List.of(new AtlasNodeText.Line("Out of reach. Complete a system joined to it by a lane first.", AtlasNodeText.Tone.HINT)),
                AtlasNodeText.stateLines(generator, state, bastion, false));

        AtlasDevTools.reach(generator, state, bastion);

        assertEquals(List.of(new AtlasNodeText.Line("A Bastion needs a tier 15 chart.", AtlasNodeText.Tone.MARKER),
                        new AtlasNodeText.Line("Click to pick a Star Chart for this system", AtlasNodeText.Tone.GOOD)),
                AtlasNodeText.stateLines(generator, state, bastion, true));

        state.add(bastion, AtlasState.COMPLETED);

        assertEquals(List.of(new AtlasNodeText.Line("Completed", AtlasNodeText.Tone.GOOD)), AtlasNodeText.stateLines(generator, state, bastion, false));
    }

    @Test
    void aSkirmishSystemNamesBothSidesAndOffersEachSideToJoin() {
        ChartTables.useAnomalies(List.of(FactionSkirmish.INSTANCE),
                List.of(new ChartAnomalies.Definition(FactionSkirmish.ID, 1f, 1, 15, "", "", true, Set.of(), false)));
        try {
            AtlasState state = new AtlasState(SEED);
            List<AtlasRegion> regions = List.of(new AtlasRegion(AtlasRegion.NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of()));
            AtlasGenerator generator = new AtlasGenerator(SEED, new AtlasRules(regions, Map.of("compact", 1f), Map.of("hegemony", 1f, "tritachyon", 1f),
                    AtlasMarkerRule.NEVER, AtlasMarkerRule.NEVER, AtlasMarkerRule.NEVER, List.of(), List.of()), AtlasUnlocks.NONE, state);
            AtlasCell cell = AtlasCell.ORIGIN.around(20).stream()
                    .filter(candidate -> AtlasAnomalies.insert(generator, state, candidate, FactionSkirmish.ID, AtlasAnomalyRecord.Source.WORLD, null)
                            .check().allows())
                    .findFirst().orElseThrow();
            String defender = AtlasNodeText.tidy(FactionSkirmish.defender(generator, state, cell));
            String attacker = AtlasNodeText.tidy(FactionSkirmish.attacker(generator, state, cell));

            assertTrue(AtlasNodeText.markers(generator, state, cell, generator.node(cell))
                    .contains(new AtlasNodeText.Line(attacker + " attacks " + defender, AtlasNodeText.Tone.PLAIN)));
            assertTrue(AtlasNodeText.confirmLines(generator, state, cell).contains("Completing the system ends the fight for good."));
            List<BenchModeSupport.OpenChoice> choices = AtlasNodeText.sideChoices(generator, state, cell);
            assertEquals(List.of("Side with " + defender, "Side with " + attacker, "Stay out"),
                    choices.stream().map(BenchModeSupport.OpenChoice::label).toList());
            assertEquals(List.of(new SkirmishEntry(cell, SkirmishSide.DEFENDER), new SkirmishEntry(cell, SkirmishSide.ATTACKER),
                    new SkirmishEntry(cell, SkirmishSide.NEITHER)), choices.stream().map(BenchModeSupport.OpenChoice::target).toList());
            assertEquals(List.of(), AtlasNodeText.sideChoices(generator, state, AtlasCell.ORIGIN));
        } finally {
            ChartTables.resetAnomalies();
        }
    }

    @Test
    void tooltipTonesBecomeTheSkillTreeHighlightStyles() {
        assertEquals(StyledText.styled("x", Style.HIGHLIGHT), AtlasScreen.styled(new AtlasNodeText.Line("x", AtlasNodeText.Tone.MARKER)));
        assertEquals(StyledText.styled("x", Style.GOOD), AtlasScreen.styled(new AtlasNodeText.Line("x", AtlasNodeText.Tone.GOOD)));
        assertEquals(StyledText.styled("x", Style.BAD), AtlasScreen.styled(new AtlasNodeText.Line("x", AtlasNodeText.Tone.BAD)));
        assertEquals(StyledText.styled("x", Style.MUTED), AtlasScreen.styled(new AtlasNodeText.Line("x", AtlasNodeText.Tone.HINT)));
        assertEquals(StyledText.of("x"), AtlasScreen.styled(new AtlasNodeText.Line("x", AtlasNodeText.Tone.PLAIN)));
    }

    @Test
    void aRelayChainSystemShowsItsStageItsLinesAndTheLastStagesUnique() throws Exception {
        ChartTables.loadAnomalies();
        AtlasData.registerNodeEffects(new JSONArray().put(new JSONObject().put("id", RelayChain.STAGE_EFFECTS_ID)
                .put("effects", "CHART_FLEET_SIZE_PERCENT:20; CHART_QUANTITY_PERCENT:25")));
        try {
            AtlasState state = new AtlasState(SEED);
            AtlasGenerator generator = generator(state);
            AtlasCell cell = generator.lanes(AtlasCell.ORIGIN).get(0);
            AtlasAnomalies.insert(generator, state, cell, RelayNetwork.ID, AtlasAnomalyRecord.Source.WORLD, null,
                    new AtlasAnomalyRecord.Chain(3, 3, AtlasCell.ORIGIN));

            List<AtlasNodeText.Line> markers = AtlasNodeText.markers(generator, state, cell, generator.node(cell));

            assertTrue(markers.contains(new AtlasNodeText.Line("Stage 3 of 3", AtlasNodeText.Tone.PLAIN)), markers.toString());
            assertTrue(markers.contains(new AtlasNodeText.Line("The Star Chart Boss always drops a unique hull item or subroutine",
                    AtlasNodeText.Tone.GOOD)), markers.toString());
            List<String> styledLines = markers.stream().filter(line -> line.tone() == AtlasNodeText.Tone.STYLED).map(AtlasNodeText::plainText).toList();
            assertEquals(List.of("40% increased enemy fleet size",
                    "50% increased quantity of items found in the system, and 50% increased completion reward"), styledLines);
            assertTrue(AtlasNodeText.confirmLines(generator, state, cell).stream().noneMatch(line -> line.contains("<")));
            assertTrue(AtlasNodeText.anomalyBody(generator, state, cell, RelayNetwork.ID).plain().contains("Stage 3 of 3"));
            assertEquals(StyledText.parse("<bad>40%</bad> x"), AtlasScreen.styled(new AtlasNodeText.Line("<bad>40%</bad> x", AtlasNodeText.Tone.STYLED)));
        } finally {
            AtlasData.registerNodeEffects(new JSONArray());
            ChartTables.resetAnomalies();
        }
    }
}
