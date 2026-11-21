package exiledsector.effects;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.ExiledSector;
import exiledsector.PlayerFleet;
import exiledsector.compat.SecretsOfTheFrontierCompat;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.EncounterSalvage;
import exiledsector.loot.FleetDifficulties;
import exiledsector.loot.LootSources;
import exiledsector.skills.skilleffect.logistics.ForgeSupplies;
import exiledsector.skills.skilleffect.logistics.SalvagedArmaments;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ItemLootListener extends BaseCampaignEventListener implements ShipRecoveryListener {

    private static final class Pending {
        private final BattleLoot battleLoot;
        private float defeatedDeploymentPoints;

        Pending(BattleLoot battleLoot) {
            this.battleLoot = battleLoot;
        }
    }

    private final Map<BattleAPI, Pending> pendingByBattle = new IdentityHashMap<>();
    private final Random lootRandom;
    private final Random chartRandom;

    public ItemLootListener() {
        this(new Random());
    }

    ItemLootListener(Random lootRandom) {
        this(lootRandom, new Random());
    }

    ItemLootListener(Random lootRandom, Random chartRandom) {
        super(false);
        this.lootRandom = lootRandom;
        this.chartRandom = chartRandom;
    }

    void holdLoot(PlayerEngagement engagement) {
        EngagementResultAPI engagementResult = engagement.result();
        if (engagementResult.getBattle() == null || SecretsOfTheFrontierCompat.inFlashbackEncounter()) {
            return;
        }
        Pending pendingLoot = pendingByBattle.computeIfAbsent(engagementResult.getBattle(),
                key -> new Pending(new BattleLoot(lootRandom, chartRandom)));
        pendingLoot.battleLoot.setPlayerWon(engagement.playerWon());
        if (!engagement.playerWon()) {
            return;
        }
        float difficultyMultiplier = FleetDifficulties.difficultyMultiplier(engagement.difficulty());
        float defeatedDp = engagement.enemyDeploymentPointsDefeated();
        pendingLoot.defeatedDeploymentPoints += defeatedDp;
        record(pendingLoot, new BattleEngagement(engagementResult.getBattle(), difficultyMultiplier, defeatedDp, engagement.enemyLosses()));
    }

    private static void record(Pending pendingLoot, BattleEngagement battleEngagement) {
        LootSources.all().forEach(source -> source.recordEngagement(pendingLoot.battleLoot, battleEngagement));
        pendingLoot.battleLoot.losses().record(battleEngagement, LootSources::fleetLootRules);
    }

    public static void addUnfoughtBattleLoot(CargoAPI loot, CampaignFleetAPI fleet, Random random) {
        if (loot == null || fleet == null || fleet.getFleetData() == null) {
            return;
        }
        Pending pendingLoot = new Pending(new BattleLoot(random, new Random(random.nextLong())));
        pendingLoot.battleLoot.setPlayerWon(true);
        pendingLoot.battleLoot.markUnfought();
        pendingLoot.battleLoot.setEncounterSalvage(EncounterSalvage.NO_WRECKS);
        record(pendingLoot, BattleEngagement.unfought(fleet, fleet.getFleetData().getMembersListCopy()));
        addEncounterLoot(pendingLoot, loot);
    }

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> recoveredShips, InteractionDialogAPI dialog) {
        ExiledSector.domains().forEach(domain -> domain.fleetChanged(PlayerFleet.orNull()));
    }

    @Override
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin encounterPlugin, CargoAPI loot) {
        if (SecretsOfTheFrontierCompat.isFlashbackEncounter(encounterPlugin)) {
            return;
        }
        SalvagedArmaments.addExtra(PlayerFleet.orNull(), loot, lootRandom);
        Pending pendingLoot = encounterPlugin == null ? null : pendingByBattle.remove(encounterPlugin.getBattle());
        if (pendingLoot != null && loot != null) {
            pendingLoot.battleLoot.setEncounterSalvage(EncounterSalvage.of(encounterPlugin));
            addEncounterLoot(pendingLoot, loot);
            ForgeSupplies.addExtra(PlayerFleet.orNull(), loot, pendingLoot.defeatedDeploymentPoints, lootRandom);
        }
        pendingByBattle.keySet().removeIf(BattleAPI::isDone);
    }

    private static void addEncounterLoot(Pending pendingLoot, CargoAPI loot) {
        LootSources.all().forEach(source -> source.addEncounterLoot(pendingLoot.battleLoot, loot));
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        Pending pendingLoot = pendingByBattle.remove(battle);
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (pendingLoot == null || !pendingLoot.battleLoot.playerWon() || playerFleet == null) {
            return;
        }
        LootSources.all().forEach(source -> source.deliverBattleLoot(pendingLoot.battleLoot, playerFleet.getCargo()));
        ForgeSupplies.addExtra(playerFleet, playerFleet.getCargo(), pendingLoot.defeatedDeploymentPoints, lootRandom);
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        pendingByBattle.keySet().removeIf(battle -> battle.isDone() || !battle.isPlayerInvolved());
    }
}
