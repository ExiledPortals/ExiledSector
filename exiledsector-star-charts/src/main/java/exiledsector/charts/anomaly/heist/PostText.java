package exiledsector.charts.anomaly.heist;

import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.List;

final class PostText {

    static final float PERCENT_SCALE = 100f;

    private PostText() {
    }

    static int percent(float share) {
        return Math.round(share * PERCENT_SCALE);
    }

    static List<String> lines(PostState state, PostScript script) {
        List<String> lines = new ArrayList<>();
        if (state.taken()) {
            lines.add(Translation.gameText(state.clean() ? "chart.post.status.takenClean" : "chart.post.status.taken"));
            return lines;
        }
        int pickets = script == null ? 0 : script.livingPickets().size();
        if (state.alarm()) {
            int vault = percent(state.vaultShare());
            lines.add(I18n.forGameText(() -> Translation.msg("chart.post.status.alarm").arg("value", vault).text()));
            if (script != null && script.defended()) {
                lines.add(Translation.gameText("chart.post.status.guarded"));
            }
            return lines;
        }
        int suspicion = percent(state.suspicion());
        lines.add(I18n.forGameText(() -> Translation.msg("chart.post.status.quiet").count(pickets).arg("suspicion", suspicion).text()));
        if (script != null && script.reportDaysLeft() >= 0f) {
            lines.add(Translation.gameText("chart.post.status.reporting"));
        }
        return lines;
    }

    static String status(PostState state, PostScript script) {
        return String.join(" ", lines(state, script));
    }
}
