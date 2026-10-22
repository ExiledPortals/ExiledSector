package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import exiledsector.effects.ItemSalvageListener;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.Locale;

public class ShowcaseLootCommand implements BaseCommand {

    static final String SPAWN_ARGUMENT = "spawn";
    static final String SPAWNED_SITE_TYPE = "station_research";
    static final float SPAWN_DISTANCE = 300f;

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        String argument = args.trim().toLowerCase(Locale.ROOT);
        if (!argument.isEmpty() && !argument.equals(SPAWN_ARGUMENT)) {
            return CommandResult.BAD_SYNTAX;
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        LocationAPI location = playerFleet == null ? null : playerFleet.getContainingLocation();
        if (location == null) {
            Console.showMessage("No player fleet found.");
            return CommandResult.ERROR;
        }
        SectorEntityToken site = argument.equals(SPAWN_ARGUMENT) ? spawnSite(playerFleet, location) : nearestSite(playerFleet, location);
        if (site == null) {
            Console.showMessage("No unlooted salvage site with Exiled Sector drops is in this location. Use \"spawn\" to add a research station.");
            return CommandResult.ERROR;
        }
        ItemSalvageListener.markShowcase(site);
        Console.showMessage(site.getName() + " (" + Math.round(Misc.getDistance(playerFleet, site))
                + " su away) will also drop three Doctrine Archives, three wormhole anchors and every crafting material when salvaged.");
        return CommandResult.SUCCESS;
    }

    static SectorEntityToken nearestSite(CampaignFleetAPI playerFleet, LocationAPI location) {
        SectorEntityToken nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (SectorEntityToken entity : location.getCustomEntities()) {
            float distance = Misc.getDistance(playerFleet, entity);
            if (distance < nearestDistance && ItemSalvageListener.canShowcase(entity)) {
                nearest = entity;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    static SectorEntityToken spawnSite(CampaignFleetAPI playerFleet, LocationAPI location) {
        SectorEntityToken site = location.addCustomEntity(null, null, SPAWNED_SITE_TYPE, Factions.NEUTRAL);
        site.setLocation(playerFleet.getLocation().x + SPAWN_DISTANCE, playerFleet.getLocation().y);
        return ItemSalvageListener.canShowcase(site) ? site : null;
    }
}
