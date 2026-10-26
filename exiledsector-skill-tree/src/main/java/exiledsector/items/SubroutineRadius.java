package exiledsector.items;

import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.skills.radius.RadiusSize;

public final class SubroutineRadius {

    public record Footprint(String centreNodeId, RadiusSize size, String factionId) {
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
            case UNIQUE -> UniqueSubroutineRadii.sizeOf(subroutine.definitionId());
            case MODSPEC_CACHE, BYPASS, ROOT_CONDUIT -> null;
        };
    }

    public static String centreNodeId(Item item, String socketNodeId) {
        if (!(item instanceof Subroutine subroutine) || subroutine.subroutineKind() == null) {
            return null;
        }
        return switch (subroutine.subroutineKind()) {
            case DOCTRINE, UNIQUE -> socketNodeId;
            case MODSPEC_CACHE, BYPASS, ROOT_CONDUIT -> null;
        };
    }

    public static Footprint footprint(Item item, String socketNodeId) {
        RadiusSize size = sizeOf(item);
        String centreNodeId = size == null ? null : centreNodeId(item, socketNodeId);
        return centreNodeId == null ? null : new Footprint(centreNodeId, size, factionIdOf(item));
    }

    static String factionIdOf(Item item) {
        if (!(item instanceof Subroutine subroutine) || subroutine.subroutineKind() != SubroutineKind.DOCTRINE) {
            return null;
        }
        DoctrineArchive archive = DoctrineArchives.get(subroutine.definitionId());
        return archive == null ? null : archive.factionId();
    }

    public static boolean hasRadius(Item item) {
        return sizeOf(item) != null;
    }

    public static String radiusName(RadiusSize size) {
        return Translation.text("socketable.radius." + size.dataKey());
    }

    public static StyledText radiusLine(RadiusSize size) {
        return Translation.msg("socketable.radiusLine").arg("radius", radiusName(size)).styled();
    }
}
