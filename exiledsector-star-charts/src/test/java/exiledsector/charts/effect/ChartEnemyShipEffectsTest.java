package exiledsector.charts.effect;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.charts.loot.StarChartLootSource;
import exiledsector.effects.GrantedNodeOrigin;
import exiledsector.effects.GrantedNodes;
import exiledsector.effects.ResolvedTree;
import exiledsector.effects.ShipEffectContext;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartEnemyShipEffectsTest {

    private static final float TOLERANCE = 0.0001f;

    private static ChartGenContext enemyRules() {
        return ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_ENEMY_OBJECTIVE_CAPTURE_RATE_PERCENT, 50f),
                new AppliedEffect(ChartModifier.CHART_ENEMY_KINETIC_DAMAGE_PERCENT, 20f),
                new AppliedEffect(ChartModifier.CHART_TOP_SPEED_PERCENT, -20f)));
    }

    private static CampaignFleetAPI fleetWith(FleetMemberAPI... members) {
        Map<String, Object> values = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getString(anyString())).thenAnswer(call -> (String) values.get(call.<String>getArgument(0)));
        doAnswer(call -> values.put(call.getArgument(0), call.getArgument(1))).when(memory).set(anyString(), any());
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        when(fleet.getFleetData()).thenReturn(fleetData);
        when(fleetData.getFleet()).thenReturn(fleet);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(members));
        for (FleetMemberAPI member : members) {
            when(member.getFleetData()).thenReturn(fleetData);
        }
        return fleet;
    }

    private static FleetMemberAPI memberWith(ShipVariantAPI variant) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getVariant()).thenReturn(variant);
        return member;
    }

    @Test
    void generationGrantsTheChartsEnemyRulesThroughTheHiddenHullModOnACopyOfAStockVariant() {
        ShipVariantAPI stock = mock(ShipVariantAPI.class);
        ShipVariantAPI copy = mock(ShipVariantAPI.class);
        when(stock.getSource()).thenReturn(VariantSource.STOCK);
        when(stock.clone()).thenReturn(copy);
        FleetMemberAPI member = memberWith(stock);
        fleetWith(member);
        CampaignFleetAPI fleet = member.getFleetData().getFleet();

        ChartEnemyShipEffects.grant(fleet, enemyRules());

        verify(copy).setSource(VariantSource.REFIT);
        verify(member).setVariant(copy, false, true);
        verify(copy).addMod(SkillTreeHullMod.ID);
        verify(stock, never()).addMod(anyString());
        ChartEnemyShipEffects.Granted granted = ChartEnemyShipEffects.grantedTo(member);
        assertEquals(Map.of(ChartFleetStat.ENEMY_OBJECTIVE_CAPTURE_RATE, 1.5f, ChartFleetStat.ENEMY_KINETIC_DAMAGE_DEALT, 1.2f), granted.statMults());
        assertNotNull(granted.damageListener());
        assertTrue(granted.nodeIds().isEmpty());
    }

    @Test
    void inflationThatSwapsInAFreshVariantGetsTheHullModBackOnlyOnGrantedFleets() {
        ShipVariantAPI granted = mock(ShipVariantAPI.class);
        when(granted.getSource()).thenReturn(VariantSource.REFIT);
        FleetMemberAPI member = memberWith(granted);
        CampaignFleetAPI fleet = fleetWith(member);
        ChartEnemyShipEffects.grant(fleet, enemyRules());
        ShipVariantAPI inflated = mock(ShipVariantAPI.class);
        when(inflated.getSource()).thenReturn(VariantSource.REFIT);
        when(member.getVariant()).thenReturn(inflated);
        ShipVariantAPI untouched = mock(ShipVariantAPI.class);
        CampaignFleetAPI ungranted = fleetWith(memberWith(untouched));

        new ChartEnemyInflationListener().reportFleetInflated(fleet, null);
        new ChartEnemyInflationListener().reportFleetInflated(ungranted, null);

        verify(inflated).addMod(SkillTreeHullMod.ID);
        verify(untouched, never()).addMod(anyString());
    }

    @Test
    void inBattleTheMemberReadsItsOwnFleetRatherThanTheCombinedOne() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getSource()).thenReturn(VariantSource.REFIT);
        FleetMemberAPI member = memberWith(variant);
        CampaignFleetAPI source = fleetWith(member);
        ChartEnemyShipEffects.grant(source, enemyRules());
        FleetDataAPI sourceData = member.getFleetData();
        CampaignFleetAPI combined = fleetWith(member);
        when(member.getFleetDataForStats()).thenReturn(sourceData);

        assertEquals(2, ChartEnemyShipEffects.grantedTo(member).statMults().size());
        assertTrue(combined.getMemoryWithoutUpdate().getString(ChartIds.ENEMY_EFFECTS_MEMORY_KEY) == null);
    }

    @Test
    void grantedNodesTravelWithTheFleetToTheTreeBuilderBesideTheOtherEnemyRules() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getSource()).thenReturn(VariantSource.REFIT);
        FleetMemberAPI member = memberWith(variant);
        CampaignFleetAPI fleet = fleetWith(member);
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_ENEMY_FREEBOOTERS_CHARGE, 1f),
                new AppliedEffect(ChartModifier.CHART_ENEMY_HULLBREACHER_ROUNDS, 1f), new AppliedEffect(ChartModifier.CHART_ENEMY_KINETIC_DAMAGE_PERCENT, 20f)));

        ChartEnemyShipEffects.grant(fleet, context);

        assertEquals(Set.of("freebooters_charge_1", "hullbreacher_rounds_1"), Set.copyOf(StarChartLootSource.INSTANCE.grantedNodeIds(fleet.getMemoryWithoutUpdate())));
        assertEquals(Map.of(ChartFleetStat.ENEMY_KINETIC_DAMAGE_DEALT, 1.2f), ChartEnemyShipEffects.grantedTo(member).statMults());
        verify(variant).addMod(SkillTreeHullMod.ID);
    }

    @Test
    void fleetsTheChartGrantedNoNodesHandTheTreeBuilderNone() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getSource()).thenReturn(VariantSource.REFIT);
        CampaignFleetAPI fleet = fleetWith(memberWith(variant));

        ChartEnemyShipEffects.grant(fleet, enemyRules());

        assertNull(StarChartLootSource.INSTANCE.grantedNodeIds(fleet.getMemoryWithoutUpdate()));
    }

    @Test
    void fleetsTheChartGrantedNothingGetNoEffects() {
        FleetMemberAPI member = memberWith(mock(ShipVariantAPI.class));
        CampaignFleetAPI fleet = fleetWith(member);

        ChartEnemyShipEffects.grant(fleet, ChartGenContext.none());

        verify(fleet.getMemoryWithoutUpdate(), never()).set(anyString(), any());
        assertSame(ChartEnemyShipEffects.Granted.NONE, ChartEnemyShipEffects.grantedTo(member));
        assertTrue(ChartEnemyShipEffects.INSTANCE.contributions(new ShipEffectContext(null, member, null, null)).isEmpty());
        assertSame(ChartEnemyShipEffects.Granted.NONE, ChartEnemyShipEffects.grantedTo(null));
    }

    @Test
    void grantedShipsCaptureFasterAndTheirShipsAndFightersDealMoreOfTheirDamageType() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getSource()).thenReturn(VariantSource.REFIT);
        FleetMemberAPI member = memberWith(variant);
        ChartEnemyShipEffects.grant(fleetWith(member), enemyRules());
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        MutableStat captureRate = new MutableStat(1f);
        when(stats.getFleetMember()).thenReturn(member);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getStat(Stats.SHIP_OBJECTIVE_CAP_RATE_MULT)).thenReturn(captureRate);
        ShipAPI ship = mock(ShipAPI.class);
        ShipAPI fighter = mock(ShipAPI.class);
        when(ship.getFleetMember()).thenReturn(member);

        ChartEnemyShipEffects.INSTANCE.applyToShip(stats);
        ChartEnemyShipEffects.INSTANCE.afterShipCreation(ship, null);
        ChartEnemyShipEffects.INSTANCE.applyToFighter(fighter, ship);

        verify(variant).addMod(SkillTreeHullMod.ID);
        assertEquals(1.5f, captureRate.getModifiedValue(), TOLERANCE);
        ChartEnemyDamageListener listener = ChartEnemyShipEffects.grantedTo(member).damageListener();
        verify(ship).addListener(listener);
        verify(fighter).addListener(listener);
    }

    @Test
    void aGrantedShipsModulesGetItsShipRulesAndDamageListener() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getSource()).thenReturn(VariantSource.REFIT);
        FleetMemberAPI member = memberWith(variant);
        ChartEnemyShipEffects.grant(fleetWith(member), enemyRules());
        ShipEffectContext parentContext = new ShipEffectContext(new ShipSkillData(), member, variant, HullSize.CAPITAL_SHIP);
        MutableShipStatsAPI moduleStats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        MutableStat captureRate = new MutableStat(1f);
        when(moduleStats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getStat(Stats.SHIP_OBJECTIVE_CAP_RATE_MULT)).thenReturn(captureRate);
        ShipAPI module = mock(ShipAPI.class);

        ChartEnemyShipEffects.INSTANCE.applyToModule(parentContext, moduleStats);
        ChartEnemyShipEffects.INSTANCE.afterModuleCreation(parentContext, module);

        assertEquals(1.5f, captureRate.getModifiedValue(), TOLERANCE);
        verify(module).addListener(ChartEnemyShipEffects.grantedTo(member).damageListener());
    }

    @Test
    void nodesGrantedWithoutATreeReachTheModulesAsPermanentTotals() {
        ShipVariantAPI moduleVariant = mock(ShipVariantAPI.class);
        when(moduleVariant.getHullSize()).thenReturn(HullSize.CRUISER);
        SkillTypeEffect fluxCapacity = new SkillTypeEffect(SkillEffect.byName("FLUX_CAPACITY_PERCENT"), 10f);
        ResolvedTree parentTree = mock(ResolvedTree.class);
        when(parentTree.contributions()).thenReturn(List.of(
                EffectContribution.scaledPermanent(new GrantedNodeOrigin("hullbreacher_rounds_1"), "granted_", List.of(fluxCapacity)),
                EffectContribution.scaledPermanent("tree node", "tree_", List.of(fluxCapacity))));
        List<EffectContribution> moduleNodes = List.of(EffectContribution.scaledPermanent("granted", "granted_", List.of(fluxCapacity, fluxCapacity)),
                new EffectContribution("burst", "burst_", List.of(fluxCapacity), 30f, null, true, true));
        ShipEffectContext parentContext = new ShipEffectContext(new ShipSkillData(), memberWith(mock(ShipVariantAPI.class)), null, HullSize.CAPITAL_SHIP);

        Map<SkillEffect, Float> moduleTotals;
        try (MockedStatic<GrantedNodes> grantedNodes = Mockito.mockStatic(GrantedNodes.class)) {
            grantedNodes.when(() -> GrantedNodes.withoutTree(eq(List.of("hullbreacher_rounds_1")),
                    argThat(context -> context.variant() == moduleVariant), eq(ChartEnemyShipEffects.NODE_MOD_ID_PREFIX))).thenReturn(moduleNodes);
            moduleTotals = ChartEnemyShipEffects.INSTANCE.moduleContributions(parentContext, parentTree, moduleVariant);
        }

        assertEquals(Map.of(SkillEffect.byName("FLUX_CAPACITY_PERCENT"), 20f), moduleTotals);
        assertTrue(ChartEnemyShipEffects.INSTANCE.moduleContributions(parentContext, mock(ResolvedTree.class), moduleVariant).isEmpty());
    }

    @Test
    void theEnemyDamageListenerRaisesOnlyItsDamageType() {
        ChartEnemyDamageListener listener = ChartEnemyDamageListener.of(Map.of(ChartFleetStat.ENEMY_KINETIC_DAMAGE_DEALT, 1.2f));
        DamageAPI kinetic = damageOf(DamageType.KINETIC);
        DamageAPI energy = damageOf(DamageType.ENERGY);

        assertEquals(ChartPlayerModifiers.MOD_ID, listener.modifyDamageDealt(null, null, kinetic, null, false));
        assertNull(listener.modifyDamageDealt(null, null, energy, null, true));

        assertEquals(1.2f, kinetic.getModifier().getMultStatMod(ChartPlayerModifiers.MOD_ID).getValue(), TOLERANCE);
        assertNull(energy.getModifier().getMultStatMod(ChartPlayerModifiers.MOD_ID));
        assertNull(ChartEnemyDamageListener.of(Map.of(ChartFleetStat.ENEMY_OBJECTIVE_CAPTURE_RATE, 1.5f)));
    }

    private static DamageAPI damageOf(DamageType type) {
        DamageAPI damage = mock(DamageAPI.class);
        MutableStat modifier = new MutableStat(1f);
        when(damage.getType()).thenReturn(type);
        when(damage.getModifier()).thenReturn(modifier);
        return damage;
    }
}
