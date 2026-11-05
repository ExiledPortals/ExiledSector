package exiledsector.ui.cargo;

import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.items.Item;

import java.awt.Color;

public final class BiomeItemIconLooks implements ItemIconLooks {

    public static final BiomeItemIconLooks INSTANCE = new BiomeItemIconLooks();

    private BiomeItemIconLooks() {
    }

    @Override
    public CargoIconLook decorated(Item item, CargoIconLook look) {
        String biomeId = BiomeModifiers.biomeOf(item);
        if (biomeId == null || look.ownArt()) {
            return look;
        }
        Color biomeColor = AtlasData.regionTint(biomeId);
        return look.withChargedBorder(biomeColor == null ? look.color() : biomeColor);
    }
}
