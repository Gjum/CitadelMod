package gjum.minecraft.civ.citadelmod.common;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

public class CitadelMod {
	private static CitadelMod INSTANCE;

	@Nullable
	private CitadelSqliteDb db = null;

	public static CitadelMod getMod() {
		return INSTANCE;
	}

	public CitadelMod() {
		if (INSTANCE != null) throw new IllegalStateException("Constructor called twice");
		INSTANCE = this;
	}

	public void init() {
	}

	public void handleConnectedToServer() {
		if (db != null) db.close();
		db = null;
		final ServerData currentServer = Minecraft.getInstance().getCurrentServer();
		if (currentServer == null) return;
		String server = currentServer.ip;
		db = new CitadelSqliteDb(server);
	}

	public void handleDisconnectedFromServer() {
		if (db != null) db.close();
		db = null;
	}

	public void handleBlockChange(BlockPos pos) {
		if (db == null) return;
		Level w = Minecraft.getInstance().level;
		if (w == null) return;
		Block type = w.getBlockState(pos).getBlock();
		if (type == Blocks.AIR) {
			db.deleteBlockInfo(pos);
			// TODO do we need to reintroduce the reinforcement if this block gets set to non-air again in a moment?
		}
	}

	/**
	 * Returns true when the packet should be dropped
	 */
	public boolean handleChat(Component message) {
		if (db == null) return false;
		BlockInfo info = ChatParser.parseChat(message, this);
		if (info != null) {
			// we could check if anything changed, but this happens only on mouse click, so very infrequently
			db.upsertBlockInfo(info);
		}
		return false;
	}

	public void handleRenderBlockOverlay(PoseStack matrices, float partialTicks) {
		if (db == null) return;
		OverlayRenderer.renderOverlay(matrices, partialTicks);
	}

	@Nullable
	public BlockInfo getBlockInfo(int x, int y, int z) {
		if (db == null) return null;
		return db.getBlockInfo(new BlockPos(x, y, z));
	}
}
