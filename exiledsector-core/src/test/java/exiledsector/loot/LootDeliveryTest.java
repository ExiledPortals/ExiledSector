package exiledsector.loot;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.items.ItemData;
import exiledsector.items.schematic.ShipSchematic;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LootDeliveryTest {

    @Test
    void deliveringWhereFoundAddsItemsThenSchematicsThenMaterialsAndAnnouncesThemInThatOrder() {
        CargoAPI cargo = mock(CargoAPI.class);
        ItemData item = mock(ItemData.class);
        SpecialItemData itemData = new SpecialItemData("item", "data");
        when(item.toSpecialItem()).thenReturn(itemData);
        ShipSchematic schematic = mock(ShipSchematic.class);
        SpecialItemData schematicData = new SpecialItemData("schematic", null);
        when(schematic.toSpecialItem()).thenReturn(schematicData);
        Loot loot = new Loot(List.of(item), List.of(schematic), Map.of("feedstock", 3));
        LootDelivery.Announcer announcer = mock(LootDelivery.Announcer.class);

        LootDelivery.deliverFoundAt(cargo, loot, null, announcer);

        InOrder order = inOrder(cargo, announcer);
        order.verify(cargo).addSpecial(itemData, 1f);
        order.verify(cargo).addSpecial(schematicData, 1f);
        order.verify(cargo).addSpecial(argThat(data -> data != null && "feedstock".equals(data.getId())), eq(3f));
        order.verify(announcer).announceItems(loot.items());
        order.verify(announcer).announceSchematics(loot.schematics());
        order.verify(announcer).announceMaterials(loot.materials());
    }

    @Test
    void announcingEachFindNamesEveryPreviewedItemAndSchematicAndHandsOverTheMaterials() {
        ItemData unpreviewed = mock(ItemData.class);
        ShipSchematic schematic = mock(ShipSchematic.class);
        when(schematic.displayName()).thenReturn("Refit Schematic");
        List<String> announcedNames = new ArrayList<>();
        List<Map<String, Integer>> announcedMaterials = new ArrayList<>();
        LootDelivery.Announcer announcer = LootDelivery.eachFind(name -> announcedNames.add(name.get()),
                announcedMaterials::add);

        announcer.announceItems(List.of(unpreviewed));
        announcer.announceSchematics(List.of(schematic));
        announcer.announceMaterials(Map.of("feedstock", 3));

        assertEquals(List.of("Refit Schematic"), announcedNames);
        assertEquals(List.of(Map.of("feedstock", 3)), announcedMaterials);
    }
}
