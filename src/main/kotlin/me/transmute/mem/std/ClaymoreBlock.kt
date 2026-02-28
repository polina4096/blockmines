package me.transmute.mem.std

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.enchantment.EnchantmentHelper
import kotlin.math.exp
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ScheduledTickAccess
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

class ClaymoreBlock(properties: Properties) : Block(properties), SimpleWaterloggedBlock, EntityBlock {
    companion object {
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED
        val FACING: EnumProperty<Direction> = BlockStateProperties.HORIZONTAL_FACING

        private val SHAPE_NORTH: VoxelShape = box(4.0, 0.0, 5.0, 12.0, 8.0, 10.0)
        private val SHAPE_SOUTH: VoxelShape = box(4.0, 0.0, 6.0, 12.0, 8.0, 11.0)
        private val SHAPE_EAST: VoxelShape  = box(5.0, 0.0, 4.0, 10.0, 8.0, 12.0)
        private val SHAPE_WEST: VoxelShape  = box(6.0, 0.0, 4.0, 11.0, 8.0, 12.0)
    }

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(WATERLOGGED, false)
            .setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(WATERLOGGED, FACING)
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return when (state.getValue(FACING)) {
            Direction.SOUTH -> SHAPE_SOUTH
            Direction.EAST -> SHAPE_EAST
            Direction.WEST -> SHAPE_WEST
            else -> SHAPE_NORTH
        }
    }

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
        return canSupportRigidBlock(level, pos.below())
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        if (!canSurvive(defaultBlockState(), context.level, context.clickedPos)) return null
        val fluidState = context.level.getFluidState(context.clickedPos)
        return defaultBlockState()
            .setValue(FACING, context.horizontalDirection)
            .setValue(WATERLOGGED, fluidState.type == Fluids.WATER)
    }

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) Fluids.WATER.getSource(false)
        else super.getFluidState(state)
    }

    override fun updateShape(
        state: BlockState,
        level: LevelReader,
        tickAccess: ScheduledTickAccess,
        pos: BlockPos,
        direction: Direction,
        neighborPos: BlockPos,
        neighborState: BlockState,
        random: RandomSource
    ): BlockState {
        if (state.getValue(WATERLOGGED)) {
            tickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
        }
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState()
        }
        return super.updateShape(state, level, tickAccess, pos, direction, neighborPos, neighborState, random)
    }

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos) as? ClaymoreBlockEntity
            if (blockEntity != null && blockEntity.mineState == ClaymoreBlockEntity.MineState.ARMED && !player.isCreative) {
                val holdingScissors = player.mainHandItem.item == Blockmines.SAFETY_SCISSORS_ITEM
                val shouldExplode = if (holdingScissors) {
                    // success = 1 - 0.5 * e^(-0.45 * level)
                    // Level 0: 50%, Level 1: 68%, Level 2: 80%, Level 3: 87%, Level 4: 92%, Level 5: 95%
                    val enchantRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    val defusalHolder = enchantRegistry.getOrThrow(Blockmines.DEFUSAL_ENCHANTMENT_KEY)
                    val defusalLevel = EnchantmentHelper.getItemEnchantmentLevel(defusalHolder, player.mainHandItem)
                    val successChance = 1.0 - 0.5 * exp(-0.45 * defusalLevel)
                    level.random.nextDouble() >= successChance
                } else true
                if (shouldExplode) {
                    blockEntity.explode(level as ServerLevel, pos)
                    return state
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ClaymoreBlockEntity(pos, state)
    }

    override fun <T : BlockEntity> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? {
        if (level.isClientSide) return null

        @Suppress("UNCHECKED_CAST")
        return if (type == Blockmines.CLAYMORE_MINE_BLOCK_ENTITY_TYPE) {
            BlockEntityTicker<ClaymoreBlockEntity> { lvl, pos, st, be -> be.tick(lvl, pos, st) } as BlockEntityTicker<T>
        } else null
    }
}
