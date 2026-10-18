package exiledsector.charts;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;

import java.util.List;
import java.util.Map;

final class ChartStarSystemGenerator extends StarSystemGenerator {

    static final float BINARY_PAD = 1500f;
    static final Map<String, List<String>> BACKGROUNDS_BY_NEBULA = Map.of(
            NEBULA_NONE, List.of("graphics/backgrounds/background2.jpg", "graphics/backgrounds/background4.jpg"),
            NEBULA_BLUE, List.of("graphics/backgrounds/background5.jpg"),
            NEBULA_AMBER, List.of("graphics/backgrounds/background6.jpg"),
            NEBULA_DEFAULT, List.of("graphics/backgrounds/background1.jpg", "graphics/backgrounds/background2.jpg"));

    ChartStarSystemGenerator(StarAge age, StarSystemType chartType) {
        super(new CustomConstellationParams(age));
        systemType = chartType;
    }

    @Override
    public void pickNebulaAndBackground() {
        boolean hasNebula = constellationAgeData.getProbNebula() > random.nextFloat();
        String picked = hasNebula ? nebulaTypes.get(constellationAge) : null;
        nebulaType = picked == null || !BACKGROUNDS_BY_NEBULA.containsKey(picked) ? NEBULA_NONE : picked;
        List<String> backgrounds = BACKGROUNDS_BY_NEBULA.get(nebulaType);
        backgroundName = backgrounds.get(random.nextInt(backgrounds.size()));
    }

    void generateLayout(StarSystemAPI system, String starId) {
        init(system, constellationAge);
        system.setType(systemType);
        pickNebulaAndBackground();
        system.setBackgroundTextureFilename(backgroundName);
        star = null;
        secondary = null;
        tertiary = null;
        systemCenter = null;
        if (!addStars(starId)) {
            throw new IllegalStateException("no star type could be picked for a " + systemType + " Star Chart layout");
        }
        updateAgeAfterPickingStar();
        GenResult result = addPlanetsAndTerrain(MAX_ORBIT_RADIUS);
        addFarStars(result);
        addSystemwideNebula();
        system.setAge(starAge);
    }

    private void addFarStars(GenResult result) {
        float primaryOrbitalRadius = result != null ? result.orbitalWidth * 0.5f : star.getRadius();
        float orbitAngle = random.nextFloat() * 360f;
        float baseOrbitRadius = primaryOrbitalRadius + BINARY_PAD;
        float orbitDays = baseOrbitRadius / (3f + random.nextFloat() * 2f);
        if (systemType == StarSystemType.BINARY_FAR && secondary != null) {
            addFarStar(secondary, orbitAngle, baseOrbitRadius, orbitDays);
        } else if (systemType == StarSystemType.TRINARY_1CLOSE_1FAR && tertiary != null) {
            addFarStar(tertiary, orbitAngle, baseOrbitRadius, orbitDays);
        } else if (systemType == StarSystemType.TRINARY_2FAR && secondary != null && tertiary != null) {
            addFarStar(secondary, orbitAngle, baseOrbitRadius, orbitDays);
            addFarStar(tertiary, orbitAngle + 60f + 180f * random.nextFloat(), baseOrbitRadius, orbitDays);
        }
    }

    @Override
    protected void addJumpPoints(GenResult result, boolean farStarMode) {
    }

    @Override
    protected void addJumpPoint(LagrangePoint point, String name) {
    }

    @Override
    protected void addStableLocations() {
    }
}
