package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import exiledlib.items.ItemDefinition;
import exiledlib.items.ItemDefinitions;
import exiledlib.items.UniqueSource;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class UniqueSourceCheck {

    private static final Logger LOG = Logger.getLogger(UniqueSourceCheck.class);

    private UniqueSourceCheck() {
    }

    public static void logUnreachableSources() {
        try {
            Set<String> knownFactionIds = new HashSet<>();
            for (FactionSpecAPI factionSpec : Global.getSettings().getAllFactionSpecs()) {
                knownFactionIds.add(factionSpec.getId());
            }
            for (String warning : unreachableSources(ItemDefinitions.all(), knownFactionIds, ItemDrops::hasUniqueRoll)) {
                LOG.warn(warning);
            }
        } catch (RuntimeException e) {
            LOG.warn("Could not check where the unique items drop", e);
        }
    }

    static List<String> unreachableSources(Collection<ItemDefinition> definitions, Set<String> knownFactionIds, Predicate<String> siteRollsUniques) {
        List<String> warnings = new ArrayList<>();
        for (ItemDefinition definition : definitions) {
            for (UniqueSource source : definition.uniqueSources()) {
                if (source.kind() == UniqueSource.Kind.FACTION && !knownFactionIds.isEmpty() && !knownFactionIds.contains(source.sourceId())) {
                    warnings.add(definition.id() + " drops from " + source.text() + ", but no faction with that id is loaded, so it can never drop there");
                } else if (source.kind() == UniqueSource.Kind.SITE && !siteRollsUniques.test(source.sourceId())) {
                    warnings.add(definition.id() + " drops from " + source.text() + ", but " + ItemDrops.DATA_PATH
                            + " has no row for that site with a uniqueChance above zero, so it can never drop there");
                }
            }
        }
        return warnings;
    }
}
