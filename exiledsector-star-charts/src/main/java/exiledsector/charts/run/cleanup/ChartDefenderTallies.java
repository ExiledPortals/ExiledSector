package exiledsector.charts.run.cleanup;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI.GenericPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SDMParams;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SalvageDefenderModificationPlugin;
import exiledsector.charts.ChartIds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public final class ChartDefenderTallies implements SalvageDefenderModificationPlugin {

    static final int NOT_HANDLED = -1;
    static final float NO_EXPIRY = -1f;

    public static void register(SectorAPI sector) {
        GenericPluginManagerAPI plugins = sector.getGenericPlugins();
        if (plugins != null && !plugins.hasPlugin(ChartDefenderTallies.class)) {
            plugins.addPlugin(new ChartDefenderTallies(), true);
        }
    }

    @Override
    public int getHandlingPriority(Object params) {
        return params instanceof SDMParams sdmParams && inChartSystem(sdmParams.entity) && delegate(sdmParams) != null
                ? GenericPluginManagerAPI.HIGHEST : NOT_HANDLED;
    }

    @Override
    public float getStrength(SDMParams p, float strength, Random random, boolean withOverride) {
        SalvageDefenderModificationPlugin delegate = delegate(p);
        return delegate == null ? strength : delegate.getStrength(p, strength, random, withOverride);
    }

    @Override
    public float getProbability(SDMParams p, float probability, Random random, boolean withOverride) {
        SalvageDefenderModificationPlugin delegate = delegate(p);
        return delegate == null ? probability : delegate.getProbability(p, probability, random, withOverride);
    }

    @Override
    public float getQuality(SDMParams p, float quality, Random random, boolean withOverride) {
        SalvageDefenderModificationPlugin delegate = delegate(p);
        return delegate == null ? quality : delegate.getQuality(p, quality, random, withOverride);
    }

    @Override
    public float getMaxSize(SDMParams p, float maxSize, Random random, boolean withOverride) {
        SalvageDefenderModificationPlugin delegate = delegate(p);
        return delegate == null ? maxSize : delegate.getMaxSize(p, maxSize, random, withOverride);
    }

    @Override
    public float getMinSize(SDMParams p, float minSize, Random random, boolean withOverride) {
        SalvageDefenderModificationPlugin delegate = delegate(p);
        return delegate == null ? minSize : delegate.getMinSize(p, minSize, random, withOverride);
    }

    @Override
    public void modifyFleet(SDMParams p, CampaignFleetAPI fleet, Random random, boolean withOverride) {
        SalvageDefenderModificationPlugin delegate = delegate(p);
        if (delegate != null) {
            delegate.modifyFleet(p, fleet, random, withOverride);
        }
    }

    @Override
    public void reportDefeated(SDMParams p, SectorEntityToken entity, CampaignFleetAPI fleet) {
        SalvageDefenderModificationPlugin delegate = delegate(p);
        if (delegate == null) {
            return;
        }
        MemoryAPI global = Global.getSector().getMemoryWithoutUpdate();
        MemorySnapshot before = MemorySnapshot.of(global);
        try {
            delegate.reportDefeated(p, entity, fleet);
        } finally {
            before.restore(global);
        }
    }

    static boolean inChartSystem(SectorEntityToken entity) {
        LocationAPI location = entity == null ? null : entity.getContainingLocation();
        return location != null && location.hasTag(ChartIds.SYSTEM_TAG);
    }

    static SalvageDefenderModificationPlugin delegate(SDMParams p) {
        GenericPluginManagerAPI plugins = Global.getSector().getGenericPlugins();
        if (plugins == null) {
            return null;
        }
        SalvageDefenderModificationPlugin picked = null;
        int pickedPriority = NOT_HANDLED;
        for (GenericPlugin plugin : plugins.getPluginsOfClass(SalvageDefenderModificationPlugin.class)) {
            if (plugin instanceof ChartDefenderTallies || !(plugin instanceof SalvageDefenderModificationPlugin candidate)) {
                continue;
            }
            int priority = candidate.getHandlingPriority(p);
            if (priority > pickedPriority) {
                picked = candidate;
                pickedPriority = priority;
            }
        }
        return picked;
    }

    record MemorySnapshot(Map<String, Object> values, Map<String, Float> expiries) {

        static MemorySnapshot of(MemoryAPI memory) {
            Map<String, Object> values = new HashMap<>();
            Map<String, Float> expiries = new HashMap<>();
            for (String key : new ArrayList<>(memory.getKeys())) {
                values.put(key, memory.get(key));
                expiries.put(key, memory.getExpire(key));
            }
            return new MemorySnapshot(values, expiries);
        }

        void restore(MemoryAPI memory) {
            for (String key : new ArrayList<>(memory.getKeys())) {
                if (!values.containsKey(key)) {
                    memory.unset(key);
                }
            }
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String key = entry.getKey();
                Object current = memory.get(key);
                if (memory.contains(key) && (current == entry.getValue() || Objects.equals(current, entry.getValue()))) {
                    continue;
                }
                float expiry = expiries.getOrDefault(key, NO_EXPIRY);
                if (expiry >= 0f) {
                    memory.set(key, entry.getValue(), expiry);
                } else {
                    memory.set(key, entry.getValue());
                }
            }
        }
    }
}
