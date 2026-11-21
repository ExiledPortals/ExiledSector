package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasEffects;
import exiledsector.charts.atlas.AtlasModifier;
import exiledsector.charts.officer.reward.CarriedItem;

import java.util.List;
import java.util.Random;

public record OfficerBoons(float trophyDuplicationPercent, float scavengeDuplicationPercent, boolean trophyRefit, float fightFrequencyPercent,
                           float scavengersPercent, boolean luckyScavenging) {

    public static final OfficerBoons NONE = new OfficerBoons(0f, 0f, false, 0f, 0f, false);

    public static OfficerBoons of(AtlasEffects effects) {
        if (effects == null) {
            return NONE;
        }
        return new OfficerBoons(effects.value(AtlasModifier.ATLAS_OFFICER_TROPHY_DUPLICATION_PERCENT),
                effects.value(AtlasModifier.ATLAS_OFFICER_SCAVENGE_DUPLICATION_PERCENT), effects.value(AtlasModifier.ATLAS_OFFICER_TROPHY_REFIT) > 0f,
                effects.value(AtlasModifier.ATLAS_OFFICER_FIGHT_FREQUENCY_PERCENT), effects.value(AtlasModifier.ATLAS_OFFICER_SCAVENGERS_PERCENT),
                effects.value(AtlasModifier.ATLAS_OFFICER_LUCKY_SCAVENGING) > 0f);
    }

    public float fightWeightMult() {
        return increasedMult(fightFrequencyPercent);
    }

    public TurnRules applyTo(TurnRules rules) {
        float scavengersMult = increasedMult(scavengersPercent);
        return rules.withCounts(Math.round(rules.maxConflictsPerTurn() * fightWeightMult()), Math.round(rules.scavengersMin() * scavengersMult),
                Math.round(rules.scavengersMax() * scavengersMult));
    }

    public List<CarriedItem> withTrophyDuplicates(List<CarriedItem> taken, Random random) {
        return trophyDuplicationPercent <= 0f ? taken : taken.stream().map(item -> duplicated(item, trophyDuplicationPercent, random)).toList();
    }

    public CarriedItem withScavengeDuplicates(CarriedItem scavenged, Random random) {
        return scavenged == null || scavengeDuplicationPercent <= 0f ? scavenged : duplicated(scavenged, scavengeDuplicationPercent, random);
    }

    static CarriedItem duplicated(CarriedItem item, float chancePercent, Random random) {
        int duplicates = 0;
        for (int entry = 0; entry < item.baseCount(); entry++) {
            if (random.nextFloat() * 100f < chancePercent) {
                duplicates++;
            }
        }
        return duplicates == 0 ? item : item.withCount(item.baseCount() + duplicates);
    }

    private static float increasedMult(float increasedPercent) {
        return Math.max(0f, 1f + increasedPercent / 100f);
    }
}
