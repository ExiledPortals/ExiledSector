package exiledsector.items;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipRecords;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DockedShipCustodyTest {

    @Test
    void aShipDockedInAnOwnedShipsModuleSlotCountsAsOwnedByThatShip() {
        ShipVariantAPI dockedVariant = mock(ShipVariantAPI.class);
        when(dockedVariant.getTags()).thenReturn(List.of(ShipRecords.SHIP_TAG_PREFIX + "docked-core"));
        ShipVariantAPI plainModule = mock(ShipVariantAPI.class);
        when(plainModule.getTags()).thenReturn(List.of());
        ShipVariantAPI carrierVariant = mock(ShipVariantAPI.class);
        when(carrierVariant.getModuleSlots()).thenReturn(List.of("MODULE", "PLATE"));
        when(carrierVariant.getModuleVariant("MODULE")).thenReturn(dockedVariant);
        when(carrierVariant.getModuleVariant("PLATE")).thenReturn(plainModule);
        FleetMemberAPI carrier = mock(FleetMemberAPI.class);
        when(carrier.getVariant()).thenReturn(carrierVariant);
        FleetMemberAPI plain = mock(FleetMemberAPI.class);
        when(plain.getVariant()).thenReturn(mock(ShipVariantAPI.class));
        Map<String, FleetMemberAPI> owned = new LinkedHashMap<>(Map.of("carrier", carrier, "plain", plain));

        SocketCustody.addDockedShips(owned);

        assertEquals(3, owned.size());
        assertSame(carrier, owned.get("docked-core"));
    }

    @Test
    void aDockedShipThatIsAlsoInTheFleetKeepsItsOwnMember() {
        ShipVariantAPI dockedVariant = mock(ShipVariantAPI.class);
        when(dockedVariant.getTags()).thenReturn(List.of(ShipRecords.SHIP_TAG_PREFIX + "core"));
        ShipVariantAPI carrierVariant = mock(ShipVariantAPI.class);
        when(carrierVariant.getModuleSlots()).thenReturn(List.of("MODULE"));
        when(carrierVariant.getModuleVariant("MODULE")).thenReturn(dockedVariant);
        FleetMemberAPI carrier = mock(FleetMemberAPI.class);
        when(carrier.getVariant()).thenReturn(carrierVariant);
        FleetMemberAPI core = mock(FleetMemberAPI.class);
        when(core.getVariant()).thenReturn(mock(ShipVariantAPI.class));
        Map<String, FleetMemberAPI> owned = new LinkedHashMap<>(Map.of("carrier", carrier, "core", core));

        SocketCustody.addDockedShips(owned);

        assertSame(core, owned.get("core"));
    }
}
