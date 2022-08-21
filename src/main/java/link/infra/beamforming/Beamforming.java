package link.infra.beamforming;

import link.infra.beamforming.blocks.PrismBlock;
import link.infra.beamforming.blocks.PrismBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.block.extensions.api.client.BlockRenderLayerMap;
import org.quiltmc.qsl.item.setting.api.QuiltItemSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Beamforming implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Beamforming");

	private static String ID;

	private static Identifier id(String path) {
		return new Identifier(ID, path);
	}

	public static final Block PRISM = new PrismBlock();
	public static final BlockEntityType<PrismBlockEntity> PRISM_BE = FabricBlockEntityTypeBuilder.create(PrismBlockEntity::new, PRISM).build();

	@Override
	public void onInitialize(ModContainer mod) {
		ID = mod.metadata().id();

		Registry.register(Registry.BLOCK, id("prism"), PRISM);
		Registry.register(Registry.ITEM, id("prism"), new BlockItem(PRISM, new QuiltItemSettings().group(ItemGroup.TRANSPORTATION)));

		Registry.register(Registry.BLOCK_ENTITY_TYPE, id("prism"), PRISM_BE);

		BlockRenderLayerMap.put(RenderLayer.getTranslucent(), PRISM);
	}
}
