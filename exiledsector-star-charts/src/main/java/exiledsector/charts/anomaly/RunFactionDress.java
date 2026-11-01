package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import java.util.List;

public record RunFactionDress(String likenessFactionId, String name, String article, String isOrAre, RepLevel towardsPlayer, boolean neutralToAll) {

    public static final String ALLY_FACTION_ID = "exiledSector_skirmish_ally";

    public static RunFactionDress alliesOf(String factionId) {
        return new RunFactionDress(factionId, null, null, null, RepLevel.FRIENDLY, false);
    }

    public void apply(FactionAPI runFaction, FactionAPI likeness, List<FactionAPI> allFactions) {
        runFaction.setDisplayNameOverride(name != null || likeness == null ? name : likeness.getDisplayName());
        runFaction.setDisplayNameWithArticleOverride(article != null || likeness == null ? article : likeness.getDisplayNameWithArticle());
        runFaction.setDisplayIsOrAreOverride(isOrAre != null || likeness == null ? isOrAre : likeness.getDisplayNameIsOrAre());
        if (likeness != null) {
            runFaction.setShipNamePrefixOverride(likeness.getShipNamePrefix());
            runFaction.setFactionCrestOverride(likeness.getCrest());
            runFaction.setFactionLogoOverride(likeness.getLogo());
        }
        runFaction.setRelationship(Factions.PLAYER, towardsPlayer);
        if (!neutralToAll) {
            return;
        }
        for (FactionAPI other : allFactions == null ? List.<FactionAPI>of() : allFactions) {
            if (other != null && other != runFaction && !runFaction.getId().equals(other.getId())) {
                runFaction.setRelationship(other.getId(), RepLevel.NEUTRAL);
            }
        }
    }
}
