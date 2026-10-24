package exiledlib;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class MockSector implements AutoCloseable {

    public final MockedStatic<Global> global = Mockito.mockStatic(Global.class);
    public final SectorAPI sector = mock(SectorAPI.class);
    public final Map<String, Object> persistentData = new HashMap<>();
    private CampaignFleetAPI playerFleet;
    private CargoAPI cargo;

    public MockSector() {
        global.when(Global::getSector).thenReturn(sector);
        when(sector.getPersistentData()).thenReturn(persistentData);
    }

    public CampaignFleetAPI playerFleet() {
        if (playerFleet == null) {
            playerFleet = mock(CampaignFleetAPI.class);
            when(sector.getPlayerFleet()).thenReturn(playerFleet);
        }
        return playerFleet;
    }

    public CargoAPI cargo() {
        if (cargo == null) {
            cargo = mock(CargoAPI.class);
            when(playerFleet().getCargo()).thenReturn(cargo);
        }
        return cargo;
    }

    @Override
    public void close() {
        global.close();
    }
}
