package exiledsector.charts.effect;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.data.EnumKeys;
import exiledsector.effects.ResolvedTree;
import exiledsector.effects.ShipEffectContext;
import exiledsector.effects.ShipEffectSource;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ChartEnemyShipEffects implements ShipEffectSource {

    public static final ChartEnemyShipEffects INSTANCE = new ChartEnemyShipEffects();
    static final String MOD_ID_PREFIX = ChartIds.PREFIX + "enemy_";
    private static final String ENTRY_SEPARATOR = ";";
    private static final String VALUE_SEPARATOR = "=";
    private static final String STAT_PREFIX = "stat:";
    private static final Logger LOG = Logger.getLogger(ChartEnemyShipEffects.class);

    record Granted(List<SkillTypeEffect> skillEffects, Map<ChartFleetStat, Float> statMults, ChartEnemyDamageListener damageListener) {

        static final Granted NONE = new Granted(List.of(), Map.of(), null);
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
        context.enemyShipEffects().forEach((effect, magnitude) -> entries.add(effect.name() + VALUE_SEPARATOR + magnitude));
        context.fleetStatMults().forEach((stat, mult) -> {
            if (stat.appliesToEnemyShips()) {
                entries.add(STAT_PREFIX + stat.name() + VALUE_SEPARATOR + mult);
            }
        });
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
        List<SkillTypeEffect> skillEffects = new ArrayList<>();
        Map<ChartFleetStat, Float> statMults = new EnumMap<>(ChartFleetStat.class);
        for (String entry : encoded.split(ENTRY_SEPARATOR)) {
            String[] fields = entry.split(VALUE_SEPARATOR);
            try {
                float value = Float.parseFloat(fields[1]);
                if (fields[0].startsWith(STAT_PREFIX)) {
                    ChartFleetStat stat = EnumKeys.parse(ChartFleetStat.class, fields[0].substring(STAT_PREFIX.length()));
                    if (stat != null && stat.appliesToEnemyShips()) {
                        statMults.put(stat, value);
                    }
                } else {
                    skillEffects.add(new SkillTypeEffect(SkillEffect.byName(fields[0]), value));
                }
            } catch (RuntimeException e) {
                LOG.warn("[ExiledSector] Skipping the unreadable chart enemy effect " + entry, e);
            }
        }
        return new Granted(List.copyOf(skillEffects), Map.copyOf(statMults), ChartEnemyDamageListener.of(statMults));
    }

    static Granted grantedTo(FleetMemberAPI member) {
        CampaignFleetAPI fleet = ChartFleetConditions.sourceFleetOf(member);
        return fleet == null ? Granted.NONE : decode(fleet.getMemoryWithoutUpdate().getString(ChartIds.ENEMY_EFFECTS_MEMORY_KEY));
    }

    @Override
    public Object cacheKey(ShipEffectContext context) {
        return grantedTo(context.member()).skillEffects();
    }

    @Override
    public List<EffectContribution> contributions(ShipEffectContext context) {
        List<SkillTypeEffect> effects = grantedTo(context.member()).skillEffects();
        return effects.isEmpty() ? List.of() : List.of(new EffectContribution(this, MOD_ID_PREFIX, effects, null, null, false, false));
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
            ship.addListener(new ChartEnemyModuleGrant(ship, granted));
        }
    }

    static void applyToModule(ShipAPI module, Granted granted) {
        MutableShipStatsAPI stats = module.getMutableStats();
        granted.statMults().forEach((stat, mult) -> stat.applyToShip(stats, MOD_ID_PREFIX, mult));
        for (SkillTypeEffect effect : granted.skillEffects()) {
            effect.effect().apply(stats, MOD_ID_PREFIX, effect.magnitude());
            effect.effect().applyAfterShipCreation(module, MOD_ID_PREFIX, effect.magnitude());
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
