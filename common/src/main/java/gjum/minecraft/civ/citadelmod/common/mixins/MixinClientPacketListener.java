package gjum.minecraft.civ.citadelmod.common.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static gjum.minecraft.civ.citadelmod.common.CitadelMod.getMod;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
	@Inject(method = "handleChat", at = @At("HEAD"), cancellable = true)
	protected void onHandleChat(ClientboundChatPacket packet, CallbackInfo ci) {
		if (!Minecraft.getInstance().isSameThread()) {
			// waiting for mc to call this again from the mc thread, so we can cancel it properly
			return; // continue method normally
		}
		try {
			boolean dropPacket = getMod().handleChat(packet.getMessage());
			if (dropPacket) ci.cancel();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Inject(method = "handleBlockUpdate", at = @At("HEAD"))
	protected void onHandleBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
		Minecraft.getInstance().execute(() -> {
			try {
				getMod().handleBlockChange(packet.getPos());
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
	}

	@Inject(method = "handleChunkBlocksUpdate", at = @At("HEAD"))
	protected void onHandleChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
		Minecraft.getInstance().execute(() -> {
			try {
				packet.runUpdates((blockPos, blockState) -> {
					getMod().handleBlockChange(blockPos);
				});
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
	}

	@Inject(method = "handleLogin", at = @At("HEAD"))
	protected void onHandleLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
		Minecraft.getInstance().execute(() -> {
			try {
				getMod().handleConnectedToServer();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
	}

	@Inject(method = "onDisconnect", at = @At("HEAD"))
	protected void onOnDisconnect(Component component, CallbackInfo ci) {
		Minecraft.getInstance().execute(() -> {
			try {
				getMod().handleDisconnectedFromServer();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
	}
}
