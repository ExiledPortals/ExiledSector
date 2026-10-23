package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledlib.i18n.Translation;
import exiledlib.skills.ShipHullSizes;

import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public enum RefitSchematic {

    SCRAPPED(2, 60f, new Color(170, 170, 170)),
    STANDARD(3, 28f, ItemRarity.COMMON.color()),
    MILITARY(4, 10f, ItemRarity.RARE.color()),
    DOMAIN(5, 2f, ItemRarity.UNIQUE.color());

    public static final String ITEM_ID = "exiledSector_hull_upgrade";
    private static final String ICON_FOLDER = "graphics/icons/hull_upgrades/refit_schematic_";
    private static final List<RefitSchematic> TIERS = List.of(values());
    private static final AtomicReference<BooleanSupplier> DROPS_ALLOWED = new AtomicReference<>(() -> true);

    private final int points;
    private final float dropWeight;
    private final Color color;

    RefitSchematic(int points, float dropWeight, Color color) {
        this.points = points;
        this.dropWeight = dropWeight;
        this.color = color;
    }

    public static RefitSchematic parse(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        String trimmed = encoded.trim();
        for (RefitSchematic schematic : TIERS) {
            if (schematic.name().equalsIgnoreCase(trimmed)) {
                return schematic;
            }
        }
        return isLegacyData(trimmed) ? STANDARD : null;
    }

    public static boolean isLegacyData(String encoded) {
        return encoded != null && ShipHullSizes.byName(encoded.trim()) != null;
    }

    public static RefitSchematic of(SpecialItemData special) {
        return special != null && ITEM_ID.equals(special.getId()) ? parse(special.getData()) : null;
    }

    public static RefitSchematic forShipPoints(int shipPoints) {
        for (RefitSchematic schematic : TIERS) {
            if (schematic.requiredPoints() == shipPoints) {
                return schematic;
            }
        }
        return null;
    }

    public static Integer absorbInto(ItemStore store, CargoAPI cargo, CargoStackAPI stack) {
        RefitSchematic schematic = of(stack.getSpecialDataIfSpecial());
        if (schematic == null) {
            return null;
        }
        int stackCount = Math.round(stack.getSize());
        if (stackCount < 1) {
            return 0;
        }
        store.addSchematics(schematic, stackCount);
        cargo.removeStack(stack);
        return stackCount;
    }

    public static RefitSchematic roll(Random random) {
        return WeightedPick.pick(TIERS, RefitSchematic::dropWeight, random);
    }

    public static void dropOnlyWhen(BooleanSupplier dropsAllowed) {
        DROPS_ALLOWED.set(dropsAllowed == null ? () -> true : dropsAllowed);
    }

    public static boolean dropsAllowed() {
        return DROPS_ALLOWED.get().getAsBoolean();
    }

    public int points() {
        return points;
    }

    public int requiredPoints() {
        return points - 1;
    }

    public float dropWeight() {
        return dropWeight;
    }

    public Color color() {
        return color;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String encode() {
        return name();
    }

    public SpecialItemData toSpecialItem() {
        return new SpecialItemData(ITEM_ID, encode());
    }

    public String iconPath() {
        return ICON_FOLDER + key() + ".png";
    }

    public String displayName() {
        return switch (this) {
            case SCRAPPED -> Translation.text("schematic.scrapped.name");
            case STANDARD -> Translation.text("schematic.standard.name");
            case MILITARY -> Translation.text("schematic.military.name");
            case DOMAIN -> Translation.text("schematic.domain.name");
        };
    }
}
