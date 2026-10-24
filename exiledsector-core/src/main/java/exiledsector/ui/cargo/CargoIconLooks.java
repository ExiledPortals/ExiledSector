package exiledsector.ui.cargo;

import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.CraftingWard;
import exiledsector.items.ItemCurrencies;
import exiledsector.items.ItemCurrency;
import exiledsector.items.Item;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemRarity;
import exiledsector.items.MaterialItems;
import exiledsector.items.MaterialTier;

import java.awt.Color;

public final class CargoIconLooks {

    private static final float CORRUPTED_GLOW = 0.45f;
    private static final DomainContributions<ItemIconLooks> ITEM_LOOKS = DomainContributions.fromEveryDomain(ExiledSectorDomain::itemIconLooks);

    private CargoIconLooks() {
    }

    public static CargoIconLook forMaterial(String itemId) {
        return forMaterial(MaterialItems.tier(itemId), MaterialItems.color(itemId)).withSwarm(swarmFor(itemId));
    }

    static SwarmMotion swarmFor(String itemId) {
        if (CraftingWard.byItemId(itemId) != null) {
            return SwarmMotion.CONTAINED;
        }
        ItemCurrency currency = ItemCurrencies.byItemId(itemId);
        if (currency == null) {
            return SwarmMotion.SLURRY;
        }
        if (currency == CraftingCurrency.CORRUPTION) {
            return SwarmMotion.FERAL;
        }
        return currency == CraftingCurrency.REPLICATION ? SwarmMotion.REPLICATING : SwarmMotion.ORBIT;
    }

    static CargoIconLook forMaterial(MaterialTier tier, Color color) {
        return switch (tier) {
            case BASIC -> CargoIconLook.plain(color);
            case MINOR -> CargoIconLook.glowing(color, 0.45f);
            case MAJOR -> CargoIconLook.ringed(color, 0.55f);
            case EXOTIC -> CargoIconLook.charged(color, 0.65f);
            case MYTHIC -> CargoIconLook.refracting(color, 0.75f);
        };
    }

    public static CargoIconLook forItem(Item item) {
        if (item == null) {
            return null;
        }
        for (ItemIconLooks itemLooks : ITEM_LOOKS.all()) {
            CargoIconLook ownLook = itemLooks.ownLook(item);
            if (ownLook != null) {
                return ownLook;
            }
        }
        ItemRarity rarity = item.rarity();
        CargoIconLook look = switch (rarity) {
            case COMMON -> CargoIconLook.outlined(rarity.color());
            case RARE -> CargoIconLook.glowing(rarity.color(), 0.35f);
            case UNIQUE -> CargoIconLook.charged(rarity.color(), 0.5f);
        };
        if (item.mirrored()) {
            look = CargoIconLook.refracting(rarity.color(), look.glow());
        } else if (item.corrupted()) {
            look = CargoIconLook.charged(CraftingCurrency.CORRUPTION.color(), Math.max(CORRUPTED_GLOW, look.glow()));
        }
        for (ItemIconLooks itemLooks : ITEM_LOOKS.all()) {
            look = itemLooks.decorated(item, look);
        }
        return look;
    }

    public static Color kindColor(ItemKind kind) {
        for (ItemIconLooks itemLooks : ITEM_LOOKS.all()) {
            Color color = itemLooks.kindColor(kind);
            if (color != null) {
                return color;
            }
        }
        return null;
    }

    public static boolean celebratesDrop(CargoIconLook look) {
        return look != null && look.glow() > 0f;
    }
}
