package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.SectorEntityToken;

public record RelaySlot(RelayKind kind, boolean extra) {

    static final String EXTRA_SUFFIX = "_extra";

    public static RelaySlot of(RelayKind kind) {
        return new RelaySlot(kind, false);
    }

    static RelaySlot extraOf(RelayKind kind) {
        return new RelaySlot(kind, true);
    }

    public String key() {
        return extra ? kind.key() + EXTRA_SUFFIX : kind.key();
    }

    static RelaySlot parse(String key) {
        if (key == null) {
            return null;
        }
        boolean extraKey = key.endsWith(EXTRA_SUFFIX);
        RelayKind kind = RelayKind.parse(extraKey ? key.substring(0, key.length() - EXTRA_SUFFIX.length()) : key);
        return kind == null ? null : new RelaySlot(kind, extraKey);
    }

    public static RelaySlot of(SectorEntityToken entity) {
        RelayKind kind = RelayKind.of(entity);
        if (kind == null) {
            return null;
        }
        RelaySlot stored = parse(entity.getMemoryWithoutUpdate().getString(RelayNetwork.SLOT_MEMORY_KEY));
        return stored != null && stored.kind() == kind ? stored : of(kind);
    }
}
