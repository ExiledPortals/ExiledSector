package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.FleetAdvanceScript;

import java.util.function.Supplier;

public final class HeldGuards {

    private HeldGuards() {
    }

    public static CampaignFleetAPI held(SectorEntityToken site, String memoryKey, Supplier<CampaignFleetAPI> create) {
        MemoryAPI memory = site.getMemoryWithoutUpdate();
        CampaignFleetAPI guards = memory.get(memoryKey) instanceof CampaignFleetAPI stored && !stored.isEmpty() ? stored : null;
        if (guards == null) {
            guards = create.get();
            if (guards == null) {
                return null;
            }
            memory.set(memoryKey, guards);
        }
        guards.setDoNotAdvanceAI(true);
        guards.setContainingLocation(site.getContainingLocation());
        guards.setLocation(site.getLocation().x, site.getLocation().y);
        if (!site.hasScriptOfClass(FleetAdvanceScript.class)) {
            site.addScript(new FleetAdvanceScript(guards));
        }
        return guards;
    }

    public static void forget(SectorEntityToken site, String memoryKey) {
        site.getMemoryWithoutUpdate().unset(memoryKey);
        site.removeScriptsOfClass(FleetAdvanceScript.class);
    }
}
