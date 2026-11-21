package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;

import java.util.List;

final class SunstruckBattleCreationPlugin extends BattleCreationPluginImpl {

    private final List<CampaignFleetAPI> sunstruckFleets;

    SunstruckBattleCreationPlugin(List<CampaignFleetAPI> sunstruckFleets) {
        this.sunstruckFleets = List.copyOf(sunstruckFleets);
    }

    @Override
    public void initBattle(BattleCreationContext context, MissionDefinitionAPI loader) {
        sunstruckFleets.forEach(ConvoySunstruck::wearDown);
        super.initBattle(context, loader);
    }
}
