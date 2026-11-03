package exiledsector.loot;

import exiledsector.MockSector;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleMaterialsTest {

    private static final String AUGMENTATION = "exiledSector_augmentation_nanoswarm";

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockSector mockSector;

    @BeforeEach
    void setUp() throws Exception {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        mockSector = new MockSector();
        BattleLootRates.register(new JSONArray()
                .put(new JSONObject().put("drop", AUGMENTATION).put("per_100_dp", "1").put("curve", "linear").put("regular_share", "1"))
                .put(new JSONObject().put("drop", "not_a_material").put("per_100_dp", "5").put("curve", "linear").put("regular_share", "1")));
    }

    @AfterEach
    void tearDown() {
        BattleLootRates.clear();
        mockSector.close();
        lunaSettingsMock.close();
    }

    @Test
    void aFeedstockOnlyFleetDropsBaseFeedstockAndNoMaterials() {
        BattleMaterials regular = new BattleMaterials();
        regular.record(200f, 3f, FleetLootRules.REGULAR);
        BattleMaterials feedstockOnly = new BattleMaterials();
        feedstockOnly.record(200f, 3f, FleetLootRules.FEEDSTOCK_ONLY);

        assertEquals(3f * feedstockOnly.feedstock(), regular.feedstock(), 1e-4f);
        assertEquals(Map.of(AUGMENTATION, 6), regular.rollMaterials(new Random(1L)));
        assertTrue(feedstockOnly.rollMaterials(new Random(1L)).isEmpty());
    }

    @Test
    void aFleetWhoseSourceRollsItsMaterialsStillCountsTowardsTheBattleButDropsNothingHere() {
        BattleMaterials battle = new BattleMaterials();
        battle.record(300f, 1f, new FleetLootRules(FleetLootRules.Rates.FULL, true, LootMultipliers.NONE, 1f));

        assertTrue(battle.rollMaterials(new Random(1L)).isEmpty());
        assertEquals(Map.of(AUGMENTATION, 3), BattleMaterials.roll(300f, 300f, 1f, FleetLootRules.full(LootMultipliers.NONE), new Random(1L)));
    }
}
