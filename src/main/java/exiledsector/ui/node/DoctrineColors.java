package exiledsector.ui.node;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.items.DoctrineArchive;
import exiledsector.items.DoctrineArchives;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public final class DoctrineColors {

    private static final Map<String, Color> BY_FACTION_ID = new HashMap<>();

    private DoctrineColors() {
    }

    public static Color forDoctrine(String doctrineDefinitionId) {
        DoctrineArchive archive = doctrineDefinitionId == null ? null : DoctrineArchives.get(doctrineDefinitionId);
        return archive == null ? null : forFaction(archive.factionId());
    }

    public static Color forFaction(String factionId) {
        if (factionId == null) {
            return null;
        }
        Color cached = BY_FACTION_ID.get(factionId);
        if (cached != null) {
            return cached;
        }
        SectorAPI sector = Global.getSector();
        FactionAPI faction = sector == null ? null : sector.getFaction(factionId);
        Color color = faction == null ? null : faction.getBaseUIColor();
        if (color != null) {
            BY_FACTION_ID.put(factionId, color);
        }
        return color;
    }
}
