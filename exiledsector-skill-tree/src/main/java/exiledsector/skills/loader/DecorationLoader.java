package exiledsector.skills.loader;

import exiledsector.skills.decoration.AbyssSerpent;
import exiledsector.skills.decoration.Decoration;
import exiledsector.skills.decoration.DecorationKind;
import exiledsector.skills.decoration.Decorations;
import exiledsector.skills.decoration.RingBelt;
import exiledsector.skills.decoration.Star;
import exiledsector.skills.decoration.StaticImage;
import exiledsector.skills.decoration.ThreatUnit;
import exiledsector.skills.layout.Rotation;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class DecorationLoader {

    static final String SECTION = "decorations";
    static final List<String> RETIRED_SECTIONS = List.of("stars", "staticImages", "ringBelts", "threatUnits");

    private DecorationLoader() {
    }

    public static Decorations parse(JSONObject treeJson, String sourceName) {
        for (String retiredSection : RETIRED_SECTIONS) {
            if (treeJson.has(retiredSection)) {
                Logger.getLogger(DecorationLoader.class).error(sourceName + " still has a \"" + retiredSection + "\" list, which is no longer read; move its entries into \""
                        + SECTION + "\" with a kind");
            }
        }
        JSONArray entries = treeJson.optJSONArray(SECTION);
        if (entries == null) {
            return Decorations.NONE;
        }
        List<Decoration> decorations = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            try {
                Decoration decoration = parseDecoration(entries.getJSONObject(i));
                if (decoration != null) {
                    decorations.add(decoration);
                }
            } catch (JSONException e) {
                Logger.getLogger(DecorationLoader.class).error("Skipping decoration " + SkillTypeLoader.entryLabel(entries, i) + " in " + sourceName + ": " + e.getMessage());
            }
        }
        return new Decorations(decorations);
    }

    static Decoration parseDecoration(JSONObject decorationJson) throws JSONException {
        String kindKey = decorationJson.getString("kind");
        DecorationKind kind = DecorationKind.byDataKey(kindKey);
        if (kind == null) {
            throw new JSONException("unknown kind \"" + kindKey + "\"");
        }
        return switch (kind) {
            case STAR -> parseStar(decorationJson);
            case IMAGE -> parseImage(decorationJson);
            case RING_BELT -> parseRingBelt(decorationJson);
            case THREAT_UNIT -> parseThreatUnit(decorationJson);
            case SERPENT -> parseSerpent(decorationJson);
        };
    }

    private static List<String> tags(JSONObject decorationJson) throws JSONException {
        return SkillTypeLoader.parseStringArray(decorationJson.optJSONArray("tags"));
    }

    private static Rotation rotation(JSONObject decorationJson) {
        return new Rotation((float) decorationJson.optDouble("rotation", 0.0), (float) decorationJson.optDouble("rotationSpeed", 0.0));
    }

    static Star parseStar(JSONObject starJson) throws JSONException {
        String overlayPath = starJson.optString("overlayPath", null);
        Star.Overlay overlay = overlayPath == null || overlayPath.isEmpty() ? null
                : new Star.Overlay(overlayPath, (float) starJson.optDouble("overlaySize", 1.0), (float) starJson.optDouble("overlayAlpha", 1.0));
        return new Star(
                starJson.getString("id"),
                (float) starJson.getDouble("x"),
                (float) starJson.getDouble("y"),
                (float) starJson.getDouble("radius"),
                starJson.optString("starType", "star_yellow"),
                starJson.optString("color", null),
                overlay,
                tags(starJson));
    }

    static StaticImage parseImage(JSONObject imageJson) throws JSONException {
        return new StaticImage(
                imageJson.getString("id"),
                (float) imageJson.getDouble("x"),
                (float) imageJson.getDouble("y"),
                new StaticImage.Shape((float) imageJson.getDouble("width"), (float) imageJson.getDouble("height"), imageJson.getString("imagePath"), rotation(imageJson)),
                new StaticImage.Depth(imageJson.optBoolean("behindStars", false), (float) imageJson.optDouble("parallax", 1.0)),
                tags(imageJson));
    }

    static RingBelt parseRingBelt(JSONObject beltJson) throws JSONException {
        return new RingBelt(
                beltJson.getString("id"),
                (float) beltJson.getDouble("x"),
                (float) beltJson.getDouble("y"),
                new RingBelt.Shape((float) beltJson.getDouble("innerRadius"), (float) beltJson.getDouble("outerRadius"), beltJson.getString("ringArtPath"), rotation(beltJson)),
                tags(beltJson));
    }

    static ThreatUnit parseThreatUnit(JSONObject unitJson) throws JSONException {
        return new ThreatUnit(
                unitJson.getString("id"),
                (float) unitJson.getDouble("x"),
                (float) unitJson.getDouble("y"),
                unitJson.optString("unitType", ThreatUnit.FABRICATOR),
                (float) unitJson.getDouble("length"),
                (float) unitJson.optDouble("rotation", 0.0),
                tags(unitJson));
    }

    static AbyssSerpent parseSerpent(JSONObject serpentJson) throws JSONException {
        return new AbyssSerpent(
                serpentJson.getString("id"),
                (float) serpentJson.getDouble("x"),
                (float) serpentJson.getDouble("y"),
                new AbyssSerpent.Orbit((float) serpentJson.getDouble("orbitRadius"), (float) serpentJson.getDouble("orbitSeconds"),
                        serpentJson.optBoolean("clockwise", true), (float) serpentJson.optDouble("startAngle", 0.0)),
                new AbyssSerpent.Wobble((float) serpentJson.optDouble("wobble", 0.0), serpentJson.optInt("wobbleWaves", 0),
                        (float) serpentJson.optDouble("wobblePhase", 0.0)),
                tags(serpentJson));
    }
}
