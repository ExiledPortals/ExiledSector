package exiledsector.ui.cargo;

import exiledsector.items.CraftingCurrency;
import exiledsector.items.CraftingWard;
import exiledsector.items.DoctrineArchive;
import exiledsector.items.DoctrineArchives;
import exiledsector.items.HullItem;
import exiledsector.items.ItemCurrencies;
import exiledsector.items.ItemCurrency;
import exiledsector.items.Item;
import exiledsector.items.ItemRarity;
import exiledsector.items.MaterialItems;
import exiledsector.items.MaterialTier;
import exiledsector.items.Subroutine;
import exiledsector.items.SubroutineKind;
import exiledsector.items.WormholeAnchor;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.ui.node.DoctrineColors;
import exiledsector.ui.section.SectionFlair;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

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
        if (item instanceof WormholeAnchor anchor && !AnimatedItemIcons.isAnimated(anchor.iconPath())) {
            return CargoIconLook.anchor(anchor.color(), new AnchorLook(anchor.crestFactionId()));
        }
        CargoIconLook doctrineLook = forDoctrine(item);
        if (doctrineLook != null) {
            return doctrineLook;
        }
        CargoIconLook cacheLook = forCache(item);
        if (cacheLook != null) {
            return cacheLook;
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

    static CargoIconLook forDoctrine(Item item) {
        if (!(item instanceof Subroutine subroutine) || subroutine.subroutineKind() != SubroutineKind.DOCTRINE
                || !subroutine.iconPath().startsWith(DoctrineIconFx.STILL_PREFIX)) {
            return null;
        }
        DoctrineArchive archive = DoctrineArchives.get(subroutine.definitionId());
        if (archive == null || archive.factionId() == null) {
            return null;
        }
        Color factionColor = DoctrineColors.forFaction(archive.factionId());
        return CargoIconLook.doctrine(factionColor == null ? item.frameColor() : factionColor,
                DoctrineLook.of(DoctrineScreen.forFaction(archive.factionId()), archive.factionId()));
    }

    static CargoIconLook forCache(Item item) {
        if (!(item instanceof Subroutine subroutine) || subroutine.subroutineKind() != SubroutineKind.MODSPEC_CACHE
                || !CacheIconFx.STILL_PATH.equals(subroutine.iconPath())) {
            return null;
        }
        List<String> chipIconPaths = new ArrayList<>();
        for (String nodeId : subroutine.currentNodeIds()) {
            SkillNode node = SkillTree.getDeclared(nodeId);
            if (node != null && node.getIconPath() != null) {
                chipIconPaths.add(node.getIconPath());
            }
        }
        return CargoIconLook.cache(item.frameColor(), new CacheLook(chipIconPaths));
    }

    public static boolean celebratesDrop(CargoIconLook look) {
        return look != null && look.glow() > 0f;
    }
}
