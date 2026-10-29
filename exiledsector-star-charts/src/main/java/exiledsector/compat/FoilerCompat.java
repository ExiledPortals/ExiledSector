package exiledsector.compat;

import java.util.List;

public final class FoilerCompat {

    public static final String TERRAIN_FX_ID = "foilerTerrain";
    public static final String HYDROFOIL_ID = "foilerHydro";
    public static final List<String> TERRAIN_FX_TESTED_VERSIONS = List.of("1.07");
    public static final List<String> HYDROFOIL_TESTED_VERSIONS = List.of("1.11.2");
    public static final CompatTarget TERRAIN_FX = new CompatTarget("Terrain FX", List.of(TERRAIN_FX_ID), TERRAIN_FX_TESTED_VERSIONS, List::of);
    public static final CompatTarget HYDROFOIL = new CompatTarget("Hydrofoil", List.of(HYDROFOIL_ID), HYDROFOIL_TESTED_VERSIONS, List::of);

    private FoilerCompat() {
    }
}
