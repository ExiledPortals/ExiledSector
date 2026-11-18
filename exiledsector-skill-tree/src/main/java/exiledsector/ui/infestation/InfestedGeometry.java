package exiledsector.ui.infestation;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.ui.node.NodeMetrics;

import java.util.ArrayList;
import java.util.List;

record InfestedGeometry(SkillNode socket, float socketX, float socketY, float worldRadius, int sceneSeed, List<InfestedNode> nodes,
                        List<InfestedLink> links) {

    static final float SMALL_VISUAL_RADIUS_SHARE = 0.5f;
    static final float LARGE_VISUAL_RADIUS_SHARE = 1f;

    record InfestedNode(float worldX, float worldY, float visualRadius, boolean large, int nodeSeed) {
    }

    record InfestedLink(InfestedNode from, InfestedNode to, float worldLength, int linkSeed) {

        float directionX() {
            return worldLength <= 0f ? 0f : (to.worldX() - from.worldX()) / worldLength;
        }

        float directionY() {
            return worldLength <= 0f ? 0f : (to.worldY() - from.worldY()) / worldLength;
        }
    }

    InfestedGeometry {
        nodes = List.copyOf(nodes);
        links = List.copyOf(links);
    }

    static InfestedGeometry of(InfestationScene scene) {
        SkillNode socket = scene.socket();
        float socketX = socket.getOffsetX();
        float socketY = socket.getOffsetY();
        List<InfestedNode> nodes = new ArrayList<>(scene.takenNodes().size());
        for (SkillNode taken : scene.takenNodes()) {
            nodes.add(infested(taken));
        }
        List<InfestedLink> links = new ArrayList<>(scene.takenLinks().size());
        for (InfestationScene.TakenLink link : scene.takenLinks()) {
            if (link.from() == null || link.to() == null) {
                continue;
            }
            InfestedNode first = infested(link.from());
            InfestedNode second = infested(link.to());
            boolean firstNearer = distanceSquared(first, socketX, socketY) <= distanceSquared(second, socketX, socketY);
            InfestedNode from = firstNearer ? first : second;
            InfestedNode to = firstNearer ? second : first;
            float length = (float) Math.hypot(to.worldX() - from.worldX(), to.worldY() - from.worldY());
            links.add(new InfestedLink(from, to, length, from.nodeSeed() * 31 + to.nodeSeed()));
        }
        return new InfestedGeometry(socket, socketX, socketY, scene.worldRadius(), socket.getId().hashCode(), nodes, links);
    }

    static float visualRadius(SkillTier tier) {
        float share = tier == SkillTier.SMALL ? SMALL_VISUAL_RADIUS_SHARE : LARGE_VISUAL_RADIUS_SHARE;
        return NodeMetrics.NODE_SIZE * tier.getSizeMultiplier() * share;
    }

    private static InfestedNode infested(SkillNode node) {
        SkillTier tier = node.getTier();
        return new InfestedNode(node.getOffsetX(), node.getOffsetY(), visualRadius(tier), tier != SkillTier.SMALL, node.getId().hashCode());
    }

    private static float distanceSquared(InfestedNode node, float socketX, float socketY) {
        float deltaX = node.worldX() - socketX;
        float deltaY = node.worldY() - socketY;
        return deltaX * deltaX + deltaY * deltaY;
    }
}
