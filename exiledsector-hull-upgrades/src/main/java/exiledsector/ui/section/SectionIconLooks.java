package exiledsector.ui.section;

import exiledsector.items.HullItem;
import exiledsector.items.Item;
import exiledsector.items.ItemKind;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.ItemIconLooks;

import java.awt.Color;

public final class SectionIconLooks implements ItemIconLooks {

    public static final SectionIconLooks INSTANCE = new SectionIconLooks();

    private SectionIconLooks() {
    }

    @Override
    public CargoIconLook decorated(Item item, CargoIconLook look) {
        return item instanceof HullItem hullItem && hullItem.section() != null ? look.withBadge(SectionFlair.colorOf(hullItem.section())) : look;
    }

    @Override
    public Color kindColor(ItemKind kind) {
        return kind != null && kind.fitsSection() ? SectionFlair.colorOf(kind) : null;
    }
}
