package me.transmute.mem.std

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.PushReaction
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import org.slf4j.LoggerFactory

object Blockmines : ModInitializer {
    private val logger = LoggerFactory.getLogger("blockmines")
    
    val CLAYMORE_MINE_ID = Identifier.fromNamespaceAndPath("blockmines", "claymore_mine")

    val CLAYMORE_MINE_BLOCK: ClaymoreBlock = Registry.register(
        BuiltInRegistries.BLOCK,
        CLAYMORE_MINE_ID,
        ClaymoreBlock(BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, CLAYMORE_MINE_ID))
            .sound(SoundType.METAL)
            .strength(0.5f, 0.5f)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY))
    )

    val CLAYMORE_MINE_ITEM: Item = Registry.register(
        BuiltInRegistries.ITEM,
        CLAYMORE_MINE_ID,
        BlockItem(CLAYMORE_MINE_BLOCK, Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, CLAYMORE_MINE_ID)))
    )

    private val SAFETY_SCISSORS_ID = Identifier.fromNamespaceAndPath("blockmines", "safety_scissors")

    val SAFETY_SCISSORS_ITEM: Item = Registry.register(
        BuiltInRegistries.ITEM,
        SAFETY_SCISSORS_ID,
        Item(Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, SAFETY_SCISSORS_ID))
            .stacksTo(1)
            .enchantable(10))
    )

    val DEFUSAL_ENCHANTMENT_KEY: ResourceKey<Enchantment> = ResourceKey.create(
        Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("blockmines", "defusal")
    )

    val CLAYMORE_MINE_BLOCK_ENTITY_TYPE: BlockEntityType<ClaymoreBlockEntity> = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        CLAYMORE_MINE_ID,
        FabricBlockEntityTypeBuilder.create(::ClaymoreBlockEntity, CLAYMORE_MINE_BLOCK).build()
    )

    private val CREATIVE_TAB_ID = Identifier.fromNamespaceAndPath("blockmines", "blockmines")

    val CREATIVE_TAB: CreativeModeTab = Registry.register(
        BuiltInRegistries.CREATIVE_MODE_TAB,
        CREATIVE_TAB_ID,
        FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.blockmines"))
            .icon { ItemStack(SAFETY_SCISSORS_ITEM) }
            .displayItems { _, entries ->
                entries.accept(CLAYMORE_MINE_ITEM)
                entries.accept(SAFETY_SCISSORS_ITEM)
            }
            .build()
    )

    override fun onInitialize() {
        logger.info("Blockmines loaded!")
    }
}
