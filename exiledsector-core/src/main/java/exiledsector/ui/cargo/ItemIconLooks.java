package exiledsector.ui.cargo;

import exiledlib.items.Item;
import exiledlib.items.ItemKind;

import java.awt.Color;

public interface ItemIconLooks {

    default CargoIconLook ownLook(Item item) {
        return null;
    }

    default CargoIconLook decorated(Item item, CargoIconLook look) {
        return look;
    }

    default Color kindColor(ItemKind kind) {
        return null;
    }
}
