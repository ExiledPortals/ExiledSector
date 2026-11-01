package exiledsector.charts.atlas;

import exiledsector.charts.content.ChartFactions;
import exiledsector.i18n.Translation;
import exiledsector.items.UseVerdict;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public record RedeploymentCatalystUse(AtlasCatalysts.Catalyst catalyst, AtlasGenerator generator, AtlasState state, AtlasCell openRunCell,
                                      Set<AtlasCell> revealed) implements AtlasCatalystUse {

    static final long REDEPLOY_SALT = 0x5245444550L;
    public static final String SPECIAL_KEY = "catalyst.refused.special";
    public static final String INFESTED_KEY = "catalyst.refused.infested";
    public static final String NO_FACTION_KEY = "catalyst.refused.no_faction";
    public static final String OPEN_RUN_KEY = "catalyst.refused.redeploy_open_run";

    public RedeploymentCatalystUse {
        revealed = Set.copyOf(revealed);
    }

    @Override
    public UseVerdict check(AtlasCell cell) {
        if (!revealed.contains(cell)) {
            return UseVerdict.refused(FOGGED_KEY);
        }
        AtlasAnomalies.InsertCheck system = AtlasAnomalies.systemCheck(generator, state, cell, openRunCell);
        if (system == AtlasAnomalies.InsertCheck.OPEN_RUN) {
            return UseVerdict.refused(OPEN_RUN_KEY);
        }
        if (!system.allows()) {
            return UseVerdict.refused(AtlasCatalystUse.refusalKey(system));
        }
        if (generator.node(cell).specialId() != null) {
            return UseVerdict.refused(SPECIAL_KEY);
        }
        String holder = AtlasOpening.ownerOf(generator, state, cell);
        if (holder != null && !holder.equals(AtlasOpening.territoryOwnerOf(generator, state, cell))) {
            return UseVerdict.refused(INFESTED_KEY).with("faction", ChartFactions.displayName(holder));
        }
        return newOwner(cell) == null ? UseVerdict.refused(NO_FACTION_KEY) : UseVerdict.ALLOW;
    }

    @Override
    public Outcome apply(AtlasCell cell) {
        String newOwner = check(cell).allowed() ? newOwner(cell) : null;
        if (newOwner == null) {
            return null;
        }
        state.capture(generator, cell, newOwner);
        return new Outcome("catalyst.window.redeployed", Map.of("faction", ChartFactions.displayName(newOwner)));
    }

    String newOwner(AtlasCell cell) {
        String owner = AtlasOpening.territoryOwnerOf(generator, state, cell);
        Map<String, Float> candidates = new LinkedHashMap<>(generator.factionPool(cell));
        candidates.remove(owner);
        long salt = REDEPLOY_SALT ^ AtlasHash.idSalt(owner == null ? "" : owner);
        return AtlasHash.race(generator.seed(), salt, cell.gridX(), cell.gridY(), candidates);
    }

    @Override
    public String usableText() {
        return Translation.text("catalyst.target.redeploy");
    }

    @Override
    public String confirmBody(String systemName) {
        return Translation.msg("catalyst.confirm.redeploy").arg("system", systemName).text();
    }
}
