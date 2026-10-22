package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.unlock.NodeGates;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class WormholeAnchor extends Item implements NodeSocketable {

    public enum Inertness {
        AREA_OFF, LOCKED, GATED, EXIT_LOCKED
    }

    public static final ItemTypeSupport SUPPORT = new ItemTypeSupport() {
        @Override
        public ItemType type() {
            return ItemType.WORMHOLE_ANCHOR;
        }

        @Override
        public Class<? extends Item> itemClass() {
            return WormholeAnchor.class;
        }

        @Override
        public String saveAlias() {
            return "exiledSector.WormholeAnchor";
        }

        @Override
        public Item create(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
            return new WormholeAnchor(id, definitionId, seed);
        }

        @Override
        public Item convert(Item source) {
            return new WormholeAnchor(source);
        }

        @Override
        public boolean uniqueRarity(ItemDefinition definition) {
            return true;
        }

        @Override
        public Integer cargoPrice(Item item, boolean forSale) {
            WormholeAnchorDefinitions.AnchorMarket anchorMarket = WormholeAnchorDefinitions.market(item.definitionId());
            if (anchorMarket == null) {
                return null;
            }
            return forSale ? anchorMarket.price() : WormholeAnchorDefinitions.sellBaseValue(anchorMarket.price(),
                    Global.getSettings().getFloat(BUY_PRICE_MULT_SETTING), Global.getSettings().getFloat(SELL_PRICE_MULT_SETTING));
        }
    };

    static final String BUY_PRICE_MULT_SETTING = "nonEconItemBuyPriceMult";
    static final String SELL_PRICE_MULT_SETTING = "nonEconItemSellPriceMult";

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

    public Color color() {
        return WormholeAnchorDefinitions.style(definitionId()).color();
    }

    public String crestFactionId() {
        return WormholeAnchorDefinitions.style(definitionId()).crestFactionId();
    }

    @Override
    public Color frameColor() {
        return color();
    }

    public boolean unlocked(SectorAPI sector) {
        ItemDefinition definition = definition();
        return definition != null && ItemUnlock.unlockConditionMet(definition, sector);
    }

    public Inertness inertness(SectorAPI sector) {
        SkillNode exitNode = exitNode();
        if (exitNode == null) {
            return Inertness.AREA_OFF;
        }
        if (!unlocked(sector)) {
            return Inertness.LOCKED;
        }
        if (NodeGates.isLocked(exitNode, null)) {
            return Inertness.GATED;
        }
        return SkillTypeUnlockStatus.isLocked(exitNode.getType(), null) ? Inertness.EXIT_LOCKED : null;
    }

    public String linkedExitNodeId() {
        return exitNode() == null ? null : exitNodeId();
    }

    public boolean canOpen(SectorAPI sector) {
        return inertness(sector) == null;
    }

    private SkillNode exitNode() {
        String exitNodeId = exitNodeId();
        SkillNode exitNode = exitNodeId == null ? null : SkillTree.get(exitNodeId);
        return exitNode != null && exitNode.getType().getTier() == SkillTier.WORMHOLE_EXIT ? exitNode : null;
    }

    @Override
    public void applyTraits(ItemTraits traits) {
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
        lines.addAll(statusLines(description == null));
        return lines;
    }

    public List<StyledText> statusLines() {
        return statusLines(true);
    }

    private List<StyledText> statusLines(boolean includeVolumeLine) {
        List<StyledText> lines = new ArrayList<>();
        String volume = volume();
        if (volume != null && includeVolumeLine) {
            lines.add(Translation.msg("socketable.anchor.volume").arg("volume", volumeName(volume)).styled());
        }
        SectorAPI sector = Global.getSector();
        Inertness inertness = inertness(sector);
        if (inertness != null) {
            lines.add(inertLine(inertness, volume));
        }
        return lines;
    }

    public List<StyledText> socketStatusLines(ShipSkillData shipData, String socketNodeId) {
        String exitNodeId = exitNodeId();
        if (shipData == null || exitNodeId == null) {
            return statusLines();
        }
        List<StyledText> lines = new ArrayList<>();
        String volume = volume();
        String volumeName = volumeName(volume);
        lines.add(Translation.msg("socketable.anchor.volume").arg("volume", volumeName).styled());
        boolean linked = exitNodeId.equals(ShipTrees.anchorExit(shipData, socketNodeId));
        Inertness inertness = inertness(Global.getSector());
        if (linked && shipData.isAllocated(exitNodeId)) {
            lines.add(Translation.msg("socketable.anchor.open").arg("volume", volumeName).styled());
        } else if (inertness != null) {
            lines.add(inertLine(inertness, volume));
        } else if (!linked) {
            lines.add(Translation.msg("socketable.anchor.duplicate").arg("volume", volumeName).styled());
        } else {
            lines.add(Translation.msg("socketable.anchor.noFreeSlot").arg("volume", volumeName).styled());
        }
        return lines;
    }

    private StyledText inertLine(Inertness inertness, String volume) {
        String volumeName = volume == null ? "" : volumeName(volume);
        return switch (inertness) {
            case AREA_OFF -> Translation.msg("socketable.anchor.areaOff").arg("volume", volumeName).styled();
            case LOCKED -> {
                ItemDefinition definition = definition();
                boolean exoship = definition != null && ItemUnlock.RAT_PLAYER_EXOSHIP.id().equals(definition.unlock());
                yield Translation.styled(exoship ? "socketable.anchor.lockedUntilExoship" : "socketable.anchor.locked");
            }
            case GATED -> gateLine(volumeName);
            case EXIT_LOCKED -> Translation.msg("socketable.anchor.exitLocked").arg("volume", volumeName).styled();
        };
    }

    private StyledText gateLine(String volumeName) {
        SkillNode exitNode = exitNode();
        String gateTag = exitNode == null ? null : NodeGates.lockingGateTag(exitNode, null);
        String gateKey = "socketable.anchor.gate." + gateTag;
        if (gateTag != null && Translation.has(gateKey)) {
            return Translation.styled(gateKey);
        }
        return Translation.msg("socketable.anchor.exitLocked").arg("volume", volumeName).styled();
    }

    static String volumeName(String volume) {
        return Translation.text("hyperspace.region." + volume);
    }
}
