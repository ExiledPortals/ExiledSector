package exiledlib;

import java.util.List;

public final class TestHost {

    public static final HostMod EXILED_SECTOR = new HostMod("exiledSector", "data/config/exiledSector/", List.of(
            "data/strings/exiledSector/core/", "data/strings/exiledSector/skillTree/", "data/strings/exiledSector/hullUpgrades/",
            "data/strings/exiledSector/starCharts/"), "exiledSector_");

    private TestHost() {
    }
}
