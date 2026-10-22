package exiledsector.items;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

public final class DoctrineText {

    private DoctrineText() {
    }

    public static StyledText rewrittenBy(DoctrineRewrite rewrite) {
        ItemDefinition definition = ItemDefinitions.get(rewrite.doctrineDefinitionId());
        String doctrineName = definition == null ? rewrite.doctrineDefinitionId() : definition.displayName();
        String key = rewrite.transformed() ? "socketable.doctrine.transformedBy" : "socketable.doctrine.augmentedBy";
        return Translation.msg(key).arg("doctrine", doctrineName).arg("revision", String.valueOf(rewrite.revision())).styled();
    }

    public static StyledText limitLine() {
        int limit = SubroutineKind.DOCTRINE.socketLimitPerShip();
        return Translation.msg("socketable.doctrine.limit").count(limit).arg("limit", limit).styled();
    }

    public static String radiusName(DoctrineArchive archive) {
        return Translation.text("socketable.doctrine.radius." + archive.radiusSize().dataKey());
    }
}
