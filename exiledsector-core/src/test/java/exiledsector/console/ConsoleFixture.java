package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.MockSector;
import exiledsector.items.ItemData;
import org.lazywizard.console.Console;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public final class ConsoleFixture implements AutoCloseable {

    private final MockSector mockSector = new MockSector();
    public final MockedStatic<Global> global = mockSector.global;
    public final MockedStatic<Console> console = Mockito.mockStatic(Console.class);
    public final SectorAPI sector = mockSector.sector;
    public final CampaignFleetAPI fleet = mockSector.playerFleet();
    public final CargoAPI cargo = mockSector.cargo();

    public List<ItemData> granted(int expectedItems) {
        ArgumentCaptor<SpecialItemData> items = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(cargo, times(expectedItems)).addSpecial(items.capture(), Mockito.eq(1f));
        return items.getAllValues().stream().map(ItemData::of).toList();
    }

    public void verifyNothingGranted() {
        verify(cargo, never()).addSpecial(any(), anyFloat());
    }

    @Override
    public void close() {
        console.close();
        mockSector.close();
    }
}
