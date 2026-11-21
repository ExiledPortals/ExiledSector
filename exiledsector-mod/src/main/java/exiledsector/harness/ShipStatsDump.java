package exiledsector.harness;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import exiledsector.ui.refit.UiReflection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

final class ShipStatsDump {

    private static final String GETTER_PREFIX = "get";

    private ShipStatsDump() {
    }

    static JSONObject of(ShipAPI ship) throws Throwable {
        JSONObject modifiedStats = new JSONObject();
        MutableShipStatsAPI stats = ship.getMutableStats();
        for (String getter : UiReflection.noArgMethodNamesReturning(MutableShipStatsAPI.class, MutableStat.class)) {
            if (UiReflection.call(stats, getter) instanceof MutableStat stat && !stat.isUnmodified()) {
                modifiedStats.put(statName(getter), statJson(stat));
            }
        }
        for (String getter : UiReflection.noArgMethodNamesReturning(MutableShipStatsAPI.class, StatBonus.class)) {
            if (UiReflection.call(stats, getter) instanceof StatBonus bonus && !bonus.isUnmodified()) {
                modifiedStats.put(statName(getter), bonusJson(bonus));
            }
        }
        return new JSONObject()
                .put("ship", CombatShips.describe(ship))
                .put("hullSize", String.valueOf(ship.getHullSize()))
                .put("side", ship.getOwner())
                .put("hitpoints", ship.getHitpoints())
                .put("maxHitpoints", ship.getMaxHitpoints())
                .put("armorRating", ship.getArmorGrid().getArmorRating())
                .put("flux", ship.getFluxTracker().getCurrFlux())
                .put("maxFlux", ship.getFluxTracker().getMaxFlux())
                .put("combatReadiness", ship.getCurrentCR())
                .put("hullMods", new JSONArray(ship.getVariant().getHullMods()))
                .put("modifiedStatCount", modifiedStats.length())
                .put("modifiedStats", modifiedStats);
    }

    static String statName(String getter) {
        String name = getter.startsWith(GETTER_PREFIX) ? getter.substring(GETTER_PREFIX.length()) : getter;
        return name.isEmpty() ? getter : Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    static JSONObject statJson(MutableStat stat) throws JSONException {
        return new JSONObject().put("base", stat.getBaseValue()).put("modified", stat.getModifiedValue())
                .put("flat", mods(stat.getFlatMods())).put("percent", mods(stat.getPercentMods())).put("mult", mods(stat.getMultMods()));
    }

    static JSONObject bonusJson(StatBonus bonus) throws JSONException {
        return new JSONObject().put("flatTotal", bonus.getFlatBonus()).put("percentTotal", bonus.getPercentMod()).put("multTotal", bonus.getMult())
                .put("flat", mods(bonus.getFlatBonuses())).put("percent", mods(bonus.getPercentBonuses())).put("mult", mods(bonus.getMultBonuses()));
    }

    private static JSONObject mods(Map<String, MutableStat.StatMod> modsBySource) throws JSONException {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, MutableStat.StatMod> entry : modsBySource.entrySet()) {
            MutableStat.StatMod mod = entry.getValue();
            json.put(entry.getKey(), mod.getDesc() == null ? (Object) mod.getValue() : new JSONObject().put("value", mod.getValue()).put("desc", mod.getDesc()));
        }
        return json;
    }
}
