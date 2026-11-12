package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.OfficerRewardDrop;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.OfficerRewardText;
import exiledsector.charts.officer.reward.RewardContext;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDrops;
import exiledsector.items.MaterialItems;
import exiledsector.ui.atlas.OfficerRewardPicker;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Random;

public class OfficerRewardPickerCommand extends PlayerFleetCommand {

    static final int DEFAULT_TIER = ChartTiers.LOWEST_TIER;

    private final Random random;

    public OfficerRewardPickerCommand() {
        this(new Random());
    }

    OfficerRewardPickerCommand(Random random) {
        this.random = random;
    }

    static Integer tier(String args) {
        List<String> tokens = ConsoleArgs.tokens(args);
        if (tokens.isEmpty()) {
            return DEFAULT_TIER;
        }
        return tokens.size() == 1 ? ConsoleArgs.wholeNumber(tokens.get(0), ChartTiers.LOWEST_TIER, ChartTiers.HIGHEST_TIER) : null;
    }

    @Override
    protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
        Integer tier = tier(args);
        if (tier == null) {
            Console.showMessage("Usage: ExiledSectorOfficerRewards [tier], tier " + ChartTiers.LOWEST_TIER + " to " + ChartTiers.HIGHEST_TIER + ".");
            return CommandResult.BAD_SYNTAX;
        }
        SectorAPI sector = Global.getSector();
        List<CarriedItem> offers = OfficerRewardPool.offers(RewardContext.forSector(sector, tier, null, null), random);
        if (offers.isEmpty()) {
            Console.showMessage("No reward can be offered at tier " + tier + ".");
            return CommandResult.ERROR;
        }
        OfficerRewardPicker.openWhenClear(sector, null, offers, chosen -> grant(playerFleet, chosen, tier, random));
        Console.showMessage("The officer reward picker opens at tier " + tier + " once the console closes.");
        return CommandResult.SUCCESS;
    }

    private static void grant(CampaignFleetAPI playerFleet, CarriedItem chosen, int tier, Random random) {
        ItemDrops.SiteLoot drop = OfficerRewardDrop.roll(List.of(chosen), tier, ChartGenContext.none(), random);
        for (ItemData item : drop.items()) {
            playerFleet.getCargo().addSpecial(item.toSpecialItem(), 1f);
        }
        drop.materials().forEach((itemId, quantity) -> MaterialItems.add(playerFleet.getCargo(), itemId, quantity));
        Global.getSector().getCampaignUI().addMessage("Picked " + OfficerRewardText.name(chosen) + " (" + chosen.toToken() + ").");
    }
}
