package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import exiledsector.skills.unlock.GateTriggers;
import exiledsector.skills.unlock.UnlockConditionType;
import org.apache.log4j.Logger;

import java.util.List;

public final class AbyssSingularity {

    static final String DEFEATED_FLAG = "$exiledSector_singularityDefeated";
    static final String ABYSS_WRECK_TAG = "rat_abyss_wreck";
    private static final Logger LOG = Logger.getLogger(AbyssSingularity.class);
    private static volatile Boolean genesisHullKnown;

    private AbyssSingularity() {
    }

    public static boolean isDefeated(SectorAPI sector) {
        return sector != null && sector.getMemoryWithoutUpdate().getBoolean(DEFEATED_FLAG);
    }

    public static boolean isGateOpen(SectorAPI sector) {
        return isDefeated(sector) || !canDetectDefeat();
    }

    static boolean canDetectDefeat() {
        Boolean known = genesisHullKnown;
        if (known == null) {
            known = CompatTarget.specExists(() -> Global.getSettings().getHullSpec(RandomAssortmentCompat.GENESIS_HULL_ID));
            genesisHullKnown = known;
            if (!known) {
                LOG.warn("[ExiledSector] Random Assortment of Things has no " + RandomAssortmentCompat.GENESIS_HULL_ID
                        + " hull, so the Singularity win can't be detected; the Abyss unlocks so its area stays usable.");
            }
        }
        return known;
    }

    static void clearCachedDetection() {
        genesisHullKnown = null;
    }

    public static void recordBattle(SectorAPI sector, EngagementResultAPI result) {
        if (sector == null || result == null || !result.didPlayerWin()) {
            return;
        }
        if (!GateTriggers.awaits(sector, UnlockConditionType.RAT_SINGULARITY_DEFEATED, isDefeated(sector))) {
            return;
        }
        EngagementResultForFleetAPI losers = result.getLoserResult();
        if (losers != null && (containsGenesis(losers.getDestroyed()) || containsGenesis(losers.getDisabled()))) {
            markDefeated(sector, "the player destroyed the Singularity's Genesis-class ship");
        }
    }

    public static void checkSaveOnLoad(SectorAPI sector) {
        if (sector == null || isDefeated(sector)) {
            return;
        }
        if (playerHasRequiem(sector)) {
            markDefeated(sector, "the player already has the Abyssal Requiem skill RAT grants for beating the Singularity");
        } else if (genesisWreckExists(sector)) {
            markDefeated(sector, "the Genesis-class wreck RAT leaves after the Singularity battle is in the sector");
        }
    }

    private static void markDefeated(SectorAPI sector, String reason) {
        GateTriggers.record(sector, UnlockConditionType.RAT_SINGULARITY_DEFEATED, sector.getMemoryWithoutUpdate(), DEFEATED_FLAG,
                "Unlocking the Abyss area: " + reason);
    }

    public static boolean containsGenesis(List<FleetMemberAPI> members) {
        if (members == null) {
            return false;
        }
        for (FleetMemberAPI member : members) {
            if (member != null && isGenesisHull(member.getHullSpec())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGenesisHull(ShipHullSpecAPI hullSpec) {
        return hullSpec != null && (RandomAssortmentCompat.GENESIS_HULL_ID.equals(hullSpec.getHullId())
                || RandomAssortmentCompat.GENESIS_HULL_ID.equals(hullSpec.getBaseHullId()));
    }

    private static boolean playerHasRequiem(SectorAPI sector) {
        PersonAPI player = sector.getPlayerPerson();
        return player != null && player.getStats() != null && player.getStats().getSkillLevel(RandomAssortmentCompat.REQUIEM_SKILL_ID) > 0f;
    }

    private static boolean genesisWreckExists(SectorAPI sector) {
        for (StarSystemAPI system : sector.getStarSystems()) {
            for (CustomCampaignEntityAPI entity : system.getCustomEntities()) {
                if (entity.hasTag(ABYSS_WRECK_TAG) && isGenesisWreck(entity)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isGenesisWreck(CustomCampaignEntityAPI entity) {
        if (!(entity.getCustomPlugin() instanceof DerelictShipEntityPlugin derelict) || derelict.getData() == null
                || derelict.getData().ship == null) {
            return false;
        }
        ShipRecoverySpecial.PerShipData ship = derelict.getData().ship;
        if (RandomAssortmentCompat.GENESIS_WRECK_VARIANT_ID.equals(ship.variantId)) {
            return true;
        }
        try {
            ShipVariantAPI variant = ship.getVariant();
            return variant != null && isGenesisHull(variant.getHullSpec());
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static final class BattleListener extends BaseCampaignEventListener {

        public BattleListener() {
            super(false);
        }

        @Override
        public void reportPlayerEngagement(EngagementResultAPI result) {
            if (RandomAssortmentCompat.isModEnabled()) {
                recordBattle(Global.getSector(), result);
            }
        }
    }
}
