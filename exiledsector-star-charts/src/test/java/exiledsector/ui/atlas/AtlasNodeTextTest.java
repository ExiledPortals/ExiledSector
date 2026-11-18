package exiledsector.ui.atlas;

import exiledsector.i18n.StyledText;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.StellarFlares;
import exiledsector.charts.anomaly.relay.RelayStages;
import exiledsector.charts.anomaly.relay.RelayNetwork;
import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.skirmish.FactionSkirmish;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasDevTools;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasAnomalyChance;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRules;
import exiledsector.charts.atlas.AtlasSpecial;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.charts.atlas.TestBorders;
import exiledsector.charts.atlas.TestBorders.Border;
import exiledsector.charts.officer.OfficerText;
import exiledsector.charts.officer.turn.Intent;
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
        AtlasRules rules = new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 1f), AtlasAnomalyChance.NEVER, AtlasAnomalyChance.NEVER,
                specials, List.of());
        return new AtlasGenerator(SEED, rules, AtlasUnlocks.NONE, state);
    }

    @Test
    void aReachableBastionNamesItsChartRequirementBeforeInvitingAClick() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell bastion = AtlasDevTools.nearestOpenSite(generator, state, "bastion");

        assertEquals(List.of(), AtlasNodeText.stateLines(generator, state, bastion, false));

        AtlasDevTools.reach(generator, state, bastion);

        assertEquals(List.of(new AtlasLine("A Bastion needs a tier 15 chart.", AtlasLine.Tone.MARKER),
                        new AtlasLine("Click to pick a Star Chart for this system", AtlasLine.Tone.GOOD)),
                AtlasNodeText.stateLines(generator, state, bastion, true));

        state.add(bastion, AtlasState.COMPLETED);

        assertEquals(List.of(new AtlasLine("Completed", AtlasLine.Tone.GOOD)), AtlasNodeText.stateLines(generator, state, bastion, false));
    }

    @Test
    void aSkirmishSystemNamesBothOfficersAndOffersNoSideToJoin() {
        ChartTables.useAnomalies(List.of(FactionSkirmish.INSTANCE),
                List.of(new ChartAnomalies.Definition(FactionSkirmish.ANOMALY_ID, 1f, 1, 15, "", "", true, Set.of(), false)));
        try {
            AtlasState state = new AtlasState(TestBorders.SEED);
            AtlasGenerator generator = TestBorders.generator(state);
            Border border = TestBorders.border(generator, state, "tritachyon", "hegemony");
            AtlasCell cell = border.contested();
            state.setDeclaredIntents(List.of(Intent.push(border.attacker().recordId(), cell, border.defender().recordId()).encode()));
            assertTrue(AtlasAnomalies.insert(generator, state, cell, FactionSkirmish.ANOMALY_ID, AtlasAnomalyRecord.Source.WORLD, null).check().allows());
            String attackerFaction = ChartFactions.displayName(border.attacker().factionId());
            String defenderFaction = ChartFactions.displayName(border.defender().factionId());

            assertTrue(AtlasNodeText.markers(generator, state, cell, generator.node(cell)).contains(new AtlasLine(OfficerText.title(border.attacker()) + " of "
                    + attackerFaction + " attacks " + OfficerText.title(border.defender()) + " of " + defenderFaction, AtlasLine.Tone.PLAIN)));
            assertTrue(AtlasNodeText.confirmLines(generator, state, cell, itemId -> 0).contains("Completing the system ends the fight permanently."));
            assertEquals(List.of(), AtlasNodeText.entryChoices(generator, state, cell));
            assertEquals(List.of(), AtlasNodeText.entryChoices(generator, state, AtlasCell.ORIGIN));
        } finally {
            ChartTables.resetAnomalies();
        }
    }

    @Test
    void anAnomalysRewardLineFollowsItsDescription() throws Exception {
        ChartTables.loadAnomalies();
        try {
            String flares = AtlasNodeText.anomalyBody(StellarFlares.ANOMALY_ID).plain();

            assertTrue(flares.startsWith("The system's stars throw flares constantly."), flares);
            assertTrue(flares.endsWith("\nHull items found in the system have 1 lucky modifier\n(Lucky modifiers are rolled twice, keeping the best)"), flares);
        } finally {
            ChartTables.resetAnomalies();
        }
    }

    @Test
    void aRelayStageSystemShowsItsStageAndItsLines() throws Exception {
        ChartTables.loadAnomalies();
        AtlasData.registerNodeEffects(new JSONArray().put(new JSONObject().put("id", RelayStages.STAGE_EFFECTS_ID)
                .put("effects", "CHART_FLEET_SIZE_PERCENT:20; CHART_QUANTITY_PERCENT:25")));
        try {
            AtlasState state = new AtlasState(SEED);
            AtlasGenerator generator = generator(state);
            AtlasCell cell = generator.lanes(AtlasCell.ORIGIN).get(0);
            AtlasAnomalies.insert(generator, state, cell, RelayNetwork.ANOMALY_ID, AtlasAnomalyRecord.Source.WORLD, null,
                    new AtlasAnomalyRecord.Chain(3, 3, AtlasCell.ORIGIN));

            List<AtlasLine> markers = AtlasNodeText.markers(generator, state, cell, generator.node(cell));

            assertTrue(markers.contains(new AtlasLine("Stage 3 of 3", AtlasLine.Tone.PLAIN)), markers.toString());
            assertTrue(markers.stream().noneMatch(line -> line.plain().contains("unique")), markers.toString());
            List<String> styledLines = markers.stream().filter(line -> line.tone() == AtlasLine.Tone.STYLED).map(AtlasLine::plain).toList();
            assertEquals(List.of("40% increased enemy fleet size",
                    "50% increased quantity of items found in the system"), styledLines);
            assertTrue(AtlasNodeText.confirmLines(generator, state, cell, itemId -> 0).stream().noneMatch(line -> line.contains("<")));
            assertTrue(AtlasNodeText.anomalyBody(generator, state, cell, RelayNetwork.ANOMALY_ID).plain().contains("Stage 3 of 3"));
            assertEquals(List.of(), AtlasNodeText.card(generator, state, cell, true).notes());
            assertEquals(StyledText.parse("<bad>40%</bad> x"), new AtlasLine("<bad>40%</bad> x", AtlasLine.Tone.STYLED).styled());
        } finally {
            AtlasData.registerNodeEffects(new JSONArray());
            ChartTables.resetAnomalies();
        }
    }
}
