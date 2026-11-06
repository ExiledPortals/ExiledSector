package exiledsector.skills.template;

import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.SocketCustody;
import exiledsector.items.WormholeAnchor;
import exiledsector.items.WormholeAnchorSocketLimits;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

public final class TemplateAnchors {

    private final Map<String, String> anchorsBySocket;
    private final Supplier<List<WormholeAnchor>> unusedAnchorSource;
    private final BiPredicate<SkillNode, WormholeAnchor> socketer;
    private List<WormholeAnchor> unusedAnchors;
    private int socketedCount;

    TemplateAnchors(SkillTreeTemplate template, Supplier<List<WormholeAnchor>> unusedAnchorSource, BiPredicate<SkillNode, WormholeAnchor> socketer) {
        this.anchorsBySocket = template.anchorsBySocket();
        this.unusedAnchorSource = unusedAnchorSource;
        this.socketer = socketer;
    }

    public static TemplateAnchors fromStorage(SkillTreeTemplate template, BiPredicate<SkillNode, WormholeAnchor> socketer) {
        return new TemplateAnchors(template, TemplateAnchors::unusedInStorage, socketer);
    }

    public static Map<String, String> socketedOn(ShipSkillData shipData) {
        return socketedOn(shipData, ItemStore::lookup);
    }

    static Map<String, String> socketedOn(ShipSkillData shipData, Function<String, Item> itemLookup) {
        Map<String, String> anchorsBySocket = new HashMap<>();
        shipData.getSocketedItems().forEach((socketId, itemId) -> {
            if (itemLookup.apply(itemId) instanceof WormholeAnchor anchor) {
                anchorsBySocket.put(socketId, anchor.definitionId());
            }
        });
        return anchorsBySocket;
    }

    public Function<TemplateStep, StepVerdict> fillingAfter(Function<TemplateStep, StepVerdict> attempt, ShipSkillData shipData) {
        if (anchorsBySocket.isEmpty()) {
            return attempt;
        }
        return step -> {
            StepVerdict verdict = attempt.apply(step);
            if (verdict == StepVerdict.ALLOCATE && anchorsBySocket.containsKey(step.nodeId())) {
                fill(shipData);
            }
            return verdict;
        };
    }

    public int socketedCount() {
        return socketedCount;
    }

    public void fill(ShipSkillData shipData) {
        for (Map.Entry<String, String> templateAnchor : anchorsBySocket.entrySet()) {
            String socketId = templateAnchor.getKey();
            SkillNode socket = SkillTree.get(socketId);
            if (socket == null || !shipData.isAllocated(socketId) || shipData.getSocketedItem(socketId) != null) {
                continue;
            }
            WormholeAnchor anchor = usableAnchor(templateAnchor.getValue(), socket, shipData);
            if (anchor != null && socketer.test(socket, anchor)) {
                unusedAnchors.remove(anchor);
                socketedCount++;
            }
        }
    }

    private WormholeAnchor usableAnchor(String anchorDefinitionId, SkillNode socket, ShipSkillData shipData) {
        if (unusedAnchors == null) {
            unusedAnchors = new ArrayList<>(unusedAnchorSource.get());
        }
        for (WormholeAnchor anchor : unusedAnchors) {
            if (anchor.definitionId().equals(anchorDefinitionId) && anchor.canSocketInto(socket) && anchor.socketRefusal().allowed()
                    && WormholeAnchorSocketLimits.refusal(anchor, socket.getId(), shipData.getSocketedItems()).allowed()) {
                return anchor;
            }
        }
        return null;
    }

    static List<WormholeAnchor> unusedInStorage() {
        Set<String> installedItemIds = SocketCustody.installations(ShipSkillDataManager.all()).keySet();
        List<WormholeAnchor> unused = new ArrayList<>();
        for (Item item : ItemStore.get().owned()) {
            if (item instanceof WormholeAnchor anchor && !installedItemIds.contains(anchor.id())) {
                unused.add(anchor);
            }
        }
        return unused;
    }
}
