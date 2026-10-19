package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;

import java.util.ArrayList;
import java.util.List;

public final class WormholeAnchor extends Item {

    WormholeAnchor(String id, String definitionId, long seed) {
        super(id, definitionId, seed, List.of());
    }

    WormholeAnchor(Item source) {
        super(source);
    }

    @Override
    public ItemType type() {
        return ItemType.WORMHOLE_ANCHOR;
    }

    public String volume() {
        return WormholeAnchorDefinitions.volume(definitionId());
    }

    public String exitNodeId() {
        return WormholeAnchorDefinitions.exitNodeId(definitionId());
    }

    public boolean areaActive() {
        return WormholeAnchorDefinitions.areaActive(definitionId());
    }

    public boolean unlocked(SectorAPI sector) {
        ItemDefinition definition = definition();
        return definition != null && ItemUnlock.unlockConditionMet(definition, sector);
    }

    @Override
    void applyTraits(ItemTraits traits) {
    }

    @Override
    public List<RolledModifier> explicits() {
        return List.of();
    }

    @Override
    public List<RolledModifier> implicits() {
        return List.of();
    }

    @Override
    public boolean mirrored() {
        return false;
    }

    @Override
    public boolean corrupted() {
        return false;
    }

    @Override
    public CorruptionOutcome corruption() {
        return null;
    }

    @Override
    public boolean canSocketInto(SkillNode socket) {
        return socket != null && socket.getType().getTier() == SkillTier.SOCKET && areaActive();
    }

    public String socketRefusal() {
        String volume = volume();
        if (volume == null || areaActive()) {
            return null;
        }
        return Translation.msg("socketable.anchor.areaOffRefusal").arg("name", name()).arg("volume", volumeName(volume)).text();
    }

    @Override
    public List<StyledText> headerLines() {
        List<StyledText> lines = new ArrayList<>();
        ItemDefinition definition = definition();
        StyledText description = definition == null ? null : definition.descriptionText();
        if (description != null) {
            lines.add(description);
        }
        lines.addAll(statusLines());
        return lines;
    }

    public List<StyledText> statusLines() {
        List<StyledText> lines = new ArrayList<>();
        String volume = volume();
        if (volume != null) {
            lines.add(Translation.msg("socketable.anchor.volume").arg("volume", volumeName(volume)).styled());
            if (!areaActive()) {
                lines.add(Translation.msg("socketable.anchor.areaOff").arg("volume", volumeName(volume)).styled());
            }
        }
        ItemDefinition definition = definition();
        if (definition != null && !definition.unlock().isEmpty() && !unlocked(Global.getSector())) {
            lines.add(Translation.styled(ItemUnlock.RAT_PLAYER_EXOSHIP.id().equals(definition.unlock())
                    ? "socketable.anchor.lockedUntilExoship" : "socketable.anchor.locked"));
        }
        return lines;
    }

    private static String volumeName(String volume) {
        return Translation.text("hyperspace.region." + volume);
    }
}
