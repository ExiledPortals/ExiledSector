package exiledsector.items;

import exiledlib.items.Item;
import exiledsector.skills.SkillNode;

public interface NodeSocketable {

    boolean canSocketInto(SkillNode socket);

    static boolean fits(Item item, SkillNode socket) {
        return item instanceof NodeSocketable socketable && socketable.canSocketInto(socket);
    }
}
