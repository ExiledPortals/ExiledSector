package exiledsector.skills.decoration;

import java.util.List;
import java.util.Set;

public final class Decorations {

    public static final Decorations NONE = new Decorations(List.of());

    private final List<Decoration> all;
    private final List<Star> stars;
    private final List<StaticImage> images;
    private final List<RingBelt> ringBelts;
    private final List<ThreatUnit> threatUnits;
    private final List<AbyssSerpent> serpents;

    public Decorations(List<? extends Decoration> decorations) {
        this.all = List.copyOf(decorations);
        this.stars = ofType(Star.class);
        this.images = ofType(StaticImage.class);
        this.ringBelts = ofType(RingBelt.class);
        this.threatUnits = ofType(ThreatUnit.class);
        this.serpents = ofType(AbyssSerpent.class);
    }

    private <T extends Decoration> List<T> ofType(Class<T> type) {
        return all.stream().filter(type::isInstance).map(type::cast).toList();
    }

    public Decorations outsideRegions(Set<String> disabledRegions) {
        if (disabledRegions.isEmpty()) {
            return this;
        }
        return new Decorations(all.stream().filter(decoration -> decoration.getRegion() == null || !disabledRegions.contains(decoration.getRegion())).toList());
    }

    public List<Decoration> all() {
        return all;
    }

    public List<Star> stars() {
        return stars;
    }

    public List<StaticImage> images() {
        return images;
    }

    public List<RingBelt> ringBelts() {
        return ringBelts;
    }

    public List<ThreatUnit> threatUnits() {
        return threatUnits;
    }

    public List<AbyssSerpent> serpents() {
        return serpents;
    }
}
