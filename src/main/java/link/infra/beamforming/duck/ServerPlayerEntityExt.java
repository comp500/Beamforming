package link.infra.beamforming.duck;

import link.infra.beamforming.blocks.BeamPathNode;

public interface ServerPlayerEntityExt {
	BeamPathNode<?> getSelectedBeamNode();
	void setSelectedBeamNode(BeamPathNode<?> node);
}
