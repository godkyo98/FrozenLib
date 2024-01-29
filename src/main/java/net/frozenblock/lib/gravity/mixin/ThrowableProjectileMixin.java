/*
 * Copyright 2023-2024 FrozenBlock
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

package net.frozenblock.lib.gravity.mixin;

import net.frozenblock.lib.gravity.impl.EntityGravityInterface;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileMixin implements EntityGravityInterface {

	@Shadow
	protected abstract float getGravity();

	@ModifyArgs(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;setDeltaMovement(DDD)V", ordinal = 0))
	private void useGravity(Args args) {
		double x = (double) args.get(0);
		double y = (double) args.get(1) + this.frozenLib$getGravity();
		double z = (double) args.get(2);
		Vec3 gravity = this.frozenLib$getEffectiveGravity();

		args.set(0, x - gravity.x);
		args.set(1, y - gravity.y);
		args.set(2, z - gravity.z);
	}

	@Unique
	@Override
	public double frozenLib$getGravity() {
		return this.getGravity();
	}
}
