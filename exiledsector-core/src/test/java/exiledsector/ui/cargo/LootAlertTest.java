package exiledsector.ui.cargo;

import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.CraftingWard;
import exiledsector.items.HullUpgradeData;
import exiledlib.items.ItemData;
import exiledsector.items.ItemDisassembly;
import exiledlib.items.ItemRarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LootAlertTest {

    private static LootAlert alertFor(String itemId) {
        return LootAlert.of(new SpecialItemData(itemId, null));
    }

    @Test
    void tuningImmutableAndReplicatorAreValuable() {
        assertEquals(LootAlert.VALUABLE, alertFor(CraftingCurrency.RECALIBRATION.itemId()));
        assertEquals(LootAlert.VALUABLE, alertFor(CraftingCurrency.FIXATION.itemId()));
        assertEquals(LootAlert.VALUABLE, alertFor(CraftingCurrency.REPLICATION.itemId()));
    }

    @Test
    void otherCurrenciesAndInhibitorsGetTheStandardAlert() {
        assertEquals(LootAlert.STANDARD, alertFor(CraftingCurrency.AUGMENTATION.itemId()));
        assertEquals(LootAlert.STANDARD, alertFor(CraftingCurrency.TRANSPOSITION.itemId()));
        assertEquals(LootAlert.STANDARD, alertFor(CraftingWard.PREFIX.itemId()));
    }

    @Test
    void feedstockAndForeignItemsStaySilent() {
        assertNull(alertFor(ItemDisassembly.PARTS_ITEM_ID));
        assertNull(alertFor("pristine_nanoforge"));
    }

    @Test
    void hullUpgradesHaveTheirOwnAlert() {
        assertEquals(LootAlert.HULL_UPGRADE, LootAlert.of(new HullUpgradeData(HullSize.CRUISER).toSpecialItem()));
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
