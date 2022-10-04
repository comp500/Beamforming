package link.infra.beamforming.blocks;

import link.infra.beamforming.duck.ServerPlayerEntityExt;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class BeamPathNode<T extends BlockEntity & BeamPathNode.Holder> {
	public final T entity;

	// Common state (persisted + synced)
	private BlockPos dest;
	private int destHoverSeed = -1;
	private boolean connected = false;
	public final float[] color = new float[] {0f, 0f, 0f};

	// Server state (persisted)
	private final List<BlockPos> sources = new ArrayList<>();

	// Server state (not persisted)
	private final List<BeamPathNode<?>> cachedSources = new ArrayList<>();
	private BeamPathNode<?> cachedDest;

	public BeamPathNode(T ent) {
		this.entity = ent;
	}

	public void readNbt(NbtCompound data) {
		if (data.contains("destX") && data.contains("destY") && data.contains("destZ")) {
			dest = new BlockPos(data.getInt("destX"), data.getInt("destY"), data.getInt("destZ"));
		} else {
			dest = null;
		}
		if (data.contains("destHoverSeed")) {
			destHoverSeed = data.getInt("destHoverSeed");
		}
		if (data.contains("connected")) {
			connected = data.getBoolean("connected");
		} else {
			connected = false;
		}
		if (data.contains("color")) {
			int[] colorInts = data.getIntArray("color");
			for (int i = 0; i < 3; i++) {
				color[i] = Float.intBitsToFloat(colorInts[i]);
			}
		} else {
			Arrays.fill(color, 0);
		}
		sources.clear();
		if (data.contains("sources")) {
			int[] sourcePosInts = data.getIntArray("sources");
			for (int i = 0; i < sourcePosInts.length - 2; i += 3) {
				sources.add(new BlockPos(sourcePosInts[i], sourcePosInts[i + 1], sourcePosInts[i + 2]));
			}
		}
	}

	public void resolveCachedData(World world) {
		if (dest != null && world.getBlockEntity(dest) instanceof BeamPathNode.Holder be) {
			cachedDest = be.getNode();
		} else {
			dest = null;
		}

		sources.removeIf(pos -> {
			if (world.getBlockEntity(pos) instanceof BeamPathNode.Holder source) {
				cachedSources.add(source.getNode());
				return false;
			} else {
				return true;
			}
		});
	}

	public NbtCompound writeNbtSync() {
		NbtCompound compound = new NbtCompound();
		if (dest != null) {
			compound.putInt("destX", dest.getX());
			compound.putInt("destY", dest.getY());
			compound.putInt("destZ", dest.getZ());
		}
		if (destHoverSeed > -1) {
			compound.putInt("destHoverSeed", destHoverSeed);
		}
		if (connected) {
			compound.putBoolean("connected", true);
		}
		int[] colorInts = new int[3];
		for (int i = 0; i < 3; i++) {
			colorInts[i] = Float.floatToIntBits(color[i]);
		}
		compound.putIntArray("color", colorInts);
		return compound;
	}

	public NbtCompound writeNbtPersist() {
		NbtCompound compound = writeNbtSync();
		if (!sources.isEmpty()) {
			List<Integer> sourcePosInts = new ArrayList<>();
			for (BlockPos pos : sources) {
				sourcePosInts.add(pos.getX());
				sourcePosInts.add(pos.getY());
				sourcePosInts.add(pos.getZ());
			}
			compound.putIntArray("sources", sourcePosInts);
		}
		return compound;
	}

	interface Holder {
		BeamPathNode<?> getNode();
		boolean isInput();
		boolean isOutput();
		boolean isSource();
		void sync();
		void markDirty();
	}

	interface Hoverable {
		int getHoverSeed();
	}

	public boolean hasPower() {
		return entity.isSource() || connected;
	}

	public boolean isValid() {
		return !entity.isRemoved();
	}

	public void updateMode() {
		if (!entity.isInput() || !isValid()) {
			for (BeamPathNode<?> source : cachedSources) {
				source.unbindDest();
			}
			sources.clear();
			cachedSources.clear();
		}
		if (!entity.isOutput() || !isValid()) {
			if (cachedDest != null) {
				cachedDest.unbindSource(this);
			}
			dest = null;
			cachedDest = null;
		}
		updatePath();
	}

	private void updatePath() {
		if (entity.getWorld().isClient()) {
			throw new IllegalStateException("Cannot update path on client");
		}
		// TODO: improve performance, allow use without chunkloading?
		updatePathRecurseUp(new HashSet<>());
	}

	private void updatePathRecurseUp(Set<BeamPathNode<?>> sourcesEncountered) {
		sourcesEncountered.add(this);
		if (cachedDest != null && !sourcesEncountered.contains(cachedDest)) {
			// Start at end of path, work backwards
			cachedDest.updatePathRecurseUp(sourcesEncountered);
		} else {
			updatePathRecurseDown(new HashSet<>());
		}
	}

	private final Set<BeamPathNode<?>> queuedUnbinds = new HashSet<>();

	private void updatePathRecurseDown(Set<BeamPathNode<?>> sourcesEncountered) {
		connected = false;
		for (BeamPathNode<?> source : cachedSources) {
			if (!source.isValid() || sourcesEncountered.contains(source)) {
				// Have previously encountered this node - can't be removed here, must be done while not iterating
				source.unbindDest();
				queuedUnbinds.add(source);
			} else {
				sourcesEncountered.add(source);
				source.updatePathRecurseDown(sourcesEncountered);

				if (queuedUnbinds.contains(source)) continue;

				if (source.hasPower()) {
					if (!connected) {
						System.arraycopy(source.color, 0, color, 0, 3);
					} else {
						color[0] = (color[0] + source.color[0]) / 2f;
						color[1] = (color[1] + source.color[1]) / 2f;
						color[2] = (color[2] + source.color[2]) / 2f;
					}
					connected = true;
				}
			}
		}
		if (!sourcesEncountered.contains(this)) {
			cachedSources.removeAll(queuedUnbinds);
			queuedUnbinds.clear();
		}
		entity.sync();
		entity.markDirty();
	}

	private void bindTo(BeamPathNode<?> node) {
		if (entity.isOutput()) {
			dest = node.entity.getPos();
			cachedDest = node;
			if (node.entity instanceof Hoverable) {
				destHoverSeed = ((Hoverable) node.entity).getHoverSeed();
			}
			if (cachedSources.contains(node)) {
				sources.remove(node.entity.getPos());
				cachedSources.remove(node);
			}
			// Calls updatePath
			node.bindSource(this);
		}
	}

	private void unbindDest() {
		dest = null;
		cachedDest = null;
		// No updatePath as sources don't change (unbindSource will handle dest)
		entity.sync();
		entity.markDirty();
	}

	private void bindSource(BeamPathNode<?> node) {
		if (cachedDest == node) {
			unbindDest();
		}
		if (!cachedSources.contains(node)) {
			sources.add(node.entity.getPos());
			cachedSources.add(node);
		}
		updatePath();
	}

	private void unbindSource(BeamPathNode<?> node) {
		sources.remove(node.entity.getPos());
		cachedSources.remove(node);
		updatePath();
	}

	public BlockPos getDest() {
		return dest;
	}

	public BlockPos getFinalDestPos() {
		if (cachedDest != null) {
			BeamPathNode<?> node = cachedDest;
			while (node.cachedDest != null) {
				node = node.cachedDest;
			}
			return node.entity.getPos();
		}
		return null;
	}

	public ActionResult activate(ServerPlayerEntity player) {
		if (!player.getAbilities().allowModifyWorld) {
			return ActionResult.FAIL;
		}
		ServerPlayerEntityExt pl = ((ServerPlayerEntityExt)player);
		BeamPathNode<?> selectedNode = pl.getSelectedBeamNode();
		if (selectedNode == null) {
			pl.setSelectedBeamNode(this);
			if (cachedDest != null) {
				cachedDest.unbindSource(this);
				unbindDest();
			}
			player.networkHandler.sendPacket(new OverlayMessageS2CPacket(Text.translatable("block.beamforming.prism.binding")));
		} else if (selectedNode == this) {
			pl.setSelectedBeamNode(null);
			player.networkHandler.sendPacket(new OverlayMessageS2CPacket(Text.translatable("block.beamforming.prism.recursion")));
		} else {
			pl.setSelectedBeamNode(null);

			// TODO: check range, same dimension
			if (entity.isInput() && selectedNode.entity.isOutput()) {
				selectedNode.bindTo(this);
				player.networkHandler.sendPacket(new OverlayMessageS2CPacket(Text.translatable("block.beamforming.prism.bound")));
			} else if (entity.isOutput() && selectedNode.entity.isInput()) {
				bindTo(selectedNode);
				player.networkHandler.sendPacket(new OverlayMessageS2CPacket(Text.translatable("block.beamforming.prism.bound")));
			} else {
				player.networkHandler.sendPacket(new OverlayMessageS2CPacket(Text.translatable("block.beamforming.prism.incompatible")));
			}
			// TODO: check for loops?
		}
		return ActionResult.SUCCESS;
	}

	public void updateColor() {
		updatePath();
	}
}
