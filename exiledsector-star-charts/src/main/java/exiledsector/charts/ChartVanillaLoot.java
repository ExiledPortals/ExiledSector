package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

final class ChartVanillaLoot {

    static final float FLEET_POINTS_PER_STEP = 100f;

    record SiteDrops(List<DropData> random, List<DropData> value) {

        static final SiteDrops NONE = new SiteDrops(List.of(), List.of());

        SiteDrops {
            random = random == null ? List.of() : List.copyOf(random);
            value = value == null ? List.of() : List.copyOf(value);
        }

        SiteDrops plus(List<DropData> moreRandom, List<DropData> moreValue) {
            List<DropData> allRandom = new ArrayList<>(random);
            if (moreRandom != null) {
                allRandom.addAll(moreRandom);
            }
            List<DropData> allValue = new ArrayList<>(value);
            if (moreValue != null) {
                allValue.addAll(moreValue);
            }
            return new SiteDrops(allRandom, allValue);
        }
    }

    private ChartVanillaLoot() {
    }

    static void addToSite(SectorEntityToken site, ChartPoi.Kind kind, SiteDrops specDrops, ChartGenContext context, int tier, Random random) {
        SiteDrops existingDrops = specDrops.plus(site.getDropRandom(), site.getDropValue());
        for (DropData existingDrop : existingDrops.random()) {
            DropData extraDrop = scaledRandomCopy(existingDrop, context, random);
            if (extraDrop != null) {
                site.addDropRandom(extraDrop);
            }
        }
        for (DropData existingDrop : existingDrops.value()) {
            DropData extraDrop = scaledValueCopy(existingDrop, context);
            if (extraDrop != null) {
                site.addDropValue(extraDrop);
            }
        }
        for (ChartLoot.Entry entry : ChartLoot.entriesFor(kind.name().toLowerCase(Locale.ROOT), tier)) {
            float mult = context.dropMult(entry.type());
            if (entry.mode() == ChartLoot.Mode.RANDOM) {
                int chances = stochasticRound(entry.amount() * mult, random);
                if (chances > 0) {
                    site.addDropRandom(randomDrop(entry.group(), chances, 1f));
                }
            } else {
                addValue(site, entry, mult);
            }
        }
    }

    private static float extraShare(DropData existingDrop, ChartGenContext context) {
        DropType type = existingDrop == null ? null : ChartLoot.typeOfGroup(existingDrop.group);
        return type == null ? 0f : context.dropMult(type) - 1f;
    }

    static DropData scaledRandomCopy(DropData existingDrop, ChartGenContext context, Random random) {
        float extraShare = extraShare(existingDrop, context);
        if (existingDrop == null || !(extraShare > 0f) || existingDrop.chances <= 0) {
            return null;
        }
        int chances = stochasticRound(existingDrop.chances * extraShare, random);
        int maxChances = existingDrop.maxChances > existingDrop.chances ? Math.max(chances, Math.round(existingDrop.maxChances * extraShare)) : -1;
        if (chances <= 0 && maxChances <= 0) {
            return null;
        }
        DropData extraDrop = existingDrop.clone();
        extraDrop.chances = chances;
        extraDrop.maxChances = maxChances;
        return extraDrop;
    }

    static DropData scaledValueCopy(DropData existingDrop, ChartGenContext context) {
        float extraShare = extraShare(existingDrop, context);
        if (existingDrop == null || !(extraShare > 0f)) {
            return null;
        }
        DropData extraDrop = existingDrop.clone();
        extraDrop.valueMult = existingDrop.valueMult * extraShare;
        return extraDrop;
    }

    static int stochasticRound(float expected, Random random) {
        if (!(expected > 0f)) {
            return 0;
        }
        int whole = (int) expected;
        return whole + (random.nextFloat() < expected - whole ? 1 : 0);
    }

    static void addToFleet(CampaignFleetAPI fleet, boolean warden, ChartGenContext context, int tier) {
        float fleetPointSteps = fleet.getFleetPoints() / FLEET_POINTS_PER_STEP;
        if (!(fleetPointSteps > 0f)) {
            return;
        }
        for (ChartLoot.Entry entry : ChartLoot.entriesFor(ChartLoot.FLEET_SCOPE, tier)) {
            addToFleet(fleet, entry, fleetPointSteps * context.dropMult(entry.type()));
        }
        if (warden) {
            for (ChartLoot.Entry entry : ChartLoot.entriesFor(ChartLoot.WARDEN_SCOPE, tier)) {
                addToFleet(fleet, entry, fleetPointSteps * context.dropMult(entry.type()) * context.wardenSpoilsMult());
            }
        }
    }

    private static void addToFleet(CampaignFleetAPI fleet, ChartLoot.Entry entry, float mult) {
        if (!(mult > 0f)) {
            return;
        }
        if (entry.mode() == ChartLoot.Mode.RANDOM) {
            fleet.addDropRandom(randomDrop(entry.group(), 1, entry.amount() * mult));
        } else {
            addValue(fleet, entry, mult);
        }
    }

    private static DropData randomDrop(String group, int chances, float valueMult) {
        DropData drop = new DropData();
        drop.group = group;
        drop.chances = chances;
        drop.valueMult = valueMult;
        return drop;
    }

    private static void addValue(SectorEntityToken holder, ChartLoot.Entry entry, float mult) {
        if (!(mult > 0f)) {
            return;
        }
        DropData drop = new DropData();
        drop.group = entry.group();
        drop.value = Math.max(1, Math.round(entry.amount()));
        drop.valueMult = mult;
        holder.addDropValue(drop);
    }
}
