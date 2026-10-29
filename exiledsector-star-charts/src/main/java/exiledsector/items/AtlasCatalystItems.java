package exiledsector.items;

import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import exiledsector.i18n.Translation;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CatalystLook;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class AtlasCatalystItems {

    public record Carried(AtlasCatalysts.Catalyst catalyst, int count) {
    }

    private static final Color TERMINUS_COLOR = new Color(190, 120, 255);
    private static final Color SURVEY_COLOR = new Color(110, 210, 230);
    private static final Color ORDERS_COLOR = new Color(240, 180, 90);

    private AtlasCatalystItems() {
    }

    public static Color color(AtlasCatalysts.Family family) {
        if (family == null) {
            return TERMINUS_COLOR;
        }
        return switch (family) {
            case TERMINUS -> TERMINUS_COLOR;
            case SURVEY -> SURVEY_COLOR;
            case ORDERS -> ORDERS_COLOR;
        };
    }

    public static CargoIconLook look(AtlasCatalysts.Family family) {
        return CargoIconLook.withArt(color(family), new CatalystLook(family));
    }

    public static List<Carried> carried(CraftingMaterials materials) {
        List<Carried> carried = new ArrayList<>();
        for (AtlasCatalysts.Family family : AtlasCatalysts.Family.values()) {
            for (AtlasCatalysts.Catalyst catalyst : AtlasCatalysts.all()) {
                int count = catalyst.family() == family ? materials.count(catalyst.itemId()) : 0;
                if (count > 0) {
                    carried.add(new Carried(catalyst, count));
                }
            }
        }
        return carried;
    }

    public static int carriedTotal(CraftingMaterials materials) {
        return carried(materials).stream().mapToInt(Carried::count).sum();
    }

    public static String familyName(AtlasCatalysts.Family family) {
        return Translation.text("catalyst.family." + family.key() + ".name");
    }

    public static String addsLine(AtlasCatalysts.Catalyst catalyst) {
        if (catalyst.effect() == AtlasCatalysts.Effect.REDEPLOY) {
            return Translation.text("catalyst.redeploys");
        }
        return Translation.msg("catalyst.adds").arg("anomaly", Translation.text("chart.atlas.anomaly." + catalyst.anomalyId())).text();
    }

    public static String flavourKey(AtlasCatalysts.Family family) {
        return "catalyst.family." + family.key() + ".flavour";
    }

    public static void localise() {
        for (AtlasCatalysts.Catalyst catalyst : AtlasCatalysts.all()) {
            SpecialItemSpecAPI spec = MaterialItems.spec(catalyst.itemId());
            if (spec != null) {
                spec.setName(Translation.data("catalyst." + catalyst.itemId() + ".name", spec.getName()));
                spec.setDesc(Translation.text(flavourKey(catalyst.family())));
            }
        }
    }
}
