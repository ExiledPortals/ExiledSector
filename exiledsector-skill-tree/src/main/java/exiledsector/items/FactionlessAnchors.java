package exiledsector.items;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledlib.ModCsv;
import exiledsector.compat.NexerelinCompat;
import exiledsector.skills.npc.NpcFactionVolumes;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class FactionlessAnchors {

    static final String DATA_PATH = "data/config/exiledSector/anchor_salvage.csv";
    static final String SITE_COLUMN = "site";
    static final String CHANCE_COLUMN = "chance";
    private static final Logger LOG = Logger.getLogger(FactionlessAnchors.class);
    private static final AtomicReference<Map<String, Float>> CHANCES_BY_SITE = new AtomicReference<>(Map.of());

    private FactionlessAnchors() {
    }

    public static void load() {
        ModCsv.load(SITE_COLUMN, DATA_PATH, LOG, FactionlessAnchors::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, Float> chancesBySite = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String siteId = ModCsv.text(row, SITE_COLUMN);
            String chanceText = ModCsv.text(row, CHANCE_COLUMN);
            try {
                float chance = Float.parseFloat(chanceText);
                if (siteId.isEmpty() || !(chance >= 0f) || Float.isInfinite(chance)) {
                    throw new NumberFormatException(chanceText);
                }
                chancesBySite.put(siteId, chance);
            } catch (NumberFormatException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": bad site or chance '" + chanceText + "'");
            }
        });
        CHANCES_BY_SITE.set(Map.copyOf(chancesBySite));
    }

    public static void clear() {
        CHANCES_BY_SITE.set(Map.of());
    }

    public static float chanceAt(String siteId) {
        return siteId == null ? 0f : CHANCES_BY_SITE.get().getOrDefault(siteId, 0f);
    }

    public static boolean factionless(String definitionId, SectorAPI sector) {
        FactionFleetDrops.Source source = FactionFleetDrops.source(definitionId);
        if (sector == null || source == null || source.anyFaction()) {
            return false;
        }
        List<String> existingFactionIds = factionsOf(source).stream().filter(factionId -> sector.getFaction(factionId) != null).toList();
        if (existingFactionIds.isEmpty()) {
            return true;
        }
        List<String> playableFactionIds = existingFactionIds.stream().filter(NexerelinCompat::isPlayable).toList();
        return !playableFactionIds.isEmpty() && playableFactionIds.stream().allMatch(NexerelinCompat::isEliminated);
    }

    static Set<String> factionsOf(FactionFleetDrops.Source source) {
        Set<String> factionIds = new LinkedHashSet<>(source.extraFactionIds());
        if (source.volume() != null) {
            factionIds.addAll(NpcFactionVolumes.factionsIn(source.volume()));
        }
        return factionIds;
    }

    public static ItemData roll(String siteId, Random random, SectorAPI sector) {
        return roll(siteId, random, sector, 1f);
    }

    public static ItemData roll(String siteId, Random random, SectorAPI sector, float chanceMult) {
        float chance = chanceAt(siteId) * chanceMult;
        if (!(chance > 0f) || random.nextFloat() >= chance) {
            return null;
        }
        List<ItemDefinition> candidates = new ArrayList<>();
        for (ItemDefinition anchor : ItemDefinitions.ofType(ItemType.WORMHOLE_ANCHOR)) {
            if (factionless(anchor.id(), sector) && WormholeAnchorDefinitions.obtainable(anchor.id(), sector)) {
                candidates.add(anchor);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return ItemData.rolled(candidates.get(random.nextInt(candidates.size())), random.nextLong());
    }
}
