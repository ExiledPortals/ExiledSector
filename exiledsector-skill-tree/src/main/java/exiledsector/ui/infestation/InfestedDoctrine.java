package exiledsector.ui.infestation;

import exiledsector.items.subroutine.doctrine.DoctrineArchive;
import exiledsector.items.subroutine.doctrine.DoctrineInfestation;
import exiledsector.items.subroutine.doctrine.DoctrineLayer;
import exiledsector.items.subroutine.doctrine.DoctrineRewrite;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.layout.TreeTopology;
import exiledsector.skills.radius.NodeRadii;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record InfestedDoctrine(InfestationLook look, InfestationScene scene) {

    public static InfestedDoctrine of(DoctrineLayer doctrineLayer) {
        DoctrineArchive archive = doctrineLayer == null ? null : doctrineLayer.archive();
        if (archive == null || !archive.infested()) {
            return null;
        }
        SkillNode socket = SkillTree.get(doctrineLayer.socketNodeId());
        if (socket == null) {
            return null;
        }
        List<SkillNode> takenNodes = new ArrayList<>();
        Set<String> takenNodeIds = new HashSet<>();
        for (DoctrineRewrite rewrite : doctrineLayer.rewrites().values()) {
            SkillNode takenNode = rewrite.rewritten() ? SkillTree.get(rewrite.nodeId()) : null;
            if (takenNode != null) {
                takenNodes.add(takenNode);
                takenNodeIds.add(takenNode.getId());
            }
        }
        takenNodes.sort((first, second) -> first.getId().compareTo(second.getId()));
        List<InfestationScene.TakenLink> takenLinks = new ArrayList<>();
        for (TreeTopology.Connector<SkillNode> connector : SkillTree.topology().connectors()) {
            if (takenNodeIds.contains(connector.from().getId()) && takenNodeIds.contains(connector.to().getId())) {
                takenLinks.add(new InfestationScene.TakenLink(connector.from(), connector.to()));
            }
        }
        return new InfestedDoctrine(lookOf(archive.infestation()),
                new InfestationScene(socket, NodeRadii.distance(archive.radiusSize()), takenNodes, takenLinks));
    }

    static InfestationLook lookOf(DoctrineInfestation infestation) {
        return switch (infestation) {
            case THREAT -> InfestationLook.THREAT;
            case SHROUD -> InfestationLook.SHROUD;
        };
    }
}
