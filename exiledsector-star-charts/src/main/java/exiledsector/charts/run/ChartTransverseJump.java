package exiledsector.charts.run;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.impl.campaign.abilities.FractureJumpAbility;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import org.apache.log4j.Logger;

public final class ChartTransverseJump {

    private static final Logger LOG = Logger.getLogger(ChartTransverseJump.class);
    private static boolean foreignPluginReported;

    private ChartTransverseJump() {
    }

    static boolean install(CampaignFleetAPI fleet) {
        AbilityPlugin current = fleet.getAbility(Abilities.TRANSVERSE_JUMP);
        if (current instanceof ChartTransverseJumpAbility) {
            return true;
        }
        if (current == null) {
            return false;
        }
        if (current.getClass() != FractureJumpAbility.class) {
            reportForeignPlugin(current);
            return false;
        }
        if (current.isActiveOrInProgress()) {
            return false;
        }
        swap(fleet, current, new ChartTransverseJumpAbility());
        return true;
    }

    static boolean isInstalled(CampaignFleetAPI fleet) {
        return fleet != null && fleet.getAbility(Abilities.TRANSVERSE_JUMP) instanceof ChartTransverseJumpAbility;
    }

    static boolean restoreWhenIdle(CampaignFleetAPI fleet) {
        AbilityPlugin current = fleet == null ? null : fleet.getAbility(Abilities.TRANSVERSE_JUMP);
        if (!(current instanceof ChartTransverseJumpAbility)) {
            return true;
        }
        if (current.isActiveOrInProgress()) {
            return false;
        }
        swap(fleet, current, new FractureJumpAbility());
        return true;
    }

    public static void restoreBeforeSave(CampaignFleetAPI fleet) {
        AbilityPlugin current = fleet == null ? null : fleet.getAbility(Abilities.TRANSVERSE_JUMP);
        if (current instanceof ChartTransverseJumpAbility chartJump) {
            chartJump.cancelCharge();
            swap(fleet, current, new FractureJumpAbility());
        }
    }

    private static void reportForeignPlugin(AbilityPlugin current) {
        if (!foreignPluginReported) {
            foreignPluginReported = true;
            LOG.warn("[ExiledSector] Transverse Jump uses " + current.getClass().getName()
                    + " instead of the vanilla plugin, so it stays blocked inside Star Chart systems");
        }
    }

    private static void swap(CampaignFleetAPI fleet, AbilityPlugin from, BaseDurationAbility to) {
        to.init(Abilities.TRANSVERSE_JUMP, fleet);
        to.setCooldownLeft(from.getCooldownLeft());
        fleet.getAbilities().put(Abilities.TRANSVERSE_JUMP, to);
        if (from instanceof EveryFrameScript oldScript) {
            fleet.removeScript(oldScript);
        }
        fleet.addScript(to);
    }
}
