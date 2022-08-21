package link.infra.superposition;

import link.infra.superposition.blocks.PrismBlockEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

public class SuperpositionClient implements ClientModInitializer {
	@Override
	public void onInitializeClient(ModContainer mod) {
		BlockEntityRendererRegistry.register(Superposition.PRISM_BE, PrismBlockEntityRenderer::new);

		ClientSpriteRegistryCallback.event(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).register((atlasTexture, registry) -> {
			registry.register(new Identifier("superposition:block/beam"));
			registry.register(new Identifier("superposition:block/beam_end"));
		});
	}
}
