package exiledsector.items;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubroutineKindPoolsTest {

    private static final int TRIALS = 2000;

    @BeforeEach
    void setUp() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(ItemFixtures.subroutineRow("volume_chip", SubroutineKind.VOLUME))
                .put(ItemFixtures.subroutineRow("doctrine_chip", SubroutineKind.DOCTRINE))
                .put(ItemFixtures.subroutineRow("bypass_chip", SubroutineKind.BYPASS))
                .put(ItemFixtures.subroutineRow("relic", SubroutineKind.UNIQUE).put("unique", "true").put("lines", "HULL_MULT:5:5")
                        .put("source", "faction:pirates"))
                .put(ItemFixtures.row("plate", "armor_plating", "HULL_MULT:4:6")));
        ItemDrops.register(new JSONArray().put(new JSONObject().put("site", "derelict").put("chances", "1;1;1").put("uniqueChance", "0")));
    }

    @AfterEach
    void tearDown() {
        ItemDrops.clear();
        ItemDefinitions.clear();
        NpcItems.clearCache();
    }

    @Test
    void onlyUniqueSubroutinesStayInTheGenericPools() {
        Set<SubroutineKind> generic = EnumSet.noneOf(SubroutineKind.class);
        for (SubroutineKind kind : SubroutineKind.values()) {
            if (kind.inGenericPools()) {
                generic.add(kind);
            }
        }

        assertEquals(EnumSet.of(SubroutineKind.UNIQUE), generic);
        assertFalse(SubroutineKind.inGenericPools(ItemDefinitions.get("doctrine_chip")));
        assertTrue(SubroutineKind.inGenericPools(ItemDefinitions.get("plate")));
    }

    @Test
    void npcSocketPicksNeverTakeVolumeDoctrineOrBypassSubroutines() {
        Random random = new Random(3L);
        Set<String> picked = new HashSet<>();
        for (int trial = 0; trial < TRIALS; trial++) {
            ItemDefinition definition = NpcItems.pickDefinition(random, "pirates", candidate -> true);
            if (definition != null) {
                picked.add(definition.id());
            }
        }

        assertEquals(Set.of("relic"), picked);
        assertNull(ItemDrops.pickBasic(new Random(5L)));
    }

    @Test
    void genericDropsNeverProduceVolumeDoctrineOrBypassSubroutines() {
        Random random = new Random(7L);
        Set<String> dropped = new HashSet<>();
        for (int trial = 0; trial < TRIALS; trial++) {
            for (ItemData item : ItemDrops.roll("derelict", random, ItemChances.of(1f), definition -> true)) {
                dropped.add(item.definition().id());
            }
            ItemDefinition sectionPick = ItemDrops.pickSectionItemBasic(ItemKind.SUBROUTINE, random);
            if (sectionPick != null) {
                dropped.add(sectionPick.id());
            }
        }

        assertTrue(dropped.stream().noneMatch(List.of("volume_chip", "doctrine_chip", "bypass_chip")::contains), dropped.toString());
        assertFalse(ItemDrops.hasDroppableBasic(ItemKind.SUBROUTINE));
    }
}
