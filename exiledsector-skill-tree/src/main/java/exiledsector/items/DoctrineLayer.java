package exiledsector.items;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTree;
import exiledsector.skills.radius.NodeRadiusCache;

import java.util.Map;

public final class DoctrineLayer {

    static final DoctrineLayer NONE = new DoctrineLayer(null, null, Map.of(), Stamp.NEVER, null);

    record Stamp(int dataRevision, int archiveGeneration, NodeRadiusCache radii) {

        static final Stamp NEVER = new Stamp(Integer.MIN_VALUE, Integer.MIN_VALUE, null);

        static Stamp of(ShipSkillData data) {
            return new Stamp(data.revision(), DoctrineArchives.generation(), SkillTree.radii());
        }

        boolean isCurrentFor(ShipSkillData data) {
            return dataRevision == data.revision() && archiveGeneration == DoctrineArchives.generation() && radii == SkillTree.radii();
        }
    }

    private final String socketNodeId;
    private final DoctrineArchive archive;
    private final Map<String, DoctrineRewrite> rewritesByNodeId;
    private final Stamp stamp;
    private final Object factsFingerprint;

    DoctrineLayer(String socketNodeId, DoctrineArchive archive, Map<String, DoctrineRewrite> rewritesByNodeId, Stamp stamp,
                  Object factsFingerprint) {
        this.socketNodeId = socketNodeId;
        this.archive = archive;
        this.rewritesByNodeId = Map.copyOf(rewritesByNodeId);
        this.stamp = stamp;
        this.factsFingerprint = factsFingerprint;
    }

    static DoctrineLayer empty(ShipSkillData data) {
        return new DoctrineLayer(null, null, Map.of(), Stamp.of(data), null);
    }

    boolean isCurrentFor(ShipSkillData data) {
        return stamp.isCurrentFor(data);
    }

    Object factsFingerprint() {
        return factsFingerprint;
    }

    public boolean isEmpty() {
        return archive == null;
    }

    public String socketNodeId() {
        return socketNodeId;
    }

    public DoctrineArchive archive() {
        return archive;
    }

    public DoctrineRewrite rewrite(String nodeId) {
        return nodeId == null ? null : rewritesByNodeId.get(nodeId);
    }

    public Map<String, DoctrineRewrite> rewrites() {
        return rewritesByNodeId;
    }
}
