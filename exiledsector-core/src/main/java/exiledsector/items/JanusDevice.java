package exiledsector.items;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.apache.log4j.Logger;

public final class JanusDevice {

    public static final String FLAG = "$playerCanUseGates";
    private static final Logger LOG = Logger.getLogger(JanusDevice.class);

    private JanusDevice() {
    }

    public static boolean obtained(SectorAPI sector) {
        try {
            MemoryAPI sectorMemory = sector == null ? null : sector.getMemoryWithoutUpdate();
            return sectorMemory != null && sectorMemory.getBoolean(FLAG);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Failed to check " + FLAG + "; treating the Janus Device as not obtained", e);
            return false;
        }
    }
}
