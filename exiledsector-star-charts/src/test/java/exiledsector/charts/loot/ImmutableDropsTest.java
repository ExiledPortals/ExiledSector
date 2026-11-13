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
                        PoolEntry.of(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 4f, 6f, 1f)), List.of(), "");
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
    void aCertainDropLocksExactlyOneOfItsModifiersAndKeepsTheRest() {
        for (long seed = 0L; seed < 20L; seed++) {
            ItemData item = dropped(BRIDGE, seed, 2);

            ItemData locked = ImmutableDrops.withChance(item, 1f);

            assertEquals(1L, immutableCount(locked));
            assertEquals(modifierIds(item), modifierIds(locked));
            assertEquals(item.seed(), locked.seed());
            assertEquals(item.frozenName(), locked.frozenName());
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
    void subroutinesAndItemsTheNanoswarmCouldNotLockAreLeftAlone() {
        ItemData subroutine = dropped(SUBROUTINE, 3L, 2);
        ItemData single = dropped(BRIDGE, 3L, 1);

        assertSame(subroutine, ImmutableDrops.withChance(subroutine, 1f));
        assertSame(single, ImmutableDrops.withChance(single, 1f));
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
