/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;

import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;

@Name("Initiator Inventory")
@Description("Returns the initiator inventory in on inventory item move event.")
@Examples({
		"on inventory item move:",
			"\tbroadcast \"%holder of past event-inventory% is transporting %event-item% to %holder of event-inventory%!\""
})
@Events("Inventory Item Move")
@Since("INSERT VERSION")
public class ExprEvtInitiator extends EventValueExpression<Inventory> {

	public ExprEvtInitiator() {
		super(Inventory.class);
	}

	static {
		Skript.registerExpression(ExprEvtInitiator.class, Inventory.class, ExpressionType.SIMPLE, "[the] [event-]initiator");
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parser) {
		if (!getParser().isCurrentEvent(InventoryMoveItemEvent.class)) {
			Skript.error("Expression 'the event-initiator' can only be used in on inventory item move event.");
			return false;
		}
		return super.init(exprs, matchedPattern, isDelayed, parser);
	}

	@Override
	public String toString() {
		return "the event-initiator of on inventory item move event";
	}
}
