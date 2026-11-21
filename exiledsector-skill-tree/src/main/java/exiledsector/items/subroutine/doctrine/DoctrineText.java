package exiledsector.items.subroutine.doctrine;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.subroutine.SubroutineKind;

public final class DoctrineText {

    private DoctrineText() {
    }

    public static StyledText revisionLine(String doctrineDefinitionId, int revision) {
        DoctrineInfestation infestation = infestationOf(doctrineDefinitionId);
        String key = infestation == null ? "socketable.doctrine.revision" : switch (infestation) {
            case THREAT -> "socketable.doctrine.fragmentPattern";
            case SHROUD -> "socketable.doctrine.frequency";
        };
        return Translation.msg(key).arg("revision", String.valueOf(revision)).styled();
    }

    public static StyledText rewrittenBy(DoctrineRewrite rewrite) {
        ItemDefinition definition = ItemDefinitions.get(rewrite.doctrineDefinitionId());
        String doctrineName = definition == null ? rewrite.doctrineDefinitionId() : definition.displayName();
        if (rewrite.rewritten() && infestationOf(rewrite.doctrineDefinitionId()) != null) {
            return Translation.msg("socketable.doctrine.convertedBy").arg("doctrine", doctrineName).styled();
        }
        String key = rewrite.rewritten() ? "socketable.doctrine.rewrittenBy" : "socketable.doctrine.augmentedBy";
        return Translation.msg(key).arg("doctrine", doctrineName).arg("revision", String.valueOf(rewrite.revision())).styled();
    }

    public static String rewritesCodexKey(DoctrineArchive archive) {
        return archive.infested() ? "codex.doctrine.converts" : "codex.doctrine.rewrites";
    }

    public static StyledText limitLine() {
        int limit = SubroutineKind.DOCTRINE.socketLimitPerShip();
        return Translation.msg("socketable.doctrine.limit").count(limit).arg("limit", limit).styled();
    }

    private static DoctrineInfestation infestationOf(String doctrineDefinitionId) {
        DoctrineArchive archive = DoctrineArchives.get(doctrineDefinitionId);
        return archive == null ? null : archive.infestation();
    }
}
