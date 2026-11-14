package exiledsector.loot;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.PlayerFleet;
import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class LootTaken {

    private static final Logger LOG = Logger.getLogger(LootTaken.class);
    private static final List<LootTakenListener> CORE_LISTENERS = List.of(new ItemStorageTransfer());
    private static final DomainContributions<LootTakenListener> DOMAIN_LISTENERS = new DomainContributions<>(ExiledSectorDomain::lootTakenListeners);

    private LootTaken() {
    }

    public static void report() {
        CargoAPI playerCargo = PlayerFleet.cargoOrNull();
        if (playerCargo != null) {
            report(playerCargo, listeners());
        }
    }

    static List<LootTakenListener> listeners() {
        List<LootTakenListener> listeners = new ArrayList<>(CORE_LISTENERS);
        listeners.addAll(DOMAIN_LISTENERS.all());
        return listeners;
    }

    static void report(CargoAPI playerCargo, List<LootTakenListener> listeners) {
        for (LootTakenListener listener : listeners) {
            try {
                listener.reportLootTaken(playerCargo);
            } catch (RuntimeException e) {
                LOG.error("[ExiledSector] " + listener.getClass().getSimpleName() + " failed after loot was taken", e);
            }
        }
    }
}
