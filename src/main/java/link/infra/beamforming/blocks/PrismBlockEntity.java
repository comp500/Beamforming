package link.infra.beamforming.blocks;

import link.infra.beamforming.Beamforming;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.random.RandomGenerator;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PrismBlockEntity extends BlockEntity implements BeamPathNode.Holder, BeamPathNode.Hoverable {
	public final BeamPathNode<PrismBlockEntity> node = new BeamPathNode<>(this);

	// Common state (persisted and synced)
	public int hoverSeed = RandomGenerator.createLegacy().nextInt(Integer.MAX_VALUE);

	private boolean firstTick = true;

	public PrismBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(Beamforming.PRISM_BE, blockPos, blockState);
	}

	public static void tickServer(World world, BlockPos blockPos, BlockState blockState, PrismBlockEntity blockEntity) {
		if (blockEntity.firstTick) {
			blockEntity.firstTick = false;
			blockEntity.node.resolveCachedData(world);
		}
	}

	@Override
	public BeamPathNode<?> getNode() {
		return node;
	}

	@Override
	public boolean isInput() {
		// TODO: !isSource()
		return true;
	}

	@Override
	public boolean isOutput() {
		return true;
	}

	@Override
	public boolean isSource() {
		// TODO: update
		return true;
	}

	@Override
	public int getHoverSeed() {
		return hoverSeed;
	}

	@Override
	public void sync() {
		world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
	}

	@Nullable
	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.of(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt() {
		NbtCompound compound = new NbtCompound();
		compound.putInt("hoverSeed", hoverSeed);
		compound.put("node", node.writeNbtSync());
		return compound;
	}

	@Override
	protected void writeNbt(NbtCompound nbt) {
		nbt.putInt("hoverSeed", hoverSeed);
		nbt.put("node", node.writeNbtPersist());
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		if (nbt.contains("hoverSeed")) {
			hoverSeed = nbt.getInt("hoverSeed");
		}
		node.readNbt(nbt.getCompound("node"));
	}

	// TODO: update on break
}
