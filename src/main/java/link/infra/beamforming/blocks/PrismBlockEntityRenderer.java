package link.infra.beamforming.blocks;

import com.mojang.blaze3d.vertex.VertexConsumer;
import link.infra.beamforming.duck.Matrix4fExt;
import link.infra.beamforming.mixin.GameRendererAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.random.RandomGenerator;

public class PrismBlockEntityRenderer implements BlockEntityRenderer<PrismBlockEntity> {
	private final BlockEntityRenderDispatcher berd;
	private final BlockRenderManager brm;
	private final GameRenderer gameRenderer;

	public PrismBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
		this.berd = ctx.getRenderDispatcher();
		this.brm = ctx.getRenderManager();
		this.gameRenderer = MinecraftClient.getInstance().gameRenderer;
	}

	private static final Direction[] DIRS = new Direction[]{
		Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null
	};

	private static void renderModelUnlit(BakedModel model, BlockState state, RandomGenerator random, VertexConsumer vc, MatrixStack matrices, int light, int overlay) {
		MatrixStack.Entry entry = matrices.peek();
		for (Direction dir : DIRS) {
			for (BakedQuad quad : model.getQuads(state, dir, random)) {
				vc.bakedQuad(entry, quad, 1f, 1f, 1f, light, overlay);
			}
		}
	}

	@Override
	public void render(PrismBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		int hoverState = (entity.hoverSeed + ((GameRendererAccessor)gameRenderer).getTicks()) % 1000000;
		// TODO: get and use dest hover state
		float hoverOffset = MathHelper.sin((hoverState + tickDelta) / 15.0f) / 10.0f + 0.1f;

		matrices.push();
		matrices.translate(0, hoverOffset, 0);
		matrices.translate(0.5, 0.5, 0.5);
		matrices.multiply(Vec3f.NEGATIVE_Y.getRadialQuaternion((hoverState + tickDelta) / 15.0f));

		// Cube where two corners are vertically aligned
		matrices.multiply(Vec3f.NEGATIVE_Z.getDegreesQuaternion(55));
		matrices.multiply(Vec3f.NEGATIVE_Y.getDegreesQuaternion(45));
		matrices.scale(0.6f, 0.6f, 0.6f);
		matrices.translate(-0.5, -0.5, -0.5);

		var state = entity.getCachedState();
		renderModelUnlit(brm.getModel(state), state, RandomGenerator.createLegacy(),
			vertexConsumers.getBuffer(RenderLayer.getTranslucentMovingBlock()), matrices, light, overlay);

		matrices.pop();

		if (entity.node.hasPower()) {
			int r, g, b;
			r = (int) (entity.node.color[0] * 255f);
			g = (int) (entity.node.color[1] * 255f);
			b = (int) (entity.node.color[2] * 255f);

			var cameraVec = berd.camera.getPos().subtract(Vec3d.ofCenter(entity.getPos())).normalize();
			renderLight(cameraVec, hoverOffset, matrices, vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, r, g, b);

			var destPos = entity.node.getDest();
			if (destPos != null) {
				var beamVec = destPos.subtract(entity.getPos());
				renderBeam(beamVec, cameraVec, hoverOffset, matrices, vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, r, g, b);
			}
		}
	}

	private Matrix4f rotationFromBasis(Vec3f x, Vec3f y, Vec3f z) {
		Matrix4f rotMatrix = new Matrix4f();
		((Matrix4fExt) (Object) rotMatrix).set(
			x.getX(), x.getY(), x.getZ(), 0f,
			y.getX(), y.getY(), y.getZ(), 0f,
			z.getX(), z.getY(), z.getZ(), 0f,
			0f, 0f, 0f, 1f
		);
		rotMatrix.invert();
		return rotMatrix;
	}

	private void renderBeam(BlockPos beamVec, Vec3d cameraVec, float hoverOffset, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int r, int g, int b) {
		var len = MathHelper.sqrt(
			beamVec.getX() * beamVec.getX() +
				beamVec.getY() * beamVec.getY() +
				beamVec.getZ() * beamVec.getZ());

		var dirVec = Vec3d.of(beamVec).add(0, hoverOffset, 0).normalize();
		// Cross, to get vector pointing upwards (perp. to camera and dir vec)
		var upVec = cameraVec.crossProduct(dirVec).normalize();
		// Cross again, to get the vector perpendicular to the dir vector and the up vector (facing out, not affected by movement parallel to the dir vec)
		var outVec = upVec.crossProduct(dirVec).normalize();
		var rot = rotationFromBasis(new Vec3f(dirVec), new Vec3f(upVec), new Vec3f(outVec));

		matrices.push();
		matrices.translate(0, hoverOffset, 0);
		matrices.translate(0.5, 0.5, 0.5);
		matrices.multiplyMatrix(rot);
		matrices.scale(1, 0.3f, 1);
		matrices.translate(0, -0.5, -0.5);

		matrices.scale(len, 1, 1);

		var vc = vertexConsumers.getBuffer(RenderLayer.getTranslucentMovingBlock());
		var mat = matrices.peek().getPosition();
		Sprite sprite = MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(new Identifier("beamforming:block/beam"));
		vc.vertex(mat, 0, 0, 0.5f).color(r, g, b, 255).uv(sprite.getMinU(), sprite.getMinV()).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 0).next();
		vc.vertex(mat, 1, 0, 0.5f).color(r, g, b, 30).uv(sprite.getMaxU(), sprite.getMinV()).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 0).next();
		vc.vertex(mat, 1, 1, 0.5f).color(r, g, b, 30).uv(sprite.getMaxU(), sprite.getMaxV()).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 0).next();
		vc.vertex(mat, 0, 1, 0.5f).color(r, g, b, 255).uv(sprite.getMinU(), sprite.getMaxV()).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 0).next();

		matrices.pop();
	}

	private void renderLight(Vec3d cameraVec, float hoverOffset, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int r, int g, int b) {
		var rightVec = new Vec3d(0, 1, 0).crossProduct(cameraVec).normalize();
		var upVec = cameraVec.crossProduct(rightVec).normalize();
		var rot = rotationFromBasis(new Vec3f(rightVec), new Vec3f(upVec), new Vec3f(cameraVec));

		matrices.push();
		matrices.translate(0, hoverOffset, 0);
		matrices.translate(0.5, 0.5, 0.5);
		matrices.multiplyMatrix(rot);
		matrices.scale(0.4f, 0.4f, 0.4f);
		matrices.translate(-0.5, -0.5, 0.1);

		Sprite spriteEnd = MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(new Identifier("beamforming:block/beam_end"));
		var vc = vertexConsumers.getBuffer(RenderLayer.getTranslucentMovingBlock());
		var mat = matrices.peek().getPosition();
		vc.vertex(mat, 0, 0, 0.1f).color(r, g, b, 255).uv(spriteEnd.getMinU(), spriteEnd.getMinV()).light(light).normal(0, 0, 0).next();
		vc.vertex(mat, 1, 0, 0.1f).color(r, g, b, 255).uv(spriteEnd.getMaxU(), spriteEnd.getMinV()).light(light).normal(0, 0, 0).next();
		vc.vertex(mat, 1, 1, 0.1f).color(r, g, b, 255).uv(spriteEnd.getMaxU(), spriteEnd.getMaxV()).light(light).normal(0, 0, 0).next();
		vc.vertex(mat, 0, 1, 0.1f).color(r, g, b, 255).uv(spriteEnd.getMinU(), spriteEnd.getMaxV()).light(light).normal(0, 0, 0).next();

		matrices.pop();
	}

	@Override
	public boolean rendersOutsideBoundingBox(PrismBlockEntity blockEntity) {
		return true;
	}

	// TODO: render distance?
}
