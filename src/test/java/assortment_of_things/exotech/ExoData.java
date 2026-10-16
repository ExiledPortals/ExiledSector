package assortment_of_things.exotech;

import com.fs.starfarer.api.campaign.SectorEntityToken;

public class ExoData {

    private boolean recoveredExoship;
    private SectorEntityToken playerExoship;

    public boolean getRecoveredExoship() {
        return recoveredExoship;
    }

    public void setRecoveredExoship(boolean recovered) {
        recoveredExoship = recovered;
    }

    public SectorEntityToken getPlayerExoship() {
        return playerExoship;
    }

    public void setPlayerExoship(SectorEntityToken exoship) {
        playerExoship = exoship;
    }
}
