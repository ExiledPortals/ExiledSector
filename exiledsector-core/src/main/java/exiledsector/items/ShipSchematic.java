package exiledsector.items;

import com.fs.starfarer.api.campaign.SpecialItemData;

public interface ShipSchematic {

    SpecialItemData toSpecialItem();

    String displayName();
}
