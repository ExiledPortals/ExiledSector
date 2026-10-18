package exiledsector.ui.cargo;

import exiledsector.items.CraftingCurrency;
import exiledsector.items.CraftingWard;
import exiledsector.items.HullItem;
import exiledsector.items.Item;
import exiledsector.items.ItemRarity;
import exiledsector.items.MaterialItems;
import exiledsector.items.MaterialTier;
import exiledsector.ui.section.SectionFlair;

import java.awt.Color;

public final class CargoIconLooks {

    private static final float CORRUPTED_GLOW = 0.45f;

    private CargoIconLooks() {
    }

    public static CargoIconLook forMaterial(String itemId) {
        return forMaterial(MaterialItems.tier(itemId), MaterialItems.color(itemId)).withSwarm(swarmFor(itemId));
    }

    static SwarmMotion swarmFor(String itemId) {
        if (CraftingWard.byItemId(itemId) != null) {
            return SwarmMotion.CONTAINED;
        }
        CraftingCurrency currency = CraftingCurrency.byItemId(itemId);
        if (currency == null) {
            return SwarmMotion.SLURRY;
        }
        return switch (currency) {
            case CORRUPTION -> SwarmMotion.FERAL;
            case REPLICATION -> SwarmMotion.REPLICATING;
            default -> SwarmMotion.ORBIT;
        };
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
        return item instanceof HullItem hullItem && hullItem.section() != null
                ? look.withBadge(SectionFlair.colorOf(hullItem.section())) : look;
    }

    public static boolean celebratesDrop(CargoIconLook look) {
        return look != null && look.glow() > 0f;
    }
}
