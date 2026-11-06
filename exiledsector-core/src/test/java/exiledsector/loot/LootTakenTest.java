package exiledsector.loot;

import com.fs.starfarer.api.campaign.CargoAPI;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class LootTakenTest {

    @Test
    void everyListenerHearsAboutTheTakenLootEvenIfOneBeforeItFails() {
        CargoAPI playerCargo = mock(CargoAPI.class);
        List<CargoAPI> heard = new ArrayList<>();
        LootTakenListener failing = cargo -> {
            throw new IllegalStateException("broken listener");
        };

        LootTaken.report(playerCargo, List.of(failing, heard::add, heard::add));

        assertEquals(List.of(playerCargo, playerCargo), heard);
    }
}
