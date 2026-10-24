package exiledsector.console;

import com.fs.starfarer.api.Global;
import exiledsector.skills.unlock.RelockedGates;
import org.lazywizard.console.Console;

public class UnlockAreasCommand extends CampaignCommand {

    @Override
    protected CommandResult runInCampaign(String args) {
        int reopenedCount = RelockedGates.reopenAll(Global.getSector());
        Console.showMessage(reopenedCount == 0 ? "No area was locked by ExiledSectorLockAreas."
                : "Lifted the lock on " + reopenedCount + " areas. Each one is open again if its normal unlock condition is met.");
        return CommandResult.SUCCESS;
    }
}
