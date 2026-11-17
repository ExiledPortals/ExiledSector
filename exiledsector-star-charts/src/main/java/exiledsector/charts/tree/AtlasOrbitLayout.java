package exiledsector.charts.tree;

import exiledsector.charts.tree.AtlasOrbitSystem.Body;
import exiledsector.charts.tree.AtlasOrbitSystem.Link;
import exiledsector.charts.tree.AtlasOrbitSystem.Track;

import java.util.ArrayList;
import java.util.List;

public final class AtlasOrbitLayout {

    public record Wheel(int highway, int outer, int inner, int centre) {
    }

    public record Belt(Track track, boolean outermost) {
    }

    private final boolean[] highway;
    private final List<Wheel> wheels;
    private final List<Link> rays;
    private final List<Belt> belts;

    private AtlasOrbitLayout(boolean[] highway, List<Wheel> wheels, List<Link> rays, List<Belt> belts) {
        this.highway = highway;
        this.wheels = List.copyOf(wheels);
        this.rays = List.copyOf(rays);
        this.belts = List.copyOf(belts);
    }

    public static AtlasOrbitLayout of(AtlasOrbitSystem system) {
        List<Body> bodies = system.bodies();
        int[] satelliteCounts = new int[bodies.size()];
        for (Body body : bodies) {
            if (body.orbits()) {
                satelliteCounts[body.centreIndex()]++;
            }
        }
        boolean[] highway = new boolean[bodies.size()];
        for (Body body : bodies) {
            highway[body.index()] = body.orbits() && !bodies.get(body.centreIndex()).orbits() && satelliteCounts[body.index()] == 0;
        }
        return new AtlasOrbitLayout(highway, wheelsOf(system, satelliteCounts), raysOf(system), beltsOf(system, highway));
    }

    private static List<Wheel> wheelsOf(AtlasOrbitSystem system, int[] satelliteCounts) {
        List<Body> bodies = system.bodies();
        List<Wheel> wheels = new ArrayList<>();
        for (Body centre : bodies) {
            if (!centre.orbits() || satelliteCounts[centre.index()] < 2) {
                continue;
            }
            Body outer = null;
            Body inner = null;
            for (Body body : bodies) {
                if (body.centreIndex() != centre.index()) {
                    continue;
                }
                if (outer == null || body.orbitRadius() > outer.orbitRadius()) {
                    inner = outer;
                    outer = body;
                } else if (inner == null || body.orbitRadius() > inner.orbitRadius()) {
                    inner = body;
                }
            }
            if (outer != null && inner != null) {
                wheels.add(new Wheel(highwayLinkedTo(system, outer, centre.index()), outer.index(), inner.index(), centre.index()));
            }
        }
        return wheels;
    }

    private static int highwayLinkedTo(AtlasOrbitSystem system, Body outer, int centreIndex) {
        for (Link link : system.links()) {
            int other = link.first() == outer.index() ? link.second() : link.second() == outer.index() ? link.first() : -1;
            if (other >= 0 && other != centreIndex && system.bodies().get(other).centreIndex() != centreIndex) {
                return other;
            }
        }
        return -1;
    }

    private static List<Link> raysOf(AtlasOrbitSystem system) {
        List<Body> bodies = system.bodies();
        List<Link> rays = new ArrayList<>();
        for (Link link : system.links()) {
            if (!bodies.get(link.first()).orbits() || !bodies.get(link.second()).orbits()) {
                rays.add(link);
            }
        }
        return rays;
    }

    private static List<Belt> beltsOf(AtlasOrbitSystem system, boolean[] highway) {
        List<Track> highwayTracks = new ArrayList<>();
        Track outermost = null;
        for (Track track : system.tracks()) {
            if (!track.memberIndexes().isEmpty() && track.memberIndexes().stream().allMatch(member -> highway[member])) {
                highwayTracks.add(track);
                if (outermost == null || track.radius() > outermost.radius()) {
                    outermost = track;
                }
            }
        }
        List<Belt> belts = new ArrayList<>();
        for (Track track : highwayTracks) {
            belts.add(new Belt(track, track == outermost));
        }
        return belts;
    }

    public boolean isHighway(int bodyIndex) {
        return highway[bodyIndex];
    }

    public List<Wheel> wheels() {
        return wheels;
    }

    public List<Link> rays() {
        return rays;
    }

    public List<Belt> belts() {
        return belts;
    }
}
