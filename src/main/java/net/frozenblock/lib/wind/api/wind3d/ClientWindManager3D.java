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

package net.frozenblock.lib.wind.api.wind3d;

import net.frozenblock.lib.config.frozenlib_config.getter.FrozenLibConfigValues;
import net.frozenblock.lib.math.api.AdvancedMath;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.phys.Vec3;

public class ClientWindManager3D {
	public static long time;
	public static long seed = 0;
	public static boolean hasInitialized;

	public static void tick() {
		time += 1;
		if (!hasInitialized && time > 80 && FrozenLibConfigValues.CONFIG.getter().useWindOnNonFrozenServers()) {
			RandomSource randomSource = AdvancedMath.random();
			setSeed(randomSource.nextLong());
			time = randomSource.nextLong();
			hasInitialized = true;
		}
	}

	public static ImprovedNoise perlinChecked = new ImprovedNoise(new LegacyRandomSource(seed));
	public static ImprovedNoise perlinLocal = new ImprovedNoise(new SingleThreadedRandomSource(seed));
	public static ImprovedNoise perlinXoro = new ImprovedNoise(new XoroshiroRandomSource(seed));

	public static void setSeed(long newSeed) {
		if (newSeed != seed) {
			seed = newSeed;
			perlinChecked = new ImprovedNoise(new LegacyRandomSource(seed));
			perlinLocal = new ImprovedNoise(new SingleThreadedRandomSource(seed));
			perlinXoro = new ImprovedNoise(new XoroshiroRandomSource(seed));
		}
	}

	public static Vec3 getWindMovement(LevelReader reader, BlockPos pos) {
		double brightness = reader.getBrightness(LightLayer.SKY, pos);
		double windMultiplier = (Math.max((brightness - (Math.max(15 - brightness, 0))), 0) * 0.0667);
		Vec3 wind = sample(Vec3.atCenterOf(pos));
		return new Vec3(wind.x() * windMultiplier, wind.y() * windMultiplier, wind.z() * windMultiplier);
	}

	public static Vec3 getWindMovement(LevelReader reader, BlockPos pos, double multiplier) {
		double brightness = reader.getBrightness(LightLayer.SKY, pos);
		double windMultiplier = (Math.max((brightness - (Math.max(15 - brightness, 0))), 0) * 0.0667);
		Vec3 wind = sample(Vec3.atCenterOf(pos));
		return new Vec3((wind.x() * windMultiplier) * multiplier, (wind.y() * windMultiplier) * multiplier, (wind.z() * windMultiplier) * multiplier);
	}

	public static Vec3 getWindMovement(LevelReader reader, BlockPos pos, double multiplier, double clamp) {
		double brightness = reader.getBrightness(LightLayer.SKY, pos);
		double windMultiplier = (Math.max((brightness - (Math.max(15 - brightness, 0))), 0) * 0.0667);
		Vec3 wind = sample(Vec3.atCenterOf(pos));
		return new Vec3(Mth.clamp((wind.x() * windMultiplier) * multiplier, -clamp, clamp),
				Mth.clamp((wind.y() * windMultiplier) * multiplier, -clamp, clamp),
				Mth.clamp((wind.z() * windMultiplier) * multiplier, -clamp, clamp));
	}

	public static Vec3 getWindMovement(Vec3 pos) {
		Vec3 wind = sample(pos);
		return new Vec3(wind.x(), wind.y(), wind.z());
	}

	public static Vec3 getWindMovement(Vec3 pos, double multiplier) {
		Vec3 wind = sample(pos);
		return new Vec3((wind.x()) * multiplier, (wind.y()) * multiplier, (wind.z()) * multiplier);
	}

	public static Vec3 getWindMovement(Vec3 pos, double multiplier, double clamp) {
		Vec3 wind = sample(pos);
		return new Vec3(Mth.clamp((wind.x()) * multiplier, -clamp, clamp),
				Mth.clamp((wind.y()) * multiplier, -clamp, clamp),
				Mth.clamp((wind.z()) * multiplier, -clamp, clamp));
	}

	public static Vec3 sample(Vec3 pos) {
		double windX = perlinXoro.noise((pos.x() + time) * 0.025, 0, 0);
		double windY = perlinXoro.noise(0, (pos.y() + time) * 0.025, 0);
		double windZ = perlinXoro.noise(0, 0, (pos.z() + time) * 0.025);
		return new Vec3(windX, windY, windZ);
	}

}
