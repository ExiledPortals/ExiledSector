package exiledsector.items.custody;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.PlayerFleet;

public interface CargoRefund {

    void refundTo(CargoAPI cargo);

    default void refundToPlayer() {
        refundTo(PlayerFleet.cargoOrNull());
    }
}
