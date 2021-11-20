package gjum.minecraft.civ.citadelmod.common;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Transformation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import static gjum.minecraft.civ.citadelmod.common.CitadelMod.getMod;

public class OverlayRenderer {
	private final static Minecraft mc = Minecraft.getInstance();

	/**
	 * how far (from the player) block infos will be shown
	 */
	public static int range = 10;

	public static void renderOverlay(PoseStack matrices, float partialTicks) {
		if (mc.player == null) return;

		final Level level = Minecraft.getInstance().level;
		if (level == null) return;

		RenderSystem.pushMatrix();

		final Camera camera = mc.gameRenderer.getMainCamera();
		final Vec3 camPos = camera.getPosition();

		RenderSystem.multMatrix(matrices.last().pose());
		RenderSystem.translated(-camPos.x(), -camPos.y(), -camPos.z());

		final BlockState bs = Blocks.VOID_AIR.defaultBlockState(); // only needed to call an instance method that might as well be static

		final BlockPos playerPos = mc.player.blockPosition();

		final long maxAge = System.currentTimeMillis() - getMod().getConfigMaxAge();

		for (int x = -range; x <= range; ++x) {
			for (int y = -range; y <= range; ++y) {
				for (int z = -range; z <= range; ++z) {
					final BlockInfo info = getMod().getBlockInfo(x + playerPos.getX(), y + playerPos.getY(), z + playerPos.getZ());
					if (info == null) continue;

					if (info.getLastCheckedTs() < maxAge) continue;

					final BlockPos p = info.getBlockPos();

					RenderSystem.pushMatrix();

					// go to center of block, so we can then rotate around it
					RenderSystem.translated(p.getX() + .5, p.getY() + .5, p.getZ() + .5);

					if (!bs.isSolidRender(level, p.north()))
						renderBlockInfoFace(info);
					// turn to next side face
					RenderSystem.rotatef(90F, 0F, 1F, 0F);
					if (!bs.isSolidRender(level, p.west()))
						renderBlockInfoFace(info);
					RenderSystem.rotatef(90F, 0F, 1F, 0F);
					if (!bs.isSolidRender(level, p.south()))
						renderBlockInfoFace(info);
					RenderSystem.rotatef(90F, 0F, 1F, 0F);
					if (!bs.isSolidRender(level, p.east()))
						renderBlockInfoFace(info);

					// align text to closest 90° facing player
					final int angleAdj = 90 + 45 - (int) mc.player.yRot;
					final int angleRound = Math.floorDiv(angleAdj, 90) * 90;
					RenderSystem.rotatef(angleRound, 0F, 1F, 0F);
					// turn from side to top face
					RenderSystem.rotatef(90F, 1F, 0F, 0F);
					if (!bs.isSolidRender(level, p.above()))
						renderBlockInfoFace(info);
					// tun from top to bottom face
					RenderSystem.rotatef(180F, 1F, 0F, 0F);
					if (!bs.isSolidRender(level, p.below()))
						renderBlockInfoFace(info);

					RenderSystem.popMatrix();
				}
			}
		}

		RenderSystem.popMatrix();

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	static void renderBlockInfoFace(BlockInfo info) {
		RenderSystem.pushMatrix();

		// we're at the center of a block; move to just outside the block face
		RenderSystem.translated(0, 0, -.502);

		RenderSystem.disableTexture();

		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false); // otherwise water/chests are messed up

		// need blend for alpha
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableAlphaTest();

		final float cornerSize = .2f;
		final float cornerOpacity = .4f;

		// colors follow ctr particles; except unreinforced which is yellow like the chat msg
		if ("Stone".equals(info.getReinforcement()))
			renderCornerTriangles(cornerSize, .5f, 0, 1, cornerOpacity);
		else if ("Iron".equals(info.getReinforcement()))
			renderCornerTriangles(cornerSize, 1, 1, 1, cornerOpacity);
		else if ("Diamond".equals(info.getReinforcement()))
			renderCornerTriangles(cornerSize, 0, .5f, 1, cornerOpacity);
		else if ("Paper".equals(info.getReinforcement()))
			renderCornerTriangles(cornerSize, 0, .7f, 0, cornerOpacity);
		else if ("Bedrock".equals(info.getReinforcement()))
			renderCornerTriangles(cornerSize, 0, 0, 0, cornerOpacity);
		else
			renderCornerTriangles(cornerSize, 1, 1, 0, cornerOpacity);

		RenderSystem.enableTexture();

		final String group = info.getGroup();
		if (group != null && !group.isEmpty()) {
			final int color = 0xffffff; // TODO based on group; good/bad/etc
			drawTextCentered(group, color, 1, .3f, .5f);
		} else if (info.getReinforcement() != null && !info.getReinforcement().isEmpty()) {
			final int color = 0xff0000;
			drawTextCentered("(hostile)", color, 1, .3f, .5f);
		}

		String healthText = String.format("%d/%d", info.getHealth(), info.getHealthMax());

		int healthColor = 0xaaaaaa;
		if (info.getHealth() < info.getHealthMax()) healthColor = 0xffffff;
		if (info.getHealth() < info.getHealthMax() / 2) healthColor = 0xffff00;
		if (info.getHealth() < info.getHealthMax() / 5) healthColor = 0xff0000;

		RenderSystem.translatef(0, -.5f + cornerSize / 2, 0);
		drawTextCentered(healthText, healthColor, 1 - cornerSize, .2f, 1);

		RenderSystem.depthMask(true);
		RenderSystem.popMatrix();
	}

	static void drawTextCentered(String text, int color, float maxWidth, float maxHeight, float offset) {
		final Matrix4f I = Transformation.identity().getMatrix();
		final boolean shadow = false;
		final boolean seeThrough = false;
		final int backdropColor = 0x40_000000;
		final int borderSize = 1;

		final int w = mc.font.width(text) + borderSize + borderSize;
		float scale = maxWidth / w;
		if (scale * mc.font.lineHeight > maxHeight) scale = maxHeight / mc.font.lineHeight;
		RenderSystem.pushMatrix();
		RenderSystem.scalef(-scale, -scale, scale);

		MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		mc.font.drawInBatch(text, borderSize - w / 2f, -offset * mc.font.lineHeight,
				color, shadow, I, bufferSource, seeThrough, backdropColor, 0xf0_00_f0);
		bufferSource.endBatch();
		RenderSystem.popMatrix();
	}

	/**
	 * triangles in corners of xz-square centered around 0,0,0.
	 *
	 * @param size 0.5 makes triangle corners touch
	 */
	static void renderCornerTriangles(float size, float r, float g, float b, float a) {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.getBuilder();
		bufferBuilder.begin(GL11.GL_TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

		bufferBuilder.vertex(-.5f, -.5f, 0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(-.5f, -.5f + size, 0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(-.5f + size, -.5f, 0).color(r, g, b, a).endVertex();

		bufferBuilder.vertex(-.5f, .5f, 0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(-.5f + size, .5f, 0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(-.5f, .5f - size, 0).color(r, g, b, a).endVertex();

		bufferBuilder.vertex(.5f, .5f, 0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(.5f, .5f - size, 0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(.5f - size, .5f, 0).color(r, g, b, a).endVertex();

		bufferBuilder.vertex(.5f, -.5f, 0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(.5f - size, -.5f, 0).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(.5f, -.5f + size, 0).color(r, g, b, a).endVertex();

		tesselator.end();
	}
}
