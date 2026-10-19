package exiledsector.items;

import exiledsector.skills.radius.RadiusSize;

public final class SubroutineRadius {

    public record Footprint(String centreNodeId, RadiusSize size) {
    }

    private SubroutineRadius() {
    }

    public static RadiusSize sizeOf(Item item) {
        if (!(item instanceof Subroutine subroutine) || subroutine.subroutineKind() == null) {
            return null;
        }
        return switch (subroutine.subroutineKind()) {
            case DOCTRINE -> {
                DoctrineArchive archive = DoctrineArchives.get(subroutine.definitionId());
                yield archive == null ? null : archive.radiusSize();
            }
            case MODSPEC_CACHE, BYPASS, UNIQUE -> null;
        };
    }

    public static String centreNodeId(Item item, String socketNodeId) {
        if (!(item instanceof Subroutine subroutine) || subroutine.subroutineKind() == null) {
            return null;
        }
        return switch (subroutine.subroutineKind()) {
            case DOCTRINE -> socketNodeId;
            case MODSPEC_CACHE, BYPASS, UNIQUE -> null;
        };
    }

    public static Footprint footprint(Item item, String socketNodeId) {
        RadiusSize size = sizeOf(item);
        String centreNodeId = size == null ? null : centreNodeId(item, socketNodeId);
        return centreNodeId == null ? null : new Footprint(centreNodeId, size);
    }

    public static boolean hasRadius(Item item) {
        return sizeOf(item) != null;
    }
}
