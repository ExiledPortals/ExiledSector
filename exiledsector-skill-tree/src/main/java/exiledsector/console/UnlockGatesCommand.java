package exiledsector.console;

import com.fs.starfarer.api.Global;
import exiledsector.skills.unlock.RelockedGates;
import org.lazywizard.console.Console;

public class UnlockGatesCommand extends CampaignCommand {

    @Override
    protected CommandResult runInCampaign(String args) {
        int reopenedCount = RelockedGates.reopenAll(Global.getSector());
        Console.showMessage(reopenedCount == 0 ? "No gate was locked by ExiledSectorLockGates."
                : "Lifted the lock on " + reopenedCount + " gates. Each one is open again if its normal unlock condition is met.");
        return CommandResult.SUCCESS;
    }
}
