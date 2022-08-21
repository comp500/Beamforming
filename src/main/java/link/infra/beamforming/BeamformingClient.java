package link.infra.beamforming;

import link.infra.beamforming.blocks.PrismBlockEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.block.extensions.api.client.BlockRenderLayerMap;

import static link.infra.beamforming.Beamforming.PRISM;

public class BeamformingClient implements ClientModInitializer {
	private static String ID;

	private static Identifier id(String path) {
		return new Identifier(ID, path);
	}

	@Override
	public void onInitializeClient(ModContainer mod) {
		ID = mod.metadata().id();

		BlockEntityRendererRegistry.register(Beamforming.PRISM_BE, PrismBlockEntityRenderer::new);

		ClientSpriteRegistryCallback.event(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).register((atlasTexture, registry) -> {
			registry.register(id("block/beam"));
			registry.register(id("block/beam_end"));
		});

		BlockRenderLayerMap.put(RenderLayer.getTranslucent(), PRISM);
	}
}
