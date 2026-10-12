package com.mchoe5699.manydoors;

import com.mchoe5699.manydoors.door.*;
import com.mchoe5699.manydoors.control.*;
import com.mchoe5699.manydoors.item.*;
import com.mchoe5699.manydoors.network.DoorNetwork;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

@Mod(ManyDoors.MOD_ID)
public final class ManyDoors {
    public static final String MOD_ID="manydoors";
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,MOD_ID);
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,MOD_ID);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ACTORS=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,MOD_ID);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<DoorColliderEntity>> COLLIDER=ACTORS.register("door_collider",()->net.minecraft.world.entity.EntityType.Builder.<DoorColliderEntity>of(DoorColliderEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(.01f,.01f).clientTrackingRange(12).updateInterval(20).build("manydoors:door_collider"));
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,MOD_ID);
    public static final Map<DoorStyle,RegistryObject<ConfigurableDoorBlock>> DOORS=new EnumMap<>(DoorStyle.class);
    public static final Map<DoorStyle,RegistryObject<Item>> DOOR_ITEMS=new EnumMap<>(DoorStyle.class);
    public static final Map<ControlBlock.Kind,RegistryObject<ControlBlock>> CONTROLS=new EnumMap<>(ControlBlock.Kind.class);
    static {
        for(DoorStyle style:DoorStyle.values()){
            DOORS.put(style,BLOCKS.register(style.id,()->new ConfigurableDoorBlock(style,doorProperties())));
            DOOR_ITEMS.put(style,ITEMS.register(style.id,()->new DoorPlacementItem(style,new Item.Properties())));
        }
        for(ControlBlock.Kind kind:ControlBlock.Kind.values()){
            String id=kind.name().toLowerCase(Locale.ROOT);
            RegistryObject<ControlBlock> block=BLOCKS.register(id,()->new ControlBlock(kind,BlockBehaviour.Properties.of().strength(1).noOcclusion().lightLevel(s->kind==ControlBlock.Kind.STATUS_LAMP&&s.getValue(ControlBlock.POWERED)?12:0)));
            CONTROLS.put(kind,block);ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties()));
        }
    }
    private static BlockBehaviour.Properties doorProperties(){return BlockBehaviour.Properties.of().strength(3).noOcclusion().isSuffocating((s,l,p)->false).isViewBlocking((s,l,p)->false).dynamicShape();}
    public static final RegistryObject<DoorPartBlock> DOOR_PART=BLOCKS.register("door_part",()->new DoorPartBlock(doorProperties().noLootTable()));
    public static final RegistryObject<LobbyMountBlock> LOBBY_MOUNT=BLOCKS.register("lobby_mount",()->new LobbyMountBlock(doorProperties().noLootTable()));
    public static final RegistryObject<Item> LINKER=ITEMS.register("door_linker",()->new LinkerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SETTINGS=ITEMS.register("door_settings",()->new SettingsItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockEntityType<DoorBlockEntity>> DOOR_ENTITY=ENTITIES.register("door",()->BlockEntityType.Builder.of(DoorBlockEntity::new,DOORS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    public static final RegistryObject<BlockEntityType<DoorPartEntity>> PART_ENTITY=ENTITIES.register("door_part",()->BlockEntityType.Builder.of(DoorPartEntity::new,DOOR_PART.get(),LOBBY_MOUNT.get()).build(null));
    public static final RegistryObject<BlockEntityType<ControlBlockEntity>> CONTROL_ENTITY=ENTITIES.register("control",()->BlockEntityType.Builder.of(ControlBlockEntity::new,CONTROLS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    public static final RegistryObject<CreativeModeTab> TAB=TABS.register("doors",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.manydoors")).icon(()->new ItemStack(DOOR_ITEMS.get(DoorStyle.APARTMENT_ENTRANCE).get())).displayItems((p,out)->{DOOR_ITEMS.values().forEach(i->out.accept(i.get()));CONTROLS.values().forEach(b->out.accept(b.get()));out.accept(LINKER.get());out.accept(SETTINGS.get());}).build());
    public ManyDoors(){IEventBus bus=FMLJavaModLoadingContext.get().getModEventBus();BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);ACTORS.register(bus);TABS.register(bus);DoorNetwork.register();}
}
