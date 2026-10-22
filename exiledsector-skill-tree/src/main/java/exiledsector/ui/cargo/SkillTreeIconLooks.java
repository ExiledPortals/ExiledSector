package exiledsector.ui.cargo;

import exiledsector.items.DoctrineArchive;
import exiledsector.items.DoctrineArchives;
import exiledlib.items.Item;
import exiledsector.items.Subroutine;
import exiledsector.items.SubroutineKind;
import exiledsector.items.WormholeAnchor;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.ui.node.DoctrineColors;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class SkillTreeIconLooks implements ItemIconLooks {

    public static final SkillTreeIconLooks INSTANCE = new SkillTreeIconLooks();

    private SkillTreeIconLooks() {
    }

    @Override
    public CargoIconLook ownLook(Item item) {
        if (item instanceof WormholeAnchor anchor && !AnimatedItemIcons.isAnimated(anchor.iconPath())) {
            return CargoIconLook.withArt(anchor.color(), new AnchorLook(anchor.crestFactionId()));
        }
        CargoIconLook doctrineLook = forDoctrine(item);
        return doctrineLook != null ? doctrineLook : forCache(item);
    }

    static CargoIconLook forDoctrine(Item item) {
        if (!(item instanceof Subroutine subroutine) || subroutine.subroutineKind() != SubroutineKind.DOCTRINE
                || !subroutine.iconPath().startsWith(DoctrineIconFx.STILL_PREFIX)) {
            return null;
        }
        DoctrineArchive archive = DoctrineArchives.get(subroutine.definitionId());
        if (archive == null || archive.factionId() == null) {
            return null;
        }
        Color factionColor = DoctrineColors.forFaction(archive.factionId());
        return CargoIconLook.withArt(factionColor == null ? item.frameColor() : factionColor,
                DoctrineLook.of(DoctrineScreen.forFaction(archive.factionId()), archive.factionId()));
    }

    static CargoIconLook forCache(Item item) {
        if (!(item instanceof Subroutine subroutine) || subroutine.subroutineKind() != SubroutineKind.MODSPEC_CACHE
                || !CacheIconFx.STILL_PATH.equals(subroutine.iconPath())) {
            return null;
        }
        List<String> chipIconPaths = new ArrayList<>();
        for (String nodeId : subroutine.currentNodeIds()) {
            SkillNode node = SkillTree.getDeclared(nodeId);
            if (node != null && node.getIconPath() != null) {
                chipIconPaths.add(node.getIconPath());
            }
        }
        return CargoIconLook.withArt(item.frameColor(), new CacheLook(chipIconPaths));
    }
}
