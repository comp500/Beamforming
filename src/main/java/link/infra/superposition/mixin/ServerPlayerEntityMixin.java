package link.infra.superposition.mixin;

import link.infra.superposition.blocks.BeamPathNode;
import link.infra.superposition.duck.ServerPlayerEntityExt;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements ServerPlayerEntityExt {
	private BeamPathNode<?> selectedBeamNode;

	@Override
	public BeamPathNode<?> getSelectedBeamNode() {
		return selectedBeamNode;
	}

	@Override
	public void setSelectedBeamNode(BeamPathNode<?> selectedBeamNode) {
		this.selectedBeamNode = selectedBeamNode;
	}
}
