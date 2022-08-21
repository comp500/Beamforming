package link.infra.superposition.blocks;

public class BindingHandler {
	private BindingHandler() {}

	public static BindingHandler INSTANCE = new BindingHandler();

	public BeamPathNodeEntity selectedNode = null;
}
