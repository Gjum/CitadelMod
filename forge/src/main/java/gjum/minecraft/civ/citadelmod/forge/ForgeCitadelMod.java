package gjum.minecraft.civ.citadelmod.forge;

import gjum.minecraft.civ.citadelmod.common.CitadelMod;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("citadelmod")
public class ForgeCitadelMod extends CitadelMod {
	public ForgeCitadelMod() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
		MinecraftForge.EVENT_BUS.register(this);
	}

	public void clientSetup(FMLClientSetupEvent event) {
		init();
	}

	@SubscribeEvent
	public void onRenderWorldLast(RenderWorldLastEvent event) {
		try {
			handleRenderBlockOverlay(event.getMatrixStack(), event.getPartialTicks());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
