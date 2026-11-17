package exiledsector.items.subroutine.doctrine;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.subroutine.SubroutineKind;

public final class DoctrineText {

    private DoctrineText() {
    }

    public static StyledText rewrittenBy(DoctrineRewrite rewrite) {
        ItemDefinition definition = ItemDefinitions.get(rewrite.doctrineDefinitionId());
        String doctrineName = definition == null ? rewrite.doctrineDefinitionId() : definition.displayName();
        String key = rewrite.rewritten() ? "socketable.doctrine.rewrittenBy" : "socketable.doctrine.augmentedBy";
        return Translation.msg(key).arg("doctrine", doctrineName).arg("revision", String.valueOf(rewrite.revision())).styled();
    }

    public static StyledText limitLine() {
        int limit = SubroutineKind.DOCTRINE.socketLimitPerShip();
        return Translation.msg("socketable.doctrine.limit").count(limit).arg("limit", limit).styled();
    }
}
