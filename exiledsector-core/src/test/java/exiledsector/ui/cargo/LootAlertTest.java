package exiledsector.ui.cargo;

import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.items.Nanoswarm;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.ItemRarity;
import exiledsector.items.RefitSchematic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LootAlertTest {

    private static LootAlert alertFor(String itemId) {
        return LootAlert.of(new SpecialItemData(itemId, null));
    }

    @Test
    void tuningAnnulmentImmutableAndReplicatorAreValuable() {
        assertEquals(LootAlert.VALUABLE, alertFor(Nanoswarm.TUNING.itemId()));
        assertEquals(LootAlert.VALUABLE, alertFor(Nanoswarm.ANNULMENT.itemId()));
        assertEquals(LootAlert.VALUABLE, alertFor(Nanoswarm.IMMUTABLE.itemId()));
        assertEquals(LootAlert.VALUABLE, alertFor(Nanoswarm.REPLICATION.itemId()));
    }

    @Test
    void otherCurrenciesAndInhibitorsGetTheStandardAlert() {
        assertEquals(LootAlert.STANDARD, alertFor(Nanoswarm.AUGMENTATION.itemId()));
        assertEquals(LootAlert.STANDARD, alertFor(Nanoswarm.METAMORPH.itemId()));
        assertEquals(LootAlert.STANDARD, alertFor(CraftingInhibitor.PREFIX.itemId()));
    }

    @Test
    void feedstockAndForeignItemsStaySilent() {
        assertNull(alertFor(ItemDisassembly.PARTS_ITEM_ID));
        assertNull(alertFor("pristine_nanoforge"));
    }

    @Test
    void everyRefitSchematicTierAndAnOldHullUpgradeHaveTheirOwnAlert() {
        for (RefitSchematic schematic : RefitSchematic.values()) {
            assertEquals(LootAlert.REFIT_SCHEMATIC, LootAlert.of(schematic.toSpecialItem()));
        }
        assertEquals(LootAlert.REFIT_SCHEMATIC, LootAlert.of(new SpecialItemData(RefitSchematic.ITEM_ID, HullSize.CRUISER.name())));
    }

    @Test
    void everyStarChartHasItsOwnAlert() {
        assertEquals(LootAlert.STAR_CHART, alertFor(ItemData.STAR_CHART_ITEM_ID));
    }

    @Test
    void itemRarityPicksTheAlert() {
        assertNull(LootAlert.of(ItemRarity.COMMON));
        assertEquals(LootAlert.STANDARD, LootAlert.of(ItemRarity.RARE));
        assertEquals(LootAlert.VALUABLE, LootAlert.of(ItemRarity.UNIQUE));
    }
}
