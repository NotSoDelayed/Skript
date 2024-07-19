package ch.njol.skript.expressions;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;

@Name("Maximum Stack Size")
@Description({
	"The maximum stack size of an item (e.g. 64 for torches, 16 for buckets, and 1 for swords), or of an inventory to have items to stack up to.",
	"Since MC 1.20.5 onwards, the maximum stack size of items can be changed (any integer from 1 to 99), and the said item can be stacked up to the set value, up to the maximum stack size of an inventory."
})
@Examples({
	"send \"You can only pick up %max stack size of player's tool% of %type of (player's tool)%\" to player",
	"set the maximum stack size of inventory of all players to 16",
	"add 8 to the maximum stack size of player's tool",
	"reset the maximum stack size of {_gui}"
})
@Since("2.1, INSERT VERSION (changeable, inventory support)")
@RequiredPlugins("Spigot 1.20.5+ (changeable item max stack size)")
public class ExprMaxStack extends SimplePropertyExpression<Object, Integer> {

	static {
		register(ExprMaxStack.class, Integer.class, "max[imum] stack[[ ]size]", "itemtypes/inventories");
	}

	private static final boolean CHANGEABLE_ITEM_STACK_SIZE = Skript.methodExists(ItemMeta.class, "setMaxStackSize", Integer.class);

	@Override
	@Nullable
	public Integer convert(Object source) {
		if (source instanceof ItemType) {
			Object itemType = ((ItemType) source).getRandomStackOrMaterial();
			if (itemType instanceof ItemStack)
				return ((ItemStack) itemType).getMaxStackSize();
			return ((Material) itemType).getMaxStackSize();
		} else if (source instanceof Inventory) {
			return (((Inventory) source).getMaxStackSize());
		} else {
			// Invalid source
			return null;
		}
	}

	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		switch (mode) {
            case ADD:
            case REMOVE:
			case RESET:
			case SET:
				if (!CHANGEABLE_ITEM_STACK_SIZE && ItemType.class.isAssignableFrom(getExpr().getReturnType())) {
					Skript.error("Changing the maximum stack size of items requires Minecraft 1.20.5 or newer!");
					return null;
				}
				return CollectionUtils.array(Integer.class);
			default:
				return null;
        }
	}

	@Override
	public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
		for (Object source : getExpr().getArray(event)) {
			Integer change = null;
			if (mode != ChangeMode.RESET)
				change = (int) delta[0];
			if (source instanceof ItemType) {
				if (!CHANGEABLE_ITEM_STACK_SIZE)
					continue;
				ItemType itemType = ((ItemType) source);
				int size = itemType.getRandom().getMaxStackSize();
                switch (mode) {
                    case ADD:
						size += change;
                        break;
                    case SET:
						size = change;
						break;
                    case REMOVE:
						size -= change;
                        break;
                }
				ItemMeta meta = itemType.getItemMeta();
				// Minecraft only accepts stack size from 1 to 99
				meta.setMaxStackSize(change != null ? Math2.fit(1, size, 99) : null);
				itemType.setItemMeta(meta);
			} else if (source instanceof Inventory) {
				Inventory inv = ((Inventory) source);
				int size = inv.getMaxStackSize();
				switch (mode) {
					case ADD:
						size += change;
						break;
					case SET:
						size = change;
						break;
					case REMOVE:
						size -= change;
						break;
					case RESET:
						size = Bukkit.createInventory(null, inv.getType()).getMaxStackSize();
						break;
				}
				inv.setMaxStackSize(size);
			}
		}
	}

	@Override
	public Class<? extends Integer> getReturnType() {
		return Integer.class;
	}

	@Override
	protected String getPropertyName() {
		return "maximum stack size";
	}

}
