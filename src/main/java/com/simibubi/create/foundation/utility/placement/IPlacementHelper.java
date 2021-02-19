package com.simibubi.create.foundation.utility.placement;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Pair;
import com.simibubi.create.foundation.utility.VecHelper;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@MethodsReturnNonnullByDefault
public interface IPlacementHelper {

	/**
	 * used as an identifier in SuperGlueHandler to skip blocks placed by helpers
	 */
	BlockState ID = new BlockState(Blocks.AIR, null);

	/**
	 * @return a predicate that gets tested with the items held in the players hands,
	 * should return true if this placement helper is active with the given item
	 */
	Predicate<ItemStack> getItemPredicate();

	/**
	 * @return a predicate that gets tested with the blockstate the player is looking at
	 * should return true if this placement helper is active with the given blockstate
	 */
	Predicate<BlockState> getStatePredicate();

	/**
	 * @return PlacementOffset.fail() if no valid offset could be found.
	 * PlacementOffset.success(newPos) with newPos being the new position the block should be placed at
	 */
	PlacementOffset getOffset(World world, BlockState state, BlockPos pos, BlockRayTraceResult ray);

	//overrides the default ghost state of the helper with the actual state of the held block item, this is used in PlacementHelpers and can be ignored in most cases
	default PlacementOffset getOffset(World world, BlockState state, BlockPos pos, BlockRayTraceResult ray, ItemStack heldItem) {
		PlacementOffset offset = getOffset(world, state, pos, ray);
		if (heldItem.getItem() instanceof BlockItem) {
			BlockItem blockItem = (BlockItem) heldItem.getItem();
			offset = offset.withGhostState(blockItem.getBlock().getDefaultState());
		}
		return offset;
	}

	//only gets called when placementOffset is successful
	default void renderAt(BlockPos pos, BlockState state, BlockRayTraceResult ray, PlacementOffset offset) {
		//IPlacementHelper.renderArrow(VecHelper.getCenterOf(pos), VecHelper.getCenterOf(offset.getPos()), ray.getFace());

		displayGhost(offset);
	}

	static void renderArrow(Vec3d center, Vec3d target, Direction arrowPlane) {
		renderArrow(center, target, arrowPlane, 1D);
	}
	static void renderArrow(Vec3d center, Vec3d target, Direction arrowPlane, double distanceFromCenter) {
		Vec3d direction = target.subtract(center).normalize();
		Vec3d facing = new Vec3d(arrowPlane.getDirectionVec());
		Vec3d start = center.add(direction);
		Vec3d offset = direction.scale(distanceFromCenter-1);
		Vec3d offsetA = direction.crossProduct(facing).normalize().scale(.25);
		Vec3d offsetB = facing.crossProduct(direction).normalize().scale(.25);
		Vec3d endA = center.add(direction.scale(.75)).add(offsetA);
		Vec3d endB = center.add(direction.scale(.75)).add(offsetB);
		CreateClient.outliner.showLine("placementArrowA" + center + target, start.add(offset), endA.add(offset)).lineWidth(1/16f);
		CreateClient.outliner.showLine("placementArrowB" + center + target, start.add(offset), endB.add(offset)).lineWidth(1/16f);
	}

	default void displayGhost(PlacementOffset offset) {
		if (!offset.hasGhostState())
			return;

		CreateClient.ghostBlocks.showGhostState(this, offset.getTransform().apply(offset.getGhostState()))
				.at(offset.getBlockPos())
				.breathingAlpha();
	}

	static List<Direction> orderedByDistanceOnlyAxis(BlockPos pos, Vec3d hit, Direction.Axis axis) {
		return orderedByDistance(pos, hit, dir -> dir.getAxis() == axis);
	}

	static List<Direction> orderedByDistanceOnlyAxis(BlockPos pos, Vec3d hit, Direction.Axis axis, Predicate<Direction> includeDirection) {
		return orderedByDistance(pos, hit, ((Predicate<Direction>) dir -> dir.getAxis() == axis).and(includeDirection));
	}

	static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3d hit, Direction.Axis axis) {
		return orderedByDistance(pos, hit, dir -> dir.getAxis() != axis);
	}

	static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3d hit, Direction.Axis axis, Predicate<Direction> includeDirection) {
		return orderedByDistance(pos, hit, ((Predicate<Direction>) dir -> dir.getAxis() != axis).and(includeDirection));
	}

	static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3d hit, Direction.Axis first, Direction.Axis second) {
		return orderedByDistanceExceptAxis(pos, hit, first, d -> d.getAxis() != second);
	}

	static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3d hit, Direction.Axis first, Direction.Axis second, Predicate<Direction> includeDirection) {
		return orderedByDistanceExceptAxis(pos, hit, first, ((Predicate<Direction>) d -> d.getAxis() != second).and(includeDirection));
	}

	static List<Direction> orderedByDistance(BlockPos pos, Vec3d hit) {
		return orderedByDistance(pos, hit, _$ -> true);
	}

	static List<Direction> orderedByDistance(BlockPos pos, Vec3d hit, Predicate<Direction> includeDirection) {
		Vec3d centerToHit = hit.subtract(VecHelper.getCenterOf(pos));
		return Arrays.stream(Iterate.directions)
				.filter(includeDirection)
				.map(dir -> Pair.of(dir, new Vec3d(dir.getDirectionVec()).distanceTo(centerToHit)))
				.sorted(Comparator.comparingDouble(Pair::getSecond))
				.map(Pair::getFirst)
				.collect(Collectors.toList());
	}

	default boolean matchesItem(ItemStack item) {
		return getItemPredicate().test(item);
	}

	default boolean matchesState(BlockState state) {
		return getStatePredicate().test(state);
	}
}
