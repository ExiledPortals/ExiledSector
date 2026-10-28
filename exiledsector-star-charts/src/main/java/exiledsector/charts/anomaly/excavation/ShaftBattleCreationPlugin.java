package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;

import java.awt.Color;
import java.util.List;

public class ShaftBattleCreationPlugin implements BattleCreationPlugin {

    static final float MAP_WIDTH = 10000f;
    static final float MAP_HEIGHT = 10000f;
    static final float EDGE_INSET = 600f;
    static final float EDGE_FIELD_WIDTH = 1200f;
    static final float ROCK_MIN_SPEED = 5f;
    static final float ROCK_MAX_SPEED = 20f;
    static final int ROCKS_PER_EDGE = 40;
    static final float STANDOFF_RANGE = 4000f;
    static final float INITIAL_BURN_SECONDS = 1.5f;
    static final float NORMAL_BURN_SECONDS = 6f;
    static final float ESCAPE_BURN_SECONDS = 1.5f;
    static final Color CAVERN_TINT = new Color(70, 60, 50);
    static final String PLAYER_SHIP_PREFIX = "ISS";

    @Override
    public void initBattle(BattleCreationContext context, MissionDefinitionAPI loader) {
        CampaignFleetAPI playerFleet = context.getPlayerFleet();
        CampaignFleetAPI otherFleet = context.getOtherFleet();
        FleetGoal playerGoal = context.getPlayerGoal();
        FleetGoal otherGoal = context.getOtherGoal();
        int baseCommandPoints = (int) Global.getSettings().getFloat("startingCommandPoints");
        loader.initFleet(FleetSide.PLAYER, PLAYER_SHIP_PREFIX, playerGoal, false, context.getPlayerCommandPoints() - baseCommandPoints,
                (int) playerFleet.getCommanderStats().getCommandPoints().getModifiedValue() - baseCommandPoints);
        loader.initFleet(FleetSide.ENEMY, "", otherGoal, true,
                (int) otherFleet.getCommanderStats().getCommandPoints().getModifiedValue() - baseCommandPoints);
        for (FleetMemberAPI member : shaftShips(playerFleet, playerGoal == FleetGoal.ESCAPE)) {
            loader.addFleetMember(FleetSide.PLAYER, member);
        }
        for (FleetMemberAPI member : shaftShips(otherFleet, otherGoal == FleetGoal.ESCAPE)) {
            loader.addFleetMember(FleetSide.ENEMY, member);
        }
        createCavern(loader, playerFleet.getContainingLocation());
        loader.addPlugin(new ShaftBattlePlugin());
        context.setInitialDeploymentBurnDuration(INITIAL_BURN_SECONDS);
        context.setNormalDeploymentBurnDuration(NORMAL_BURN_SECONDS);
        context.setEscapeDeploymentBurnDuration(ESCAPE_BURN_SECONDS);
        if (playerGoal == FleetGoal.ESCAPE || otherGoal == FleetGoal.ESCAPE) {
            context.setInitialEscapeRange(Global.getSettings().getFloat("escapeStartDistance"));
            context.setFlankDeploymentDistance(Global.getSettings().getFloat("escapeFlankDistance"));
        } else {
            context.setStandoffRange(STANDOFF_RANGE);
            context.setFlankDeploymentDistance(MAP_HEIGHT / 2f);
        }
    }

    static List<FleetMemberAPI> shaftShips(CampaignFleetAPI fleet, boolean escaping) {
        List<FleetMemberAPI> members = escaping ? fleet.getFleetData().getMembersListCopy() : fleet.getFleetData().getCombatReadyMembersListCopy();
        return members.stream().filter(member -> !ShaftBattlePlugin.holdsAbove(member)).toList();
    }

    static void createCavern(MissionDefinitionAPI loader, LocationAPI location) {
        float halfWidth = MAP_WIDTH / 2f;
        float halfHeight = MAP_HEIGHT / 2f;
        loader.initMap(-halfWidth, halfWidth, -halfHeight, halfHeight);
        if (location != null && location.getBackgroundTextureFilename() != null) {
            loader.setBackgroundSpriteName(location.getBackgroundTextureFilename());
        }
        loader.setHyperspaceMode(false);
        loader.addAsteroidField(0f, halfHeight - EDGE_INSET, 0f, EDGE_FIELD_WIDTH, ROCK_MIN_SPEED, ROCK_MAX_SPEED, ROCKS_PER_EDGE);
        loader.addAsteroidField(0f, EDGE_INSET - halfHeight, 0f, EDGE_FIELD_WIDTH, ROCK_MIN_SPEED, ROCK_MAX_SPEED, ROCKS_PER_EDGE);
        loader.addAsteroidField(halfWidth - EDGE_INSET, 0f, 90f, EDGE_FIELD_WIDTH, ROCK_MIN_SPEED, ROCK_MAX_SPEED, ROCKS_PER_EDGE);
        loader.addAsteroidField(EDGE_INSET - halfWidth, 0f, 90f, EDGE_FIELD_WIDTH, ROCK_MIN_SPEED, ROCK_MAX_SPEED, ROCKS_PER_EDGE);
    }

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        engine.setBackgroundColor(CAVERN_TINT);
        engine.setRenderStarfield(false);
    }
}
