package exiledsector.skills.skilleffect.hullmodule;

public enum ModuleShare {
    SHARE,
    SHARE_REBUILD,
    SKIP,
    EXCLUDE;

    public boolean isShared() {
        return this == SHARE || this == SHARE_REBUILD;
    }
}
