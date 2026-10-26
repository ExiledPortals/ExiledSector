package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.ChartGenContext;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.LongFunction;
import java.util.function.Predicate;

public final class AnomalyContentSite {

    private final StarSystemAPI system;
    private final String runId;
    private final int tier;
    private final ChartGenContext context;
    private final List<SectorEntityToken> pois;
    private final Vector2f entry;
    private final Predicate<Vector2f> clear;
    private final LongFunction<Random> randoms;

    public AnomalyContentSite(StarSystemAPI system, String runId, int tier, ChartGenContext context, List<SectorEntityToken> pois, Vector2f entry,
                         Predicate<Vector2f> clear, LongFunction<Random> randoms) {
        this.system = system;
        this.runId = runId;
        this.tier = tier;
        this.context = context == null ? ChartGenContext.none() : context;
        this.pois = pois == null ? List.of() : pois.stream().filter(Objects::nonNull).toList();
        this.entry = entry == null ? null : new Vector2f(entry);
        this.clear = clear;
        this.randoms = randoms;
    }

    public StarSystemAPI system() {
        return system;
    }

    public String runId() {
        return runId;
    }

    public int tier() {
        return tier;
    }

    public ChartGenContext context() {
        return context;
    }

    public List<SectorEntityToken> pois() {
        return pois;
    }

    public boolean isClear(Vector2f point) {
        return point != null && (clear == null || clear.test(point));
    }

    public float distanceToEntry(Vector2f point) {
        return entry == null || point == null ? Float.MAX_VALUE : Vector2f.sub(point, entry, new Vector2f()).length();
    }

    public Random random(long salt) {
        return randoms.apply(salt);
    }
}
