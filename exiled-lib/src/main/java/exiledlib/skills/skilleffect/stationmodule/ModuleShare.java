package exiledlib.skills.skilleffect.stationmodule;

public enum ModuleShare {
    SHARE,
    SHARE_REBUILD,
    SKIP,
    EXCLUDE;

    public boolean isShared() {
        return this == SHARE || this == SHARE_REBUILD;
    }
}
