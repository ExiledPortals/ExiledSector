package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import exiledsector.charts.ChartGenContext.AppliedEffect;
import exiledsector.charts.ChartGenContext;
import exiledsector.charts.content.ChartPoi;
import exiledsector.charts.ChartTables;
import exiledsector.charts.ChartTiers;
import exiledsector.charts.effect.ChartEffect;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartLootTest {

    private static final Set<String> KNOWN_GROUPS = Set.of("weapons1", "weapons2", "basic", "goods", "ai_cores3", "rare_tech", "blueprints_low");

    @AfterEach
    void tearDown() {
        ChartLoot.clear();
    }

    private static JSONObject row(String id, String dropType, String scope, String group, String mode, String amount, String minTier) throws Exception {
        return new JSONObject().put("id", id).put("drop_type", dropType).put("scope", scope).put("group", group).put("mode", mode)
                .put("amount", amount).put("min_tier", minTier);
    }

    private static JSONObject mapping(String dropType, String group) throws Exception {
        return row(group, dropType, "", group, "", "", "");
    }

    private static void registerTable() throws Exception {
        ChartLoot.register(new JSONArray().put(mapping("weapon", "weapons1")).put(mapping("weapon", "weapons2"))
                .put(mapping("resource", "basic")).put(mapping("resource", "goods")).put(mapping("ai_core", "ai_cores3"))
                .put(row("derelict_weapons1", "weapon", "derelict", "weapons1", "random", "2", "1"))
                .put(row("derelict_basic", "resource", "derelict", "basic", "value", "1500", "6"))
                .put(row("fleet_weapons1", "weapon", "fleet", "weapons1", "random", "1", "1"))
                .put(row("boss_ai_cores3", "ai_core", "boss", "ai_cores3", "random", "2", "1")), KNOWN_GROUPS::contains);
    }

    private static DropData drop(String group, int chances, int value) {
        DropData drop = new DropData();
        drop.group = group;
        drop.chances = chances;
        drop.value = value;
        return drop;
    }

    private static ChartGenContext context(ChartEffect effect, float value) {
        return ChartGenContext.of(List.of(new AppliedEffect(effect, value)));
    }

    private static List<DropData> randomDropsAddedTo(SectorEntityToken holder) {
        ArgumentCaptor<DropData> added = ArgumentCaptor.forClass(DropData.class);
        verify(holder, org.mockito.Mockito.atLeast(0)).addDropRandom(added.capture());
        return added.getAllValues();
    }

    private static List<DropData> valueDropsAddedTo(SectorEntityToken holder) {
        ArgumentCaptor<DropData> added = ArgumentCaptor.forClass(DropData.class);
        verify(holder, org.mockito.Mockito.atLeast(0)).addDropValue(added.capture());
        return added.getAllValues();
    }

    @Test
    void theShippedTableParsesAndEveryBaseEntryUsesAGroupOfItsOwnDropType() throws Exception {
        List<String> problems = ChartLoot.register(ChartTables.csvRows(ChartLoot.PATH), group -> true);

        assertEquals(List.of(), problems);
        for (DropType type : DropType.values()) {
            if (type.isVanilla()) {
                assertTrue(ChartLoot.groupTypes().containsValue(type), type.name());
            }
        }
        for (String scope : ChartLoot.SCOPES) {
            for (ChartLoot.Entry entry : ChartLoot.entriesFor(scope, ChartTiers.HIGHEST_TIER)) {
                assertEquals(entry.type(), ChartLoot.typeOfGroup(entry.group()), entry.toString());
            }
        }
        assertTrue(ChartLoot.entriesFor(ChartLoot.BOSS_SCOPE, ChartTiers.HIGHEST_TIER).size() >= 4);
        assertNull(ChartLoot.typeOfGroup("rare_tech"), "colony item groups are never mapped, so no bonus scales them");
    }

    @Test
    void badRowsAreSkippedWithAProblem() throws Exception {
        List<String> problems = ChartLoot.register(new JSONArray()
                .put(mapping("currency", "weapons1"))
                .put(mapping("weapon", "modded_group"))
                .put(mapping("weapon", "goods"))
                .put(mapping("resource", "goods"))
                .put(row("bad_scope", "weapon", "vault", "weapons1", "random", "1", "1"))
                .put(row("bad_mode", "weapon", "fleet", "weapons1", "lots", "1", "1"))
                .put(row("no_amount", "weapon", "fleet", "weapons1", "random", "0", "1"))
                .put(row("kept", "weapon", "fleet", "weapons2", "random", "1", "1"))
                .put(mapping("resource", "rare_tech")), KNOWN_GROUPS::contains);

        assertEquals(7, problems.size(), problems.toString());
        assertEquals(Map.of("goods", DropType.WEAPON), ChartLoot.groupTypes());
        assertEquals(1, ChartLoot.entriesFor(ChartLoot.FLEET_SCOPE, 1).size());
    }

    @Test
    void aSiteGetsExtraChancesOnItsOwnMappedSpecDropsOnly() throws Exception {
        registerTable();
        DropData weapons = drop("weapons2", 10, -1);
        weapons.maxChances = 15;
        DropData goods = drop("goods", 10, 5000);
        DropData colonyItems = drop("rare_tech", 1, -1);
        DropData basicValue = drop("basic", -1, 20000);
        ChartVanillaLoot.SiteDrops specDrops = new ChartVanillaLoot.SiteDrops(List.of(weapons, goods, colonyItems), List.of(basicValue));
        SectorEntityToken site = mock(SectorEntityToken.class);

        ChartVanillaLoot.addToSite(site, ChartPoi.Kind.CACHE, specDrops,
                ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_WEAPON_PERCENT, 30f), new AppliedEffect(ChartEffect.CHART_QUANTITY_PERCENT, 10f))), 5,
                new Random(1L));

        List<DropData> randomCopies = randomDropsAddedTo(site);
        assertEquals(2, randomCopies.size());
        assertEquals("weapons2", randomCopies.get(0).group);
        assertEquals(4, randomCopies.get(0).chances);
        assertEquals(6, randomCopies.get(0).maxChances);
        assertEquals(1f, randomCopies.get(0).valueMult, "extra chances, not valueMult, so a debris field's own valueMult keeps the bonus in proportion");
        assertEquals("goods", randomCopies.get(1).group);
        assertEquals(1, randomCopies.get(1).chances);
        assertEquals(5000, randomCopies.get(1).value);
        List<DropData> valueCopies = valueDropsAddedTo(site);
        assertEquals(1, valueCopies.size());
        assertEquals(20000, valueCopies.get(0).value);
        assertEquals(0.1f, valueCopies.get(0).valueMult, 1e-5f);
        assertEquals(1f, weapons.valueMult, "the spec's own entry is never changed");
    }

    @Test
    void noBonusAddsNoCopies() throws Exception {
        registerTable();
        SectorEntityToken site = mock(SectorEntityToken.class);

        ChartVanillaLoot.addToSite(site, ChartPoi.Kind.STATION, new ChartVanillaLoot.SiteDrops(List.of(drop("weapons2", 10, -1)), List.of()),
                ChartGenContext.none(), 15, new Random(1L));

        verify(site, never()).addDropRandom(any(DropData.class));
        verify(site, never()).addDropValue(any(DropData.class));
    }

    @Test
    void baseEntriesGoOnlyToTheirCategoryFromTheirMinTierScaledByTheBonus() throws Exception {
        registerTable();
        SectorEntityToken lowTierWreck = mock(SectorEntityToken.class);
        SectorEntityToken highTierWreck = mock(SectorEntityToken.class);
        SectorEntityToken cache = mock(SectorEntityToken.class);
        ChartGenContext moreWeapons = context(ChartEffect.CHART_WEAPON_PERCENT, 50f);

        ChartVanillaLoot.addToSite(lowTierWreck, ChartPoi.Kind.DERELICT, ChartVanillaLoot.SiteDrops.NONE, moreWeapons, 3, new Random(1L));
        ChartVanillaLoot.addToSite(highTierWreck, ChartPoi.Kind.DERELICT, ChartVanillaLoot.SiteDrops.NONE, moreWeapons, 6, new Random(1L));
        ChartVanillaLoot.addToSite(cache, ChartPoi.Kind.CACHE, ChartVanillaLoot.SiteDrops.NONE, moreWeapons, 6, new Random(1L));

        List<DropData> lowRandom = randomDropsAddedTo(lowTierWreck);
        assertEquals(1, lowRandom.size());
        assertEquals("weapons1", lowRandom.get(0).group);
        assertEquals(3, lowRandom.get(0).chances);
        assertEquals(1f, lowRandom.get(0).valueMult, 1e-5f);
        assertEquals(List.of(), valueDropsAddedTo(lowTierWreck));
        List<DropData> highValue = valueDropsAddedTo(highTierWreck);
        assertEquals(1, highValue.size());
        assertEquals(1500, highValue.get(0).value);
        assertEquals(1f, highValue.get(0).valueMult, 1e-5f);
        verify(cache, never()).addDropRandom(any(DropData.class));
    }

    @Test
    void fleetEntriesScaleWithFleetPointsAndOnlyTheBossGetsSpoilsScaledByTheHoard() throws Exception {
        registerTable();
        CampaignFleetAPI guard = mock(CampaignFleetAPI.class);
        when(guard.getFleetPoints()).thenReturn(250);
        CampaignFleetAPI boss = mock(CampaignFleetAPI.class);
        when(boss.getFleetPoints()).thenReturn(400);
        ChartGenContext hoard = context(ChartEffect.CHART_BOSS_SPOILS_PERCENT, 50f);

        ChartVanillaLoot.addToFleet(guard, false, hoard, 8);
        ChartVanillaLoot.addToFleet(boss, true, hoard, 8);

        List<DropData> guardDrops = randomDropsAddedTo(guard);
        assertEquals(1, guardDrops.size());
        assertEquals("weapons1", guardDrops.get(0).group);
        assertEquals(2.5f, guardDrops.get(0).valueMult, 1e-5f);
        List<DropData> bossDrops = randomDropsAddedTo(boss);
        assertEquals(2, bossDrops.size());
        assertEquals(4f, bossDrops.get(0).valueMult, 1e-5f);
        assertEquals("ai_cores3", bossDrops.get(1).group);
        assertEquals(2f * 4f * 1.5f, bossDrops.get(1).valueMult, 1e-5f);
    }

    @Test
    void quantityNeverReachesUnmappedColonyItemGroups() throws Exception {
        registerTable();
        SectorEntityToken site = mock(SectorEntityToken.class);

        ChartVanillaLoot.addToSite(site, ChartPoi.Kind.DOMAIN, new ChartVanillaLoot.SiteDrops(List.of(drop("rare_tech", 1, -1)), List.of()),
                context(ChartEffect.CHART_QUANTITY_PERCENT, 200f), 15, new Random(1L));

        verify(site, never()).addDropRandom(any(DropData.class));
    }

    @Test
    void aSitesOwnEntriesSuchAsAWrecksHullSizedValueAreScaledToo() throws Exception {
        registerTable();
        SectorEntityToken wreck = mock(SectorEntityToken.class);
        when(wreck.getDropValue()).thenReturn(List.of(drop("basic", -1, 4000)));

        ChartVanillaLoot.addToSite(wreck, ChartPoi.Kind.DERELICT, ChartVanillaLoot.SiteDrops.NONE, context(ChartEffect.CHART_RESOURCE_PERCENT, 25f), 1,
                new Random(1L));

        List<DropData> valueCopies = valueDropsAddedTo(wreck);
        assertEquals(4000, valueCopies.get(0).value);
        assertEquals(0.25f, valueCopies.get(0).valueMult, 1e-5f);
    }

    @Test
    void fractionalExtraChancesRoundUpAsOftenAsTheirFraction() {
        Random random = new Random(7L);
        int total = 0;
        for (int i = 0; i < 10000; i++) {
            total += ChartVanillaLoot.stochasticRound(2.3f, random);
        }
        assertEquals(2.3f, total / 10000f, 0.03f);
        assertEquals(0, ChartVanillaLoot.stochasticRound(-1f, random));
    }
}
