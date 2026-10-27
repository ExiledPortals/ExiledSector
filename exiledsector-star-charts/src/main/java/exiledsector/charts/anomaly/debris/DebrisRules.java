package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import exiledsector.charts.generation.ChartTier;
import exiledsector.items.WeightedPick;

import java.util.List;
import java.util.Random;

public final class DebrisRules {

    static final int MIN_FIELDS = 1;
    static final int MAX_FIELDS = 4;
    static final float MIN_FIELD_RADIUS = 300f;
    static final float MAX_FIELD_RADIUS = 450f;
    static final float REVEAL_MARGIN = 600f;
    static final float FLEET_SIZE_SHARE = 0.6f;
    static final float MIN_FLEET_POINTS = 20f;
    static final float CORRUPTED_SIZE_MULT = 1.5f;
    static final float CORRUPTED_QUALITY_PERCENT = 30f;
    static final int CORRUPTED_CORE_STEPS = 1;
    static final int CORRUPTED_STRENGTH_BONUS_PERCENT = 20;
    static final float CURRENCY_PER_FLEET = 1f;
    static final float MAX_QUALITY = 1f;
    static final List<String> CORE_LEVELS = List.of(Commodities.GAMMA_CORE, Commodities.BETA_CORE, Commodities.ALPHA_CORE);
    static final List<CoreBand> CORE_BANDS = List.of(
            new CoreBand(1, List.of(3f, 1f, 0f)),
            new CoreBand(5, List.of(1f, 2f, 1f)),
            new CoreBand(10, List.of(0f, 1f, 3f)));

    record CoreBand(int fromTier, List<Float> weights) {
    }

    private DebrisRules() {
    }

    public static int fieldCount(Random random, int extraFields) {
        return MIN_FIELDS + random.nextInt(MAX_FIELDS - MIN_FIELDS + 1) + Math.max(0, extraFields);
    }

    public static float fieldRadius(Random random) {
        return MIN_FIELD_RADIUS + random.nextFloat() * (MAX_FIELD_RADIUS - MIN_FIELD_RADIUS);
    }

    public static float revealRange(float fieldRadius) {
        return fieldRadius + REVEAL_MARGIN;
    }

    public static boolean reveals(float distance, float fleetRadius, float fieldRadius) {
        return distance - Math.max(0f, fleetRadius) <= revealRange(fieldRadius);
    }

    public static float fleetPoints(ChartTier adjustedTier, boolean corrupted) {
        float points = ChartTier.fleetPoints(adjustedTier.fleetSize()) * FLEET_SIZE_SHARE * (corrupted ? CORRUPTED_SIZE_MULT : 1f);
        return Math.max(MIN_FLEET_POINTS, points);
    }

    public static float quality(ChartTier adjustedTier, boolean corrupted) {
        float percent = adjustedTier.fleetQuality() + (corrupted ? CORRUPTED_QUALITY_PERCENT : 0f);
        return Math.max(0f, Math.min(MAX_QUALITY, percent / ChartTier.PERCENT));
    }

    static CoreBand bandFor(int tier) {
        CoreBand chosen = CORE_BANDS.get(0);
        for (CoreBand band : CORE_BANDS) {
            if (tier >= band.fromTier()) {
                chosen = band;
            }
        }
        return chosen;
    }

    public static int currencyCount(float quantityMult, Random random) {
        float expected = CURRENCY_PER_FLEET * Math.max(0f, quantityMult);
        int whole = (int) expected;
        return whole + (random.nextFloat() < expected - whole ? 1 : 0);
    }

    public static int coreLevel(int tier, Random random) {
        List<Float> weights = bandFor(tier).weights();
        List<Integer> levels = List.of(0, 1, 2);
        return WeightedPick.pick(levels, level -> weights.get(level), random);
    }

    public static String coreId(int coreLevel, boolean corrupted) {
        int level = Math.max(0, Math.min(CORE_LEVELS.size() - 1, coreLevel + (corrupted ? CORRUPTED_CORE_STEPS : 0)));
        return CORE_LEVELS.get(level);
    }
}
