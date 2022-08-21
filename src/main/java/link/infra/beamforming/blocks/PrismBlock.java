package link.infra.beamforming.blocks;

import link.infra.beamforming.Beamforming;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.block.extensions.api.QuiltBlockSettings;

public class PrismBlock extends BlockWithEntity implements BlockEntityProvider {
	public PrismBlock() {
		// TODO: light up when receiving wave?
		super(QuiltBlockSettings.of(Material.GLASS).strength(1.0f).sounds(BlockSoundGroup.AMETHYST_BLOCK)
			.nonOpaque().solidBlock((state, world, pos) -> false).blockVision((state, world, pos) -> false));
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new PrismBlockEntity(pos, state);
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return world.isClient ? null : checkType(type, Beamforming.PRISM_BE, PrismBlockEntity::tickServer);
	}

	@Override
	public boolean isTranslucent(BlockState state, BlockView world, BlockPos pos) {
		return true;
	}

	@Override
	public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
		return 0.2f;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!world.isClient()) {
			if (world.getBlockEntity(pos) instanceof PrismBlockEntity pbe) {
				return pbe.getNode().activate((ServerPlayerEntity) player);
			}
		}
		return ActionResult.SUCCESS;
	}

	// TODO: update cached method
}
