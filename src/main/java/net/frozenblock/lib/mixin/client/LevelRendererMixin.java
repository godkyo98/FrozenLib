/*
 * Copyright 2022 FrozenBlock
 * This file is part of FrozenLib.
 *
 * FrozenLib is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * FrozenLib is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with FrozenLib. If not, see <https://www.gnu.org/licenses/>.
 */

package net.frozenblock.lib.mixin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Matrix4f;
import net.frozenblock.lib.spotting_icons.impl.EntityRenderDispatcherWithIcon;
import net.frozenblock.lib.wind.ClientWindManager;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

	@Shadow @Final
	private Minecraft minecraft;
	@Shadow
	@Final
	private RenderBuffers renderBuffers;
	@Shadow
	@Final
	private EntityRenderDispatcher entityRenderDispatcher;
	@Shadow
	@Nullable
	private ClientLevel level;
	@Shadow
	private int ticks;
	@Shadow
	private boolean generateClouds;
	@Shadow @Nullable
	private VertexBuffer cloudBuffer;
	@Shadow
	private int prevCloudX;
	@Shadow
	private int prevCloudY;
	@Shadow
	private int prevCloudZ;
	@Shadow
	private Vec3 prevCloudColor;
	@Shadow @Nullable
	private CloudStatus prevCloudsType;
	@Shadow @Final
	private static ResourceLocation CLOUDS_LOCATION;

	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	public void renderClouds(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, double camX, double camY, double camZ, CallbackInfo info) {
		info.cancel();
		float f = this.level.effects().getCloudHeight();
		if (!Float.isNaN(f)) {
			RenderSystem.disableCull();
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.depthMask(true);

			double cloudX = (camX / 12.0D) - ClientWindManager.getCloudX(partialTick);
			double cloudY = (double)(f - (float)camY + 0.33F) + ClientWindManager.getCloudY(partialTick);
			double cloudZ = (camZ / 12.0D + 0.33000001311302185D) - ClientWindManager.getCloudZ(partialTick);

			cloudX -= Mth.floor(cloudX / 2048.0D) * 2048;
			cloudZ -= Mth.floor(cloudZ / 2048.0D) * 2048;
			float l = (float)(cloudX - (double)Mth.floor(cloudX));
			float m = (float)(cloudY / 4.0D - (double)Mth.floor(cloudY / 4.0D)) * 4.0F;
			float n = (float)(cloudZ - (double)Mth.floor(cloudZ));
			Vec3 vec3 = this.level.getCloudColor(partialTick);
			int o = (int)Math.floor(cloudX);
			int p = (int)Math.floor(cloudY / 4.0D);
			int q = (int)Math.floor(cloudZ);
			if (o != this.prevCloudX || p != this.prevCloudY || q != this.prevCloudZ || this.minecraft.options.getCloudsType() != this.prevCloudsType || this.prevCloudColor.distanceToSqr(vec3) > 2.0E-4D) {
				this.prevCloudX = o;
				this.prevCloudY = p;
				this.prevCloudZ = q;
				this.prevCloudColor = vec3;
				this.prevCloudsType = this.minecraft.options.getCloudsType();
				this.generateClouds = true;
			}

			if (this.generateClouds) {
				this.generateClouds = false;
				BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
				if (this.cloudBuffer != null) {
					this.cloudBuffer.close();
				}

				this.cloudBuffer = new VertexBuffer();
				BufferBuilder.RenderedBuffer renderedBuffer = this.buildClouds(bufferBuilder, cloudX, cloudY, cloudZ, vec3);
				this.cloudBuffer.bind();
				this.cloudBuffer.upload(renderedBuffer);
				VertexBuffer.unbind();
			}

			RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
			RenderSystem.setShaderTexture(0, CLOUDS_LOCATION);
			FogRenderer.levelFogColor();
			poseStack.pushPose();
			poseStack.scale(12.0F, 1.0F, 12.0F);
			poseStack.translate(-l, m, -n);
			if (this.cloudBuffer != null) {
				this.cloudBuffer.bind();
				int r = this.prevCloudsType == CloudStatus.FANCY ? 0 : 1;

				for(int s = r; s < 2; ++s) {
					if (s == 0) {
						RenderSystem.colorMask(false, false, false, false);
					} else {
						RenderSystem.colorMask(true, true, true, true);
					}

					ShaderInstance shaderInstance = RenderSystem.getShader();
					this.cloudBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shaderInstance);
				}

				VertexBuffer.unbind();
			}

			poseStack.popPose();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
		}
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;", shift = At.Shift.AFTER))
	public void renderLevel(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo info) {
		if (this.level != null) {
			MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
			for (Entity entity : this.level.entitiesForRendering()) {
				Vec3 vec3 = camera.getPosition();
				double d = vec3.x();
				double e = vec3.y();
				double f = vec3.z();
				if (entity.tickCount == 0) {
					entity.xOld = entity.getX();
					entity.yOld = entity.getY();
					entity.zOld = entity.getZ();
				}
				this.renderEntityIcon(entity, d, e, f, partialTick, poseStack, bufferSource);
			}
		}
	}

	@Unique
	private void renderEntityIcon(Entity entity, double camX, double camY, double camZ, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
		double d = Mth.lerp(partialTick, entity.xOld, entity.getX());
		double e = Mth.lerp(partialTick, entity.yOld, entity.getY());
		double f = Mth.lerp(partialTick, entity.zOld, entity.getZ());
		float g = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
		((EntityRenderDispatcherWithIcon) this.entityRenderDispatcher).renderIcon(entity, d - camX, e - camY, f - camZ, g, partialTick, poseStack, bufferSource, this.entityRenderDispatcher.getPackedLightCoords(entity, partialTick));
	}

	@Shadow
	private BufferBuilder.RenderedBuffer buildClouds(BufferBuilder builder, double x, double y, double z, Vec3 cloudColor) {
		throw new AssertionError("Mixin injection failed - FrozenLib LevelRendererMixin");
	}

}
