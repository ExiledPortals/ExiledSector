package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemModifiers;
import exiledsector.items.ModifierSources;
import exiledsector.items.PoolEntry;
import exiledsector.items.RolledModifier;
import exiledsector.modifiers.ModifierSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImmutableDropsTest {

    private static final String BRIDGE = "test_bridge";
    private static final String SUBROUTINE = "test_subroutine";

    @BeforeEach
    void setUp() {
        ItemDefinitions.register(List.of(definition(BRIDGE, ItemKind.BRIDGE), definition(SUBROUTINE, ItemKind.SUBROUTINE)));
    }

    @AfterEach
    void tearDown() {
        ModifierSources.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static ItemDefinition definition(String id, ItemKind kind) {
        return new ItemDefinition(id, kind, id, ItemDefinition.FALLBACK_ICON, 1f, false, "",
                List.of(PoolEntry.of(ModifierSlot.PREFIX, "HULL_PERCENT", 4f, 6f, 1f),
                        PoolEntry.of(ModifierSlot.PREFIX, "FLUX_CAPACITY_PERCENT", 4f, 6f, 1f)),
                List.of(PoolEntry.of(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 4f, 6f, 1f),
                        PoolEntry.of(ModifierSlot.SUFFIX, "FLUX_DISSIPATION_PERCENT", 4f, 6f, 1f)), "");
    }

    private static ItemData dropped(String definitionId, long seed, int modifierCount) {
        return ItemData.rolledWithModifierCount(ItemDefinitions.get(definitionId), seed, modifierCount);
    }

    private static long immutableCount(ItemData item) {
        return item.explicits().stream().filter(RolledModifier::immutable).count();
    }

    private static List<String> modifierIds(ItemData item) {
        return item.explicits().stream().map(RolledModifier::modifierId).toList();
    }

    @Test
    void aCertainDropFillsToFourModifiersKeepingItsOwnAndLocksExactlyOne() {
        for (long seed = 0L; seed < 20L; seed++) {
            ItemData item = dropped(BRIDGE, seed, 2);

            ItemData locked = ImmutableDrops.withChance(item, 1f);

            assertEquals(1L, immutableCount(locked));
            assertEquals(4, locked.explicits().size());
            assertTrue(modifierIds(locked).containsAll(modifierIds(item)));
            assertEquals(item.seed(), locked.seed());
            assertSame(item.traits(), locked.traits());
        }
    }

    @Test
    void aboutOneDropInTenIsLockedAtTheBlackHolesChance() {
        int locked = 0;
        for (long seed = 0L; seed < 2000L; seed++) {
            locked += (int) immutableCount(ImmutableDrops.withChance(dropped(BRIDGE, seed, 2), 0.1f));
        }
        assertTrue(locked > 140 && locked < 260, "got " + locked);
    }

    @Test
    void theSameDropAlwaysRollsTheSameWay() {
        for (long seed = 0L; seed < 50L; seed++) {
            assertEquals(ImmutableDrops.withChance(dropped(BRIDGE, seed, 2), 0.5f), ImmutableDrops.withChance(dropped(BRIDGE, seed, 2), 0.5f));
        }
    }

    @Test
    void aSingleModifierDropIsFilledToFourAndLocked() {
        ItemData locked = ImmutableDrops.withChance(dropped(BRIDGE, 3L, 1), 1f);

        assertEquals(4, locked.explicits().size());
        assertEquals(1L, immutableCount(locked));
    }

    @Test
    void anUnlockedDropKeepsItsModifierCount() {
        for (long seed = 0L; seed < 50L; seed++) {
            ItemData item = dropped(BRIDGE, seed, 2);
            ItemData shaped = ImmutableDrops.withChance(item, 0.1f);
            if (immutableCount(shaped) == 0L) {
                assertSame(item, shaped);
            }
        }
    }

    @Test
    void subroutinesAreLeftAlone() {
        ItemData subroutine = dropped(SUBROUTINE, 3L, 2);

        assertSame(subroutine, ImmutableDrops.withChance(subroutine, 1f));
        assertSame(null, ImmutableDrops.withChance(null, 1f));
    }

    @Test
    void onlyAMarkedSiteLocksItsDrops() {
        ItemData item = dropped(BRIDGE, 7L, 2);
        SectorEntityToken site = mock(SectorEntityToken.class);
        MemoryAPI memory = FakeChartContentEngine.memory();
        when(site.getMemoryWithoutUpdate()).thenReturn(memory);

        assertSame(item, StarChartLootSource.INSTANCE.shapeSiteDrop(item, site));

        ImmutableDrops.mark(site, 1f);

        assertEquals(1L, immutableCount(StarChartLootSource.INSTANCE.shapeSiteDrop(item, site)));
        assertSame(item, ImmutableDrops.onSiteDrop(item, null));
    }
}
