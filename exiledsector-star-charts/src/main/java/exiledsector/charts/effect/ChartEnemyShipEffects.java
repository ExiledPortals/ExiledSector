package exiledsector.charts.effect;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.data.EnumKeys;
import exiledsector.effects.GrantedNodeOrigin;
import exiledsector.effects.GrantedNodes;
import exiledsector.effects.ResolvedTree;
import exiledsector.effects.ShipEffectContext;
import exiledsector.effects.ShipEffectSource;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTypeEffect;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ChartEnemyShipEffects implements ShipEffectSource {

    public static final ChartEnemyShipEffects INSTANCE = new ChartEnemyShipEffects();
    static final String MOD_ID_PREFIX = ChartIds.PREFIX + "enemy_";
    static final String NODE_MOD_ID_PREFIX = MOD_ID_PREFIX + "node_";
    private static final String ENTRY_SEPARATOR = ";";
    private static final String VALUE_SEPARATOR = "=";
    private static final String STAT_PREFIX = "stat:";
    private static final String NODE_PREFIX = "node:";
    private static final Logger LOG = Logger.getLogger(ChartEnemyShipEffects.class);

    record Granted(Map<ChartFleetStat, Float> statMults, ChartEnemyDamageListener damageListener, List<String> nodeIds) {

        static final Granted NONE = new Granted(Map.of(), null, List.of());
    }

    private static String lastEncoded;
    private static Granted lastGranted = Granted.NONE;

    private ChartEnemyShipEffects() {
    }

    public static void grant(CampaignFleetAPI fleet, ChartGenContext context) {
        if (fleet == null || context == null) {
            return;
        }
        String encoded = encode(context);
        if (encoded.isEmpty()) {
            return;
        }
        fleet.getMemoryWithoutUpdate().set(ChartIds.ENEMY_EFFECTS_MEMORY_KEY, encoded);
        reinstall(fleet);
    }

    static void reinstall(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFleetData() == null || fleet.getMemoryWithoutUpdate().getString(ChartIds.ENEMY_EFFECTS_MEMORY_KEY) == null) {
            return;
        }
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            installHullMod(member);
        }
    }

    private static void installHullMod(FleetMemberAPI member) {
        ShipVariantAPI variant = member.getVariant();
        if (variant == null || variant.hasHullMod(SkillTreeHullMod.ID)) {
            return;
        }
        if (variant.getSource() != VariantSource.REFIT) {
            variant = variant.clone();
            variant.setSource(VariantSource.REFIT);
            member.setVariant(variant, false, true);
        }
        variant.addMod(SkillTreeHullMod.ID);
    }

    static String encode(ChartGenContext context) {
        List<String> entries = new ArrayList<>();
        context.fleetStatMults().forEach((stat, mult) -> {
            if (stat.appliesToEnemyShips()) {
                entries.add(STAT_PREFIX + stat.name() + VALUE_SEPARATOR + mult);
            }
        });
        context.enemyGrantedNodeIds().forEach(nodeId -> entries.add(NODE_PREFIX + nodeId));
        return String.join(ENTRY_SEPARATOR, entries);
    }

    static synchronized Granted decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return Granted.NONE;
        }
        if (!encoded.equals(lastEncoded)) {
            lastGranted = parse(encoded);
            lastEncoded = encoded;
        }
        return lastGranted;
    }

    private static Granted parse(String encoded) {
        Map<ChartFleetStat, Float> statMults = new EnumMap<>(ChartFleetStat.class);
        List<String> nodeIds = new ArrayList<>();
        for (String entry : encoded.split(ENTRY_SEPARATOR)) {
            if (entry.startsWith(NODE_PREFIX)) {
                nodeIds.add(entry.substring(NODE_PREFIX.length()));
                continue;
            }
            String[] fields = entry.split(VALUE_SEPARATOR);
            try {
                float value = Float.parseFloat(fields[1]);
                ChartFleetStat stat = fields[0].startsWith(STAT_PREFIX)
                        ? EnumKeys.parse(ChartFleetStat.class, fields[0].substring(STAT_PREFIX.length())) : null;
                if (stat != null && stat.appliesToEnemyShips()) {
                    statMults.put(stat, value);
                }
            } catch (RuntimeException e) {
                LOG.warn("[ExiledSector] Skipping the unreadable chart enemy effect " + entry, e);
            }
        }
        return new Granted(Map.copyOf(statMults), ChartEnemyDamageListener.of(statMults), List.copyOf(nodeIds));
    }

    public static List<String> grantedNodeIds(MemoryAPI fleetMemory) {
        return fleetMemory == null ? List.of() : decode(fleetMemory.getString(ChartIds.ENEMY_EFFECTS_MEMORY_KEY)).nodeIds();
    }

    static Granted grantedTo(FleetMemberAPI member) {
        CampaignFleetAPI fleet = ChartPlayerModifiers.sourceFleetOf(member);
        return fleet == null ? Granted.NONE : decode(fleet.getMemoryWithoutUpdate().getString(ChartIds.ENEMY_EFFECTS_MEMORY_KEY));
    }

    @Override
    public Object cacheKey(ShipEffectContext context) {
        Granted granted = grantedTo(context.member());
        if (granted.nodeIds().isEmpty() || context.variant() == null || context.variant().getHullSpec() == null) {
            return granted.nodeIds();
        }
        return List.of(granted.nodeIds(), context.variant().getHullSpec().getHullId(),
                Set.copyOf(context.variant().getHullMods()));
    }

    @Override
    public List<EffectContribution> contributions(ShipEffectContext context) {
        return GrantedNodes.withoutTree(grantedTo(context.member()).nodeIds(), context, NODE_MOD_ID_PREFIX);
    }

    @Override
    public void applyToShip(MutableShipStatsAPI stats) {
        if (stats == null) {
            return;
        }
        grantedTo(stats.getFleetMember()).statMults().forEach((stat, mult) -> stat.applyToShip(stats, MOD_ID_PREFIX, mult));
    }

    @Override
    public void afterShipCreation(ShipAPI ship, ResolvedTree resolvedTree) {
        Granted granted = grantedTo(SkillTreeHullMod.memberOf(ship));
        if (granted == Granted.NONE) {
            return;
        }
        addDamageListener(ship, granted);
        if (ship.getHullSpec() != null && ShipFacts.hasModuleSlots(ship.getHullSpec())) {
            ship.addListener(new ChartEnemyModuleGrant(ship, granted, nodeIdsGrantedWithoutTree(resolvedTree)));
        }
    }

    static List<String> nodeIdsGrantedWithoutTree(ResolvedTree resolvedTree) {
        if (resolvedTree == null) {
            return List.of();
        }
        List<String> nodeIds = new ArrayList<>();
        for (EffectContribution contribution : resolvedTree.contributions()) {
            if (contribution.origin() instanceof GrantedNodeOrigin origin) {
                nodeIds.add(origin.nodeId());
            }
        }
        return nodeIds;
    }

    static void applyToModule(ShipAPI module, Granted granted, List<String> nodeIdsGrantedWithoutTree) {
        MutableShipStatsAPI stats = module.getMutableStats();
        granted.statMults().forEach((stat, mult) -> stat.applyToShip(stats, MOD_ID_PREFIX, mult));
        ShipEffectContext moduleContext = new ShipEffectContext(new ShipSkillData(), null, module.getVariant(), module.getHullSize());
        for (EffectContribution contribution : GrantedNodes.withoutTree(nodeIdsGrantedWithoutTree, moduleContext, NODE_MOD_ID_PREFIX)) {
            if (contribution.permanent()) {
                for (SkillTypeEffect effect : contribution.effects()) {
                    effect.effect().apply(stats, contribution.modIdPrefix(), effect.magnitude());
                    effect.effect().applyAfterShipCreation(module, contribution.modIdPrefix(), effect.magnitude());
                }
            }
        }
        addDamageListener(module, granted);
    }

    @Override
    public void applyToFighter(ShipAPI fighter, ShipAPI ship) {
        if (fighter != null && ship != null) {
            addDamageListener(fighter, grantedTo(ship.getFleetMember()));
        }
    }

    private static void addDamageListener(ShipAPI ship, Granted granted) {
        if (granted.damageListener() != null && !ship.hasListenerOfClass(ChartEnemyDamageListener.class)) {
            ship.addListener(granted.damageListener());
        }
    }
}
