package link.infra.superposition.duck;

import link.infra.superposition.blocks.BeamPathNode;

public interface ServerPlayerEntityExt {
	BeamPathNode<?> getSelectedBeamNode();
	void setSelectedBeamNode(BeamPathNode<?> node);
}
