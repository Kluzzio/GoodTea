package timefall.goodtea.blocks;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import timefall.goodtea.blocks.entities.TeaKettleBlockEntity;
import timefall.goodtea.registries.BlockEntitiesRegistry;
import timefall.goodtea.util.FluidStack;

@SuppressWarnings("deprecation")
public class TeaKettleBlock extends BlockWithEntity{
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private VoxelShape shape() {
        return VoxelShapes.union(
                VoxelShapes.cuboid(5,0,5,11,5,11),
                VoxelShapes.cuboid(4,0,6,5,4,10),
                VoxelShapes.cuboid(11,0,6,12,4,10),
                VoxelShapes.cuboid(6,0,11,10,4,12),
                VoxelShapes.cuboid(6,0,4,10,4,5),
                VoxelShapes.cuboid(6,5,6,10,6,10),
                VoxelShapes.cuboid(7,8,5,8,9,6),
                VoxelShapes.cuboid(8,8,10,9,9,11),
                VoxelShapes.cuboid(7,8,9,8,9,11),
                VoxelShapes.cuboid(8,8,5,9,9,7),
                VoxelShapes.cuboid(7,8,6,8,9,9),
                VoxelShapes.cuboid(8,8,7,9,9,10),
                VoxelShapes.cuboid(7,4,2,9,5,3),
                VoxelShapes.cuboid(7,3,3,9,5,4),
                VoxelShapes.cuboid(8,4,4,9,5,5),
                VoxelShapes.cuboid(8,4,11,9,7,12),
                VoxelShapes.cuboid(7,4,4,8,7,5),
                VoxelShapes.cuboid(8,6,10,9,8,11),
                VoxelShapes.cuboid(7,6,5,8,8,6)
        );
    }

    public TeaKettleBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shape();
    }

   @Override
   public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
       return shape();
   }




    /* BLOCK ENTITY STUFF */

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TeaKettleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, BlockEntitiesRegistry.TEA_KETTLE_BLOCK_ENTITY, TeaKettleBlockEntity::tick);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayerFacing());
    }


    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof Inventory) {
                ItemScatterer.spawn(world, pos, (Inventory) blockEntity);
                world.updateComparators(pos, this);
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            NamedScreenHandlerFactory namedScreenHandlerFactory = state.createScreenHandlerFactory(world, pos);

            if (blockEntity instanceof TeaKettleBlockEntity teaKettleBlockEntity) {
                ItemStack itemStack = player.getStackInHand(hand);
                if (itemStack.isOf(Items.WATER_BUCKET) && teaKettleBlockEntity.fluidStorage.amount != FluidStack.convertDropletsToMb(FluidConstants.BUCKET)) {
                    try (Transaction transaction = Transaction.openOuter()) {
                        teaKettleBlockEntity.fluidStorage.insert(
                                FluidVariant.of(Fluids.WATER),
                                FluidStack.convertDropletsToMb(FluidConstants.BUCKET) - teaKettleBlockEntity.fluidStorage.amount,
                                transaction
                        );
                        transaction.commit();
                        player.setStackInHand(hand, Items.BUCKET.getDefaultStack());
                    }
                }

                // TODO: ASK AMPHIBATRON ABOUT HOW THESE MATHS NEED TO BE
                //if ((itemStack.isOf(Items.POTION) && PotionUtil.getPotion(itemStack) == Potions.WATER) && teaKettleBlockEntity.fluidStorage.amount != FluidStack.convertDropletsToMb(FluidConstants.BUCKET)) {
                //    try (Transaction transaction = Transaction.openOuter()) {
                //        teaKettleBlockEntity.fluidStorage.insert(
                //                FluidVariant.of(Fluids.WATER),
                //                (FluidStack.convertDropletsToMb(FluidConstants.BUCKET) / 3) - teaKettleBlockEntity.fluidStorage.amount,
                //                transaction
                //        );
                //        transaction.commit();
                //        player.setStackInHand(hand, Items.GLASS_BOTTLE.getDefaultStack());
                //    }
                //}

                if (!(itemStack.isOf(Items.WATER_BUCKET) || (itemStack.isOf(Items.POTION) && PotionUtil.getPotion(itemStack) == Potions.WATER)) && namedScreenHandlerFactory != null) {
                    player.openHandledScreen(namedScreenHandlerFactory);
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
    }
}
