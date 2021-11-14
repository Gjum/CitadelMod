package gjum.minecraft.civ.citadelmod.fabric;

import gjum.minecraft.civ.citadelmod.common.CitadelMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class FabricCitadelMod extends CitadelMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STARTED.register(e -> FabricCitadelMod.this.init());
		WorldRenderEvents.BEFORE_ENTITIES.register(((context) -> {
			try {
				handleRenderBlockOverlay(context.matrixStack(), context.tickDelta());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));
	}
}
