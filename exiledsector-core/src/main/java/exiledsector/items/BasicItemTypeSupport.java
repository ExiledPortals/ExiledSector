package exiledsector.items;

import java.util.List;
import java.util.function.UnaryOperator;

public class BasicItemTypeSupport implements ItemTypeSupport {

    public interface Creator {
        Item create(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers);
    }

    private final ItemType type;
    private final Class<? extends Item> itemClass;
    private final String saveAlias;
    private final Creator creator;
    private final UnaryOperator<Item> converter;

    public BasicItemTypeSupport(ItemType type, Class<? extends Item> itemClass, String saveAlias, Creator creator, UnaryOperator<Item> converter) {
        this.type = type;
        this.itemClass = itemClass;
        this.saveAlias = saveAlias;
        this.creator = creator;
        this.converter = converter;
    }

    @Override
    public ItemType type() {
        return type;
    }

    @Override
    public Class<? extends Item> itemClass() {
        return itemClass;
    }

    @Override
    public String saveAlias() {
        return saveAlias;
    }

    @Override
    public Item create(String id, String definitionId, long seed, List<RolledModifier> explicitModifiers) {
        return creator.create(id, definitionId, seed, explicitModifiers);
    }

    @Override
    public Item convert(Item source) {
        return converter.apply(source);
    }
}
