package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogisticsDModEffectsTest {

    private static final float EPSILON = 0.001f;
    private static final String STORAGE_DMOD = "comp_storage";
    private static final String ENGINE_DMOD = "degraded_engines";

    private MockedStatic<Global> globalMock;
    private MutableShipStatsAPI stats;
    private StatBonus cargo;
    private MutableStat maxSpeed;
    private MutableStat suppliesToRecover;
    private MutableStat suppliesPerMonth;
    private MutableStat maxBurn;

    private static final class FakeDynamicStats implements DynamicStatsAPI {
        private final Map<String, MutableStat> statsById = new HashMap<>();
        private final Map<String, StatBonus> modsById = new HashMap<>();

        @Override
        public MutableStat getStat(String id) {
            return statsById.computeIfAbsent(id, key -> new MutableStat(1f));
        }

        @Override
        public float getValue(String id) {
            return getStat(id).getModifiedValue();
        }

        @Override
        public StatBonus getMod(String id) {
            return modsById.computeIfAbsent(id, key -> new StatBonus());
        }

        @Override
        public float getValue(String id, float base) {
            return statsById.containsKey(id) ? statsById.get(id).getModifiedValue() : base;
        }

        @Override
        public void removeUmodified() {
            statsById.values().removeIf(stat -> stat.getModifiedValue() == stat.getBaseValue());
        }

        @Override
        public Map<String, MutableStat> getStats() {
            return statsById;
        }

        @Override
        public Map<String, StatBonus> getMods() {
            return modsById;
        }

        @Override
        public boolean isEmpty() {
            return statsById.isEmpty() && modsById.isEmpty();
        }
    }

    private static final class StorageDMod extends BaseHullMod {
        @Override
        public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
            float effect = stats.getDynamic().getValue(Stats.DMOD_EFFECT_MULT);
            stats.getCargoMod().modifyPercent(id, -10f * effect);
            stats.getMaxBurnLevel().modifyFlat(id, -1f);
            stats.getSuppliesToRecover().modifyMult(id, 0.8f);
        }
    }

    private static final class EngineDMod extends BaseHullMod {
        @Override
        public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
            float effect = stats.getDynamic().getValue(Stats.DMOD_EFFECT_MULT);
            stats.getMaxSpeed().modifyPercent(id, -10f * effect);
            stats.getSuppliesToRecover().modifyMult(id, 0.8f);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        SettingsAPI settings = mock(SettingsAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        when(settings.getMergedSpreadsheetDataForMod(anyString(), anyString(), anyString())).thenReturn(new JSONArray());
        when(settings.getMergedSpreadsheetDataForMod("hullmod", "data/config/exiledSector/logistics_dmods.csv", "exiledSector"))
                .thenReturn(new JSONArray().put(new JSONObject().put("hullmod", STORAGE_DMOD)));
        CsvIdList.loadAll();

        HullModSpecAPI storageSpec = dModSpec(STORAGE_DMOD, new StorageDMod());
        HullModSpecAPI engineSpec = dModSpec(ENGINE_DMOD, new EngineDMod());
        when(settings.getHullModSpec(STORAGE_DMOD)).thenReturn(storageSpec);
        when(settings.getHullModSpec(ENGINE_DMOD)).thenReturn(engineSpec);
        buildShip();
    }

    private void buildShip() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullMods()).thenReturn(List.of(STORAGE_DMOD, ENGINE_DMOD));
        when(variant.getHullSize()).thenReturn(HullSize.CRUISER);
        stats = mock(MutableShipStatsAPI.class);
        when(stats.getVariant()).thenReturn(variant);
        when(stats.getDynamic()).thenReturn(new FakeDynamicStats());
        cargo = new StatBonus();
        maxSpeed = new MutableStat(100f);
        suppliesToRecover = new MutableStat(10f);
        suppliesPerMonth = new MutableStat(10f);
        maxBurn = new MutableStat(9f);
        when(stats.getCargoMod()).thenReturn(cargo);
        when(stats.getMaxSpeed()).thenReturn(maxSpeed);
        when(stats.getSuppliesToRecover()).thenReturn(suppliesToRecover);
        when(stats.getSuppliesPerMonth()).thenReturn(suppliesPerMonth);
        when(stats.getMaxBurnLevel()).thenReturn(maxBurn);
        new StorageDMod().applyEffectsBeforeShipCreation(HullSize.CRUISER, stats, STORAGE_DMOD);
        new EngineDMod().applyEffectsBeforeShipCreation(HullSize.CRUISER, stats, ENGINE_DMOD);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static HullModSpecAPI dModSpec(String id, BaseHullMod effect) {
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(spec.getId()).thenReturn(id);
        when(spec.hasTag(Tags.HULLMOD_DMOD)).thenReturn(true);
        when(spec.getEffect()).thenReturn(effect);
        return spec;
    }

    private float cargoPenaltyPercent() {
        return cargo.computeEffective(100f) - 100f;
    }

    private float speedPenaltyPercent() {
        return maxSpeed.getModifiedValue() - 100f;
    }

    @Test
    void theLogisticsEffectWorsensOnlyLogisticsDMods() {
        DefenseSkillEffect.LOGISTICS_DMOD_EFFECT_MULT_PER_DMOD.apply(stats, "mod_id", 30f);

        assertEquals(-10f * 1.3f * 1.3f, cargoPenaltyPercent(), EPSILON);
        assertEquals(-10f, speedPenaltyPercent(), EPSILON);
    }

    @Test
    void theNonLogisticsEffectWorsensOnlyCombatDMods() {
        DefenseSkillEffect.NON_LOGISTICS_DMOD_EFFECT_MULT_PER_DMOD.apply(stats, "mod_id", 10f);

        assertEquals(-10f, cargoPenaltyPercent(), EPSILON);
        assertEquals(-10f * 1.1f * 1.1f, speedPenaltyPercent(), EPSILON);
    }

    @Test
    void bothEffectsGiveTheSameResultWhicheverAppliesFirst() {
        DefenseSkillEffect.NON_LOGISTICS_DMOD_EFFECT_MULT_PER_DMOD.apply(stats, "combat", 10f);
        DefenseSkillEffect.LOGISTICS_DMOD_EFFECT_MULT_PER_DMOD.apply(stats, "logistics", 30f);
        float cargoCombatFirst = cargoPenaltyPercent();
        float speedCombatFirst = speedPenaltyPercent();

        buildShip();
        DefenseSkillEffect.LOGISTICS_DMOD_EFFECT_MULT_PER_DMOD.apply(stats, "logistics", 30f);
        DefenseSkillEffect.NON_LOGISTICS_DMOD_EFFECT_MULT_PER_DMOD.apply(stats, "combat", 10f);

        assertEquals(-10f * 1.3f * 1.3f, cargoCombatFirst, EPSILON);
        assertEquals(-10f * 1.1f * 1.1f, speedCombatFirst, EPSILON);
        assertEquals(cargoCombatFirst, cargoPenaltyPercent(), EPSILON);
        assertEquals(speedCombatFirst, speedPenaltyPercent(), EPSILON);
    }

    @Test
    void theExtraCopyNeverAddsRecoveryCostDiscountsOrBurnLevelPenalties() {
        DefenseSkillEffect.LOGISTICS_DMOD_EFFECT_MULT_PER_DMOD.apply(stats, "mod_id", 30f);

        assertEquals(10f * 0.8f * 0.8f, suppliesToRecover.getModifiedValue(), EPSILON);
        assertEquals(8f, maxBurn.getModifiedValue(), EPSILON);
        assertEquals(10f, suppliesPerMonth.getModifiedValue(), EPSILON);
    }
}
