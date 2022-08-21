package link.infra.beamforming.blocks;

import net.minecraft.util.math.BlockPos;

public interface BeamPathNodeEntity {
	void bindTo(BeamPathNodeEntity node);
	void attachSource(BeamPathNodeEntity node);
	void detachSource(BeamPathNodeEntity node);
	BlockPos getPos();
}
