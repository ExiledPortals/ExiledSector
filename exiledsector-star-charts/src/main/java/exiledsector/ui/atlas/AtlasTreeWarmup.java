package exiledsector.ui.atlas;

import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import exiledsector.PlanetSpecs;
import exiledsector.charts.tree.AtlasBodyArt;
import exiledsector.charts.tree.AtlasTreeBodies;
import exiledsector.charts.tree.AtlasTreeBodies.Body;
import exiledsector.ui.decoration.DecorationWarmup;

final class AtlasTreeWarmup {

    private AtlasTreeWarmup() {
    }

    static DecorationWarmup of(AtlasTreeBodies bodies) {
        DecorationWarmup warmup = new DecorationWarmup("Atlas tree");
        for (Body body : bodies.bodies()) {
            body.art().convoys().forEach(warmup::addHulls);
            body.art().haulers().forEach(warmup::addHulls);
        }
        for (Body body : bodies.bodies()) {
            AtlasBodyArt art = body.art();
            warmup.addHulls(art.hullIds());
            warmup.addHulls(art.wreckHulls());
            warmup.addHulls(art.rivalHulls());
        }
        for (Body body : bodies.bodies()) {
            AtlasBodyArt art = body.art();
            art.spritePaths().forEach(warmup::addTexture);
            PlanetSpecAPI spec = art.isPlanet() ? PlanetSpecs.byType(art.planetType()) : null;
            if (spec != null) {
                warmup.addTexture(spec.getTexture());
                if (spec.isStar()) {
                    warmup.addTexture(spec.getCoronaTexture());
                }
            }
        }
        return warmup;
    }
}
