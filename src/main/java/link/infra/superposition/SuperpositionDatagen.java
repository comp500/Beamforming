package link.infra.superposition;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.model.BlockStateModelGenerator;

public class SuperpositionDatagen implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		fabricDataGenerator.addProvider(ModelProvider::new);
	}

	private static class ModelProvider extends FabricModelProvider {
		public ModelProvider(FabricDataGenerator dataGenerator) {
			super(dataGenerator);
		}

		@Override
		public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
			blockStateModelGenerator.registerSimpleCubeAll(Superposition.PRISM);
		}

		@Override
		public void generateItemModels(ItemModelGenerator itemModelGenerator) {
		}
	}
}
