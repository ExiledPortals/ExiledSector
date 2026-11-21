package exiledsector.ui.cargo;

import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.items.Item;
import exiledsector.items.ItemKind;
import exiledsector.items.crafting.ItemDisassembly;
import exiledsector.items.crafting.material.CraftingInhibitor;
import exiledsector.items.crafting.material.CraftingMaterialFamilies;
import exiledsector.items.crafting.material.CraftingMaterialFamily;
import exiledsector.items.crafting.material.MaterialItems;
import exiledsector.items.crafting.material.MaterialTier;
import exiledsector.items.crafting.material.Nanoswarm;
import exiledsector.items.modifier.ItemRarity;

import java.awt.Color;

public final class CargoIconLooks {

    private static final float CORRUPTED_GLOW = 0.45f;
    public static final Color WARPED_BORDER = new Color(170, 90, 255);
    private static final DomainContributions<ItemIconLooks> ITEM_LOOKS = DomainContributions.fromEveryDomain(ExiledSectorDomain::itemIconLooks);

    private CargoIconLooks() {
    }

    public static CargoIconLook forMaterial(String itemId) {
        CargoIconLook domainLook = ITEM_LOOKS.first(itemLooks -> itemLooks.materialLook(itemId));
        if (domainLook != null) {
            return domainLook;
        }
        return forMaterial(MaterialItems.tier(itemId), MaterialItems.color(itemId)).withSwarm(swarmFor(itemId));
    }

    static SwarmMotion swarmFor(String itemId) {
        if (CraftingInhibitor.byItemId(itemId) != null) {
            return SwarmMotion.CONTAINED;
        }
        CraftingMaterialFamily craftingItem = CraftingMaterialFamilies.byItemId(itemId);
        if (craftingItem == null) {
            return ItemDisassembly.FEEDSTOCK_ITEM_ID.equals(itemId) ? SwarmMotion.ORBIT : SwarmMotion.SLURRY;
        }
        if (craftingItem == Nanoswarm.CORRUPTION) {
            return SwarmMotion.FERAL;
        }
        return craftingItem == Nanoswarm.REPLICATION ? SwarmMotion.REPLICATING : SwarmMotion.ORBIT;
    }

    public static CargoIconLook forMaterial(MaterialTier tier, Color color) {
        return switch (tier) {
            case COMMON -> CargoIconLook.glowing(color, 0.45f);
            case RARE -> CargoIconLook.ringed(color, 0.55f);
            case UNIQUE -> CargoIconLook.charged(color, 0.65f);
            case MYTHIC -> CargoIconLook.refracting(color, 0.75f);
        };
    }

    public static CargoIconLook forItem(Item item) {
        if (item == null) {
            return null;
        }
        CargoIconLook ownLook = ITEM_LOOKS.first(itemLooks -> itemLooks.ownLook(item));
        if (ownLook != null) {
            return ownLook;
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
            look = CargoIconLook.charged(Nanoswarm.CORRUPTION.color(), Math.max(CORRUPTED_GLOW, look.glow()));
        }
        for (ItemIconLooks itemLooks : ITEM_LOOKS.all()) {
            look = itemLooks.decorated(item, look);
        }
        return item.isWarped() && !look.ownArt() ? look.withChargedBorder(WARPED_BORDER) : look;
    }

    public static Color kindColor(ItemKind kind) {
        return ITEM_LOOKS.first(itemLooks -> itemLooks.kindColor(kind));
    }

    public static boolean celebratesDrop(CargoIconLook look) {
        return look != null && look.glow() > 0f;
    }
}
