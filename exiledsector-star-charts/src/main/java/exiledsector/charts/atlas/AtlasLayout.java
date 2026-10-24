package exiledsector.charts.atlas;

import exiledsector.charts.ChartShape;

public record AtlasLayout(String id, float weight, ChartShape shape) {

    public AtlasLayout {
        weight = Math.max(0f, weight);
        shape = shape == null ? ChartShape.ANY : shape;
    }
}
