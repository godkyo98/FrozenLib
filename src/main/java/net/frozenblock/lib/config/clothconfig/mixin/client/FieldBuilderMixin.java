/*
 * Copyright 2023 FrozenBlock
 * This file is part of FrozenLib.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package net.frozenblock.lib.config.clothconfig.mixin.client;

import java.lang.reflect.Field;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.lib.FrozenBools;
import net.frozenblock.lib.FrozenLogUtils;
import net.frozenblock.lib.config.api.annotation.FieldIdentifier;
import net.frozenblock.lib.config.api.network.ConfigSyncModification;
import net.frozenblock.lib.config.clothconfig.impl.AbstractConfigListEntryInterface;
import net.frozenblock.lib.config.clothconfig.impl.FieldBuilderInterface;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(FieldBuilder.class)
public class FieldBuilderMixin<T, A extends AbstractConfigListEntry, SELF extends FieldBuilder<T, A, SELF>> implements FieldBuilderInterface {

	@Shadow
	protected Requirement enableRequirement;

	@Unique
	private ConfigSyncModification.ModifyType frozenLib$modifyType = ConfigSyncModification.ModifyType.CAN_MODIFY;

	@Inject(method = "finishBuilding", at = @At(value = "RETURN", ordinal = 1), remap = false)
	public void finishBuilding(A gui, CallbackInfoReturnable<A> cir) {
		((AbstractConfigListEntryInterface)gui).frozenLib$setFieldBuilder(FieldBuilder.class.cast(this));
	}

	@Override
	@Unique
	public void frozenLib$addSyncData(@NotNull Class<?> clazz, String identifier) {
		Field field = null;
		for (Field fieldToCheck : clazz.getDeclaredFields()) {
			if (
				fieldToCheck.isAnnotationPresent(FieldIdentifier.class)
				&& fieldToCheck.getAnnotation(FieldIdentifier.class).identifier().equals(identifier)
			) {
				if (field != null) FrozenLogUtils.logError("Multiple fields in " + clazz.getName() + " contain identifier " + identifier + "!", true, null);
				field = fieldToCheck;
			}
		}
		Field finalField = field;
		if (finalField == null) {
			FrozenLogUtils.logError("No such field with identifier " + identifier + " exists in " + clazz.getName() + "!", true, null);
		}
		Requirement nonSyncRequirement = () -> {
			this.frozenLib$modifyType = ConfigSyncModification.canModifyField(finalField);
			return FrozenBools.connectedToLocalServer() || this.frozenLib$modifyType.canModify;
		};
		if (this.enableRequirement != null) {
			this.setRequirement(Requirement.all(this.enableRequirement, nonSyncRequirement));
		} else {
			this.setRequirement(nonSyncRequirement);
		}
	}

	@Override
	@Unique
	public ConfigSyncModification.ModifyType frozenLib$getModifyType() {
		return this.frozenLib$modifyType;
	}

	@Shadow
	public final SELF setRequirement(Requirement requirement) {
		throw new AssertionError("Mixin injection failed - FrozenLib FieldBuilderMixin.");
	}

}
