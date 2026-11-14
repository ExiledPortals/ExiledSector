package exiledsector.charts.anomaly.hypershunt;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.AtlasRule;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.GuardedStation;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.anomaly.StarRule;
import exiledsector.charts.anomaly.VanillaDefenders;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.UseVerdict;

import java.util.Random;

public final class CoronalHypershunt implements StarRule, AtlasRule, ContentPlacer, RunParticipant {

    public static final String ANOMALY_ID = "coronal_hypershunt";
    public static final String HYPERSHUNT_TAG = ChartIds.PREFIX + "coronal_hypershunt";
    public static final CoronalHypershunt INSTANCE = new CoronalHypershunt();
    public static final float MIN_DISTANCE = 15f;
    public static final String TOO_CLOSE_KEY = "catalyst.refused.hypershunt.too_close";
    static final String ENTITY_TYPE = Entities.CORONAL_TAP;
    public static final GuardedStation STATION = new GuardedStation(ENTITY_TYPE, HYPERSHUNT_TAG, ChartIds.PREFIX + "coronal_hypershunt_", null,
            HypershuntGuards.GUARD_SALT);
    static final float ORBIT_GAP = 250f;
    static final long PLACEMENT_SALT = 0x48595053484e54L;

    private CoronalHypershunt() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public String primaryStar() {
        return StarTypes.BLUE_SUPERGIANT;
    }

    @Override
    public StarSystemType systemType() {
        return StarSystemType.SINGLE;
    }

    @Override
    public boolean fitsAt(AtlasTraits traits) {
        return traits.distance() >= MIN_DISTANCE;
    }

    @Override
    public UseVerdict insertVerdict(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return fitsAt(generator.traits(cell)) ? UseVerdict.ALLOW : UseVerdict.refused(TOO_CLOSE_KEY);
    }

    @Override
    public void generate(AnomalyContentSite site) {
        if (place(site) == null) {
            throw new IllegalStateException("no star for the coronal hypershunt");
        }
    }

    static SectorEntityToken place(AnomalyContentSite site) {
        PlanetAPI star = site.system().getStar();
        if (star == null) {
            return null;
        }
        Random random = site.random(PLACEMENT_SALT);
        float radius = star.getRadius() + ORBIT_GAP;
        ChartSites.Orbit orbit = site.clearOrbit(star, radius, radius, random);
        if (orbit == null) {
            orbit = AnomalyContentSite.orbit(star, random.nextFloat() * 360f, radius);
        }
        return STATION.place(site, orbit);
    }

    static ChartRun liveRunFor(SectorEntityToken hypershunt) {
        return STATION.liveRun(hypershunt, ANOMALY_ID);
    }

    @Override
    public InteractionDialogPlugin dialogFor(AnomalyRun run, SectorEntityToken target) {
        return STATION.holds(target) && run.holds(target) ? new HypershuntDialog() : null;
    }

    @Override
    public String intelText(AnomalyRun run) {
        SectorEntityToken hypershunt = STATION.in(run.system());
        PlanetAPI star = hypershunt == null || !VanillaDefenders.guarded(hypershunt) || !(hypershunt.getOrbitFocus() instanceof PlanetAPI focus)
                ? null : focus;
        if (star == null) {
            return null;
        }
        StyledText name = StyledText.styled(star.getName(), Style.HIGHLIGHT);
        return I18n.forGameText(() -> Translation.msg("chart.anomaly.coronal_hypershunt.intel").arg("star", name).text());
    }
}
