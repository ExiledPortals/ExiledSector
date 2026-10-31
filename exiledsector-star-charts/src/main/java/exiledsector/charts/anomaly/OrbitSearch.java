package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.SectorEntityToken;

public record OrbitSearch(SectorEntityToken focus, float firstRadius, float bandWidth, float bandStep, int bands, float minEntryGap) {

    public static OrbitSearch bands(SectorEntityToken focus, float firstRadius, float bandWidth, int bands) {
        return new OrbitSearch(focus, firstRadius, bandWidth, bandWidth, bands, 0f);
    }

    public static OrbitSearch repeated(SectorEntityToken focus, float minRadius, float maxRadius, int tries) {
        return new OrbitSearch(focus, minRadius, maxRadius - minRadius, 0f, tries, 0f);
    }

    public static OrbitSearch stepping(SectorEntityToken focus, float firstRadius, float lastRadius, float step) {
        return new OrbitSearch(focus, firstRadius, 0f, step, (int) Math.floor((lastRadius - firstRadius) / step) + 1, 0f);
    }

    public OrbitSearch awayFromEntry(float gap) {
        return new OrbitSearch(focus, firstRadius, bandWidth, bandStep, bands, gap);
    }

    float bandMin(int band) {
        return firstRadius + band * bandStep;
    }
}
