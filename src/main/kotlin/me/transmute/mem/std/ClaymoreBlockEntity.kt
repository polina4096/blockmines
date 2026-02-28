package me.transmute.mem.std

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3

class ClaymoreBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(Blockmines.CLAYMORE_MINE_BLOCK_ENTITY_TYPE, pos, state) {

    enum class MineState {
        INACTIVE, // Just placed, not yet armed
        ARMING,   // Player left range, counting down 3 seconds
        ARMED     // Ready to explode on proximity
    }

    var mineState: MineState = MineState.INACTIVE
        private set
    private var armingTicks: Int = 0

    companion object {
        const val ACTIVATION_RANGE = 2.5
        const val ACTIVATION_RANGE_SQ = ACTIVATION_RANGE * ACTIVATION_RANGE
        const val ARMING_TIME_TICKS = 60 // 3 seconds at 20 tps
    }

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        val serverLevel = level as? ServerLevel ?: return

        val center = Vec3.atCenterOf(pos)
        val anyPlayerInRange = level.players().any { player ->
            player.isAlive && !player.isSpectator && player.distanceToSqr(center) <= ACTIVATION_RANGE_SQ
        }

        when (mineState) {
            MineState.INACTIVE -> {
                if (!anyPlayerInRange) {
                    mineState = MineState.ARMING
                    armingTicks = ARMING_TIME_TICKS
                    setChanged()
                }
            }

            MineState.ARMING -> {
                if (anyPlayerInRange) {
                    // Player came back within range, reset
                    mineState = MineState.INACTIVE
                    armingTicks = 0
                    setChanged()
                } else {
                    armingTicks--
                    if (armingTicks <= 0) {
                        mineState = MineState.ARMED
                        // Beep to indicate the mine is now armed
                        level.playSound(
                            null,
                            pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                            SoundEvents.NOTE_BLOCK_BIT,
                            SoundSource.BLOCKS,
                            1.0f, 2.0f
                        )
                        // Blink - spawn electric spark particles
                        serverLevel.sendParticles(
                            ParticleTypes.ELECTRIC_SPARK,
                            pos.x + 0.5, pos.y + 0.4, pos.z + 0.5,
                            12, 0.15, 0.1, 0.15, 0.02
                        )
                        setChanged()
                    }
                }
            }

            MineState.ARMED -> {
                if (anyPlayerInRange) {
                    explode(serverLevel, pos)
                }
            }
        }
    }

    fun explode(level: ServerLevel, pos: BlockPos) {
        level.removeBlock(pos, false)
        level.explode(
            null,
            pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
            10.0f,
            Level.ExplosionInteraction.TNT
        )
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        output.putInt("MineState", mineState.ordinal)
        output.putInt("ArmingTicks", armingTicks)
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        mineState = MineState.entries.getOrElse(input.getIntOr("MineState", 0)) { MineState.INACTIVE }
        armingTicks = input.getIntOr("ArmingTicks", 0)
    }
}
