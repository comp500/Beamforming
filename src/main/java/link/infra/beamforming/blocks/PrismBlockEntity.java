package link.infra.beamforming.blocks;

import link.infra.beamforming.Beamforming;
import net.minecraft.block.BeaconBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.random.RandomGenerator;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PrismBlockEntity extends BlockEntity implements BeamPathNode.Holder, BeamPathNode.Hoverable {
	public final BeamPathNode<PrismBlockEntity> node = new BeamPathNode<>(this);

	// Common state (persisted and synced)
	public int hoverSeed = RandomGenerator.createLegacy().nextInt(Integer.MAX_VALUE);
	public boolean source = false;

	// Server state (not persisted)
	private boolean firstTick = true;
	private int ticks = 0;

	public PrismBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(Beamforming.PRISM_BE, blockPos, blockState);
	}

	public static void tickServer(World world, BlockPos blockPos, BlockState blockState, PrismBlockEntity be) {
		if (be.firstTick) {
			be.firstTick = false;
			be.node.resolveCachedData(world);
		}
		be.ticks++;
		if (be.ticks % 10 == 0) {
			// Check for beacon below
			BlockPos.Mutable mutPos = blockPos.mutableCopy();
			boolean foundBeacon = false;
			while (true) {
				mutPos.move(0, -1, 0);
				if (world.isOutOfHeightLimit(mutPos)) break;

				BlockState bs = world.getBlockState(mutPos);
				if (bs.isAir()) continue;
				if (bs.getOpacity(world, mutPos) >= 15) break;

				if (bs.getBlock() instanceof BeaconBlock) {
					if (world.getBlockEntity(mutPos) instanceof BeaconBlockEntity beaconBe) {
						boolean requiresPathUpdate = false;
						int currY = mutPos.getY();
						for (BeaconBlockEntity.BeamSegment seg : beaconBe.getBeamSegments()) {
							currY += seg.getHeight();
							if (currY > blockPos.getY()) {
								System.arraycopy(seg.getColor(), 0, be.node.color, 0, 3);
								requiresPathUpdate = true;
								break;
							}
						}

						foundBeacon = true;
						if (!be.source) {
							be.source = true;
							be.node.updateMode();
						} else if (requiresPathUpdate) {
							be.node.updateColor();
						}
						BlockPos teleportPos = be.node.getFinalDestPos();
						if (teleportPos != null) {
							// Make the box actually greater than 0 width/depth
							mutPos.move(1, 1, 1);
							// Get entities and teleport them
							List<Entity> ents = world.getOtherEntities(null, new Box(blockPos, mutPos), EntityPredicates.VALID_ENTITY);
							for (Entity ent : ents) {
								ent.teleport(teleportPos.getX(), teleportPos.getY() + 1, teleportPos.getZ());
							}
						}
					}
					break;
				}
			}
			if (!foundBeacon) {
				be.source = false;
				be.node.updateMode();
			}
		}
	}

	@Override
	public BeamPathNode<?> getNode() {
		return node;
	}

	@Override
	public boolean isInput() {
		return !isSource();
	}

	@Override
	public boolean isOutput() {
		return true;
	}

	@Override
	public boolean isSource() {
		return source;
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
		compound.putBoolean("source", source);
		compound.put("node", node.writeNbtSync());
		return compound;
	}

	@Override
	protected void writeNbt(NbtCompound nbt) {
		nbt.putInt("hoverSeed", hoverSeed);
		nbt.putBoolean("source", source);
		nbt.put("node", node.writeNbtPersist());
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		if (nbt.contains("hoverSeed")) {
			hoverSeed = nbt.getInt("hoverSeed");
		}
		if (nbt.contains("source")) {
			source = nbt.getBoolean("source");
		}
		node.readNbt(nbt.getCompound("node"));
	}

	// TODO: update on break
}
