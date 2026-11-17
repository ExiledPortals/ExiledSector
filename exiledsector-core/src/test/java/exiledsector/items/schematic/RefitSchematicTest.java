package exiledsector.items.schematic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefitSchematicTest {

    @Test
    void eachTierSellsForOneThousandMoreThanTheLast() {
        assertEquals(1000, RefitSchematic.SCRAPPED.price());
        assertEquals(2000, RefitSchematic.STANDARD.price());
        assertEquals(3000, RefitSchematic.MILITARY.price());
        assertEquals(4000, RefitSchematic.DOMAIN.price());
    }
}
