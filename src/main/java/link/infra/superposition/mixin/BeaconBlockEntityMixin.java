package link.infra.superposition.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BeaconBlockEntity.class)
public class BeaconBlockEntityMixin {
	@ModifyExpressionValue(method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getTopY(Lnet/minecraft/world/Heightmap$Type;II)I"))
	private static int superposition_prismBeaconTop(int value) {
		return 85;
	}
}
