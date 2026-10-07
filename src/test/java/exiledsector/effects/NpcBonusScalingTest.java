package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.NpcBonusScaling;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.CombatSkillEffect;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.skills.skilleffect.LogisticsSkillEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcBonusScalingTest {

    private static final float TOLERANCE = 0.001f;

    private MockedStatic<Global> globalMock;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private int nodeCount;

    @BeforeEach
    void setUp() {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        ResolvedTree.clearCache();
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        lunaSettingsMock.close();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        ResolvedTree.clearCache();
    }

    @Test
    void bonusesAreMultipliedWhileDrawbacksStayAsWritten() {
        ShipSkillData shipData = shipWith(List.of(),
                new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f),
                new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, 30f),
                new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, -10f));

        Map<String, Float> magnitudes = magnitudesByModId(ResolvedTree.of(shipData, HullSize.CRUISER, 3f), DefenseSkillEffect.ARMOR_PERCENT);

        assertEquals(30f, magnitude(ResolvedTree.of(shipData, HullSize.CRUISER, 3f), DefenseSkillEffect.HULL_PERCENT), TOLERANCE);
        assertEquals(Map.of("exiledSector_skill_node_2_ARMOR_PERCENT", 90f, "exiledSector_skill_node_3_ARMOR_PERCENT", -10f), magnitudes);
    }

    @Test
    void multiplierEffectsScaleTheirCombinedTotal() {
        ShipSkillData shipData = shipWith(List.of(),
                new SkillTypeEffect(DefenseSkillEffect.HULL_MULT, 5f),
                new SkillTypeEffect(DefenseSkillEffect.HULL_MULT, 5f));

        assertEquals(30f, magnitude(ResolvedTree.of(shipData, HullSize.CRUISER, 3f), DefenseSkillEffect.HULL_MULT), TOLERANCE);
    }

    @Test
    void reductionsCompoundTowardTheirLimitInsteadOfPassingIt() {
        ShipSkillData shipData = shipWith(List.of(),
                new SkillTypeEffect(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT, -20f),
                new SkillTypeEffect(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT, -20f));

        float scaledTotal = magnitudesByModId(ResolvedTree.of(shipData, HullSize.CRUISER, 3f), DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT)
                .values().stream().reduce(0f, Float::sum);

        assertEquals(-100f * (1f - (float) Math.pow(0.6f, 3)), scaledTotal, TOLERANCE);
    }

    @Test
    void theDamageTakenCapStillAppliesAfterScaling() {
        ShipSkillData shipData = shipWith(List.of(),
                new SkillTypeEffect(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT, -30f),
                new SkillTypeEffect(DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT, -30f));

        Map<String, Float> magnitudes = magnitudesByModId(ResolvedTree.of(shipData, HullSize.CRUISER, 3f), DefenseSkillEffect.ENERGY_DAMAGE_TAKEN_PERCENT);

        assertEquals(Map.of("exiledSector_skillCapped_ENERGY_DAMAGE_TAKEN_PERCENT", -80f), magnitudes);
    }

    @Test
    void logisticsNodesCountedEffectsAndListenerEffectsAreNotScaled() {
        ShipSkillData shipData = shipWith(List.of("logistics"), new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f));
        allocate(shipData, List.of(), new SkillTypeEffect(LogisticsSkillEffect.BURN_LEVEL_FLAT, 1f));
        allocate(shipData, List.of(), new SkillTypeEffect(CombatSkillEffect.DISINTEGRATION_ARMOR_DAMAGE_PERCENT, 20f));

        ResolvedTree resolvedTree = ResolvedTree.of(shipData, HullSize.CRUISER, 3f);

        assertEquals(10f, magnitude(resolvedTree, DefenseSkillEffect.HULL_PERCENT), TOLERANCE);
        assertEquals(1f, magnitude(resolvedTree, LogisticsSkillEffect.BURN_LEVEL_FLAT), TOLERANCE);
        assertEquals(20f, magnitude(resolvedTree, CombatSkillEffect.DISINTEGRATION_ARMOR_DAMAGE_PERCENT), TOLERANCE);
    }

    @Test
    void campaignStatsAndDrawbackParametersAreNotScaledEvenOnUntaggedNodes() {
        SkillEffect sensorStrength = SkillEffect.byName("SENSOR_STRENGTH_FLAT");
        SkillEffect rangeThreshold = SkillEffect.byName("WEAPON_RANGE_THRESHOLD_FLAT");
        ShipSkillData shipData = shipWith(List.of(), new SkillTypeEffect(sensorStrength, 50f), new SkillTypeEffect(rangeThreshold, 450f));

        ResolvedTree resolvedTree = ResolvedTree.of(shipData, HullSize.CRUISER, 3f);

        assertEquals(50f, magnitude(resolvedTree, sensorStrength), TOLERANCE);
        assertEquals(450f, magnitude(resolvedTree, rangeThreshold), TOLERANCE);
    }

    @Test
    void aScaleOfOneLeavesEveryMagnitudeUnchanged() {
        ShipSkillData shipData = shipWith(List.of(), new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f));

        assertEquals(10f, magnitude(ResolvedTree.of(shipData, HullSize.CRUISER, 1f), DefenseSkillEffect.HULL_PERCENT), TOLERANCE);
    }

    @Test
    void flatReductionsScaleLinearlyAndFullReductionsStayFull() {
        assertEquals(3f, NpcBonusScaling.factor(StatMode.FLAT, -5f, 3f), TOLERANCE);
        assertEquals(1f, NpcBonusScaling.factor(StatMode.PERCENT, -100f, 3f), TOLERANCE);
        assertEquals(3f, NpcBonusScaling.factor(StatMode.PERCENT, 25f, 3f), TOLERANCE);
    }

    @Test
    void everyUnscaledEffectNameIsARealEffect() {
        NpcBonusScaling.UNSCALED_EFFECTS.forEach(effectName -> assertNotNull(SkillEffect.byName(effectName)));
    }

    private ShipSkillData shipWith(List<String> tags, SkillTypeEffect... effects) {
        ShipSkillData shipData = ShipSkillDataManager.get("npc-ship");
        for (SkillTypeEffect effect : effects) {
            allocate(shipData, tags, effect);
        }
        return shipData;
    }

    private void allocate(ShipSkillData shipData, List<String> tags, SkillTypeEffect effect) {
        nodeCount++;
        SkillType type = new SkillType.Builder("type_" + nodeCount, "Type", "a.png", SkillTier.SMALL)
                .effects(List.of(effect)).tags(tags).build();
        SkillNode node = new SkillNode("node_" + nodeCount, type, List.of(), 0f, 0f);
        SkillTree.register(node);
        shipData.allocate(node, 1);
    }

    private static Map<String, Float> magnitudesByModId(ResolvedTree resolvedTree, SkillEffect effect) {
        Map<String, Float> magnitudes = new HashMap<>();
        for (ResolvedTree.Entry entry : resolvedTree.entries()) {
            if (entry instanceof ResolvedTree.EffectEntry effectEntry && effectEntry.effect() == effect) {
                magnitudes.put(effectEntry.modId(), effectEntry.magnitude());
            }
        }
        return magnitudes;
    }

    private static float magnitude(ResolvedTree resolvedTree, SkillEffect effect) {
        return magnitudesByModId(resolvedTree, effect).values().stream().reduce(0f, Float::sum);
    }
}
