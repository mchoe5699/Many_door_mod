package com.mchoe5699.manydoors.gametest;

import com.mchoe5699.manydoors.ManyDoors;
import com.mchoe5699.manydoors.compat.HomeNetLinks;
import com.mchoe5699.manydoors.control.*;
import com.mchoe5699.manydoors.door.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(ManyDoors.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorGameTests {
    private static final BlockPos CENTER=new BlockPos(12,2,12);
    private static DoorBlockEntity door(GameTestHelper h,BlockPos p,DoorStyle style){
        h.setBlock(p,ManyDoors.DOORS.get(style).get());
        var d=(DoorBlockEntity)h.getBlockEntity(p);h.assertTrue(d.configure(d.settings(),null).equals("ok"),"Door placement failed");return d;
    }
    private static DoorSettings settings(DoorBlockEntity d,DoorSettings.Mode mode,int close){
        var s=d.settings();return new DoorSettings(s.width(),s.height(),s.thickness(),s.angle(),mode,close,s.rightHinge());
    }
    private static boolean open(DoorBlockEntity d){return d.getBlockState().getValue(ConfigurableDoorBlock.OPEN);}
    @GameTest(template="empty")
    public static void manualAutomaticAndHybrid(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.WOODEN);
        d.configure(settings(d,DoorSettings.Mode.MANUAL,0),null);
        h.assertTrue(!d.activate(false),"Manual door accepted sensor");h.assertTrue(d.manualToggle()&&open(d),"Manual click failed");
        d.setOpen(false);d.configure(settings(d,DoorSettings.Mode.AUTOMATIC,20),null);
        h.assertTrue(!d.manualToggle()&&!open(d),"Automatic door accepted click");h.assertTrue(d.activate(false)&&open(d),"Automatic activation failed");
        d.setOpen(false);d.configure(settings(d,DoorSettings.Mode.BOTH,20),null);h.assertTrue(d.manualToggle()&&open(d),"Hybrid click failed");
        d.setOpen(false);h.assertTrue(d.activate(false)&&open(d),"Hybrid sensor failed");h.succeed();
    }
    @GameTest(template="empty")
    public static void settingsPersistAndResize(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.WOODEN);UUID id=d.identity();
        var next=new DoorSettings(3,4,.25,137.5,DoorSettings.Mode.AUTOMATIC,100,true);
        h.assertTrue(d.configure(next,null).equals("ok"),"Resize failed");
        var copy=new DoorBlockEntity(d.getBlockPos(),d.getBlockState());copy.load(d.saveWithoutMetadata());
        h.assertTrue(copy.settings().equals(next)&&copy.identity().equals(id),"Save/load lost settings or link identity");
        var footprint=DoorGeometry.footprint(d.getBlockPos(),d.facing(),d.style(),next);
        for(BlockPos p:footprint)h.assertTrue(DoorBlockEntity.at(h.getLevel(),p)==d,"Missing movement proxy");
        h.assertTrue(d.configure(DoorSettings.defaults(DoorStyle.WOODEN),null).equals("ok"),"Shrink failed");h.succeed();
    }
    @GameTest(template="empty")
    public static void blockedResizeIsAtomic(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.WOODEN);var before=d.settings();
        h.setBlock(CENTER.offset(2,0,0),Blocks.DIAMOND_BLOCK);
        var next=new DoorSettings(3,2,.125,90,DoorSettings.Mode.BOTH,60,false);
        h.assertTrue(d.configure(next,null).equals("no_space"),"Resize overwrote another block");
        h.assertTrue(d.settings().equals(before),"Failed resize changed settings");
        h.assertBlockPresent(Blocks.DIAMOND_BLOCK,CENTER.offset(2,0,0));h.succeed();
    }
    @GameTest(template="empty")
    public static void invalidAndNonFiniteSettingsRejected(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.WOODEN);
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,0,181})h.assertTrue(d.configure(new DoorSettings(1,2,.125,invalid,DoorSettings.Mode.BOTH,60,false),null).equals("invalid_settings"),"Unsafe angle accepted");
        h.assertTrue(!new DoorSettings(7,2,.125,90,DoorSettings.Mode.BOTH,60,false).valid(),"Oversized door accepted");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void buttonOpensMultipleDoorsAndAutoCloses(GameTestHelper h){
        var a=door(h,new BlockPos(5,2,8),DoorStyle.WOODEN);var b=door(h,new BlockPos(10,2,8),DoorStyle.STEEL_ENTRANCE);
        a.configure(settings(a,DoorSettings.Mode.AUTOMATIC,20),null);b.configure(settings(b,DoorSettings.Mode.AUTOMATIC,20),null);
        h.setBlock(new BlockPos(5,2,14),ManyDoors.CONTROLS.get(ControlBlock.Kind.BUTTON).get());
        var button=(ControlBlockEntity)h.getBlockEntity(new BlockPos(5,2,14));
        button.toggle(LinkTarget.of(a));button.toggle(LinkTarget.of(b));button.press();h.assertTrue(open(a)&&open(b),"Button did not open both doors");
        h.runAfterDelay(30,()->{h.assertTrue(!open(a)&&!open(b),"Doors did not auto-close");h.succeed();});
    }
    @GameTest(template="empty")
    public static void staleLinkDoesNotOpenReplacement(GameTestHelper h){
        var old=door(h,CENTER,DoorStyle.WOODEN);LinkTarget link=LinkTarget.of(old);
        h.getLevel().destroyBlock(old.getBlockPos(),false);var replacement=door(h,CENTER,DoorStyle.WOODEN);
        h.assertTrue(link.resolve(h.getLevel())==null&&!open(replacement),"Old link resolved to a replacement");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void motionSensorDetectsEntity(GameTestHelper h){
        var d=door(h,new BlockPos(8,2,8),DoorStyle.WOODEN);d.configure(settings(d,DoorSettings.Mode.AUTOMATIC,40),null);
        BlockPos p=new BlockPos(12,2,12);h.setBlock(p,ManyDoors.CONTROLS.get(ControlBlock.Kind.MOTION_SENSOR).get());
        ((ControlBlockEntity)h.getBlockEntity(p)).toggle(LinkTarget.of(d));h.spawn(EntityType.ARMOR_STAND,p.above());
        h.runAfterDelay(10,()->{h.assertTrue(open(d),"Motion sensor did not detect living entity");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void doorContactOutputsRedstone(GameTestHelper h){
        var d=door(h,new BlockPos(8,2,8),DoorStyle.WOODEN);BlockPos p=new BlockPos(12,2,12);
        h.setBlock(p,ManyDoors.CONTROLS.get(ControlBlock.Kind.CONTACT_SENSOR).get());
        ((ControlBlockEntity)h.getBlockEntity(p)).toggle(LinkTarget.of(d));d.setOpen(true);
        h.runAfterDelay(10,()->{h.assertTrue(h.getBlockState(p).getValue(ControlBlock.POWERED),"Contact sensor did not report open");d.setOpen(false);
            h.runAfterDelay(10,()->{h.assertTrue(!h.getBlockState(p).getValue(ControlBlock.POWERED),"Contact sensor did not report closed");h.succeed();});});
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void nativeOpenPropertyDrivesGeometry(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.WOODEN);
        h.setBlock(CENTER,d.getBlockState().setValue(ConfigurableDoorBlock.OPEN,true));
        h.runAfterDelay(20,()->{h.assertTrue(d.progress(1)>.99,"HomeNet-compatible OPEN did not animate");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void everyStyleHasConsistentCollisionFootprint(GameTestHelper h){
        for(DoorStyle style:DoorStyle.values())for(Direction facing:Direction.Plane.HORIZONTAL){
            var s=new DoorSettings(2,3,.1875,137.5,DoorSettings.Mode.BOTH,60,false);
            var footprint=DoorGeometry.footprint(BlockPos.ZERO,facing,style,s);
            for(int step=0;step<=72;step++)for(var box:DoorGeometry.boxes(style,s,step/72.0,facing)){
                h.assertTrue(Double.isFinite(box.minX)&&box.maxX>=box.minX,"Invalid geometry");
                BlockPos center=BlockPos.containing(box.getCenter());h.assertTrue(footprint.contains(center),"Unreserved moving panel: "+style+" "+center);
            }
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void allStylesCanPlaceAndRemoveParts(GameTestHelper h){
        for(DoorStyle style:DoorStyle.values()){
            var d=door(h,CENTER,style);h.assertTrue(d.getBlockState().getBlock().asItem()==ManyDoors.DOOR_ITEMS.get(style).get(),"Creative pick item missing");Set<BlockPos> parts=DoorGeometry.footprint(d.getBlockPos(),d.facing(),style,d.settings());
            h.getLevel().destroyBlock(d.getBlockPos(),false);
            for(BlockPos p:parts)h.assertTrue(h.getLevel().getBlockState(p).isAir(),"Orphan proxy for "+style);
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void linkerItemFlowAndDimensionRejection(GameTestHelper h){
        var d=door(h,new BlockPos(8,2,8),DoorStyle.WOODEN);
        BlockPos buttonPos=new BlockPos(14,2,14);h.setBlock(buttonPos,ManyDoors.CONTROLS.get(ControlBlock.Kind.BUTTON).get());
        var player=h.makeMockPlayer();d.setOwner(player.getUUID());
        var stack=new net.minecraft.world.item.ItemStack(ManyDoors.LINKER.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        var tool=(com.mchoe5699.manydoors.item.LinkerItem)stack.getItem();
        var doorHit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(d.getBlockPos()),Direction.UP,d.getBlockPos(),false);
        tool.onItemUseFirst(stack,new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,doorHit));
        var buttonHit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(buttonPos)),Direction.UP,h.absolutePos(buttonPos),false);
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,buttonHit);
        tool.onItemUseFirst(stack,context);var button=(ControlBlockEntity)h.getBlockEntity(buttonPos);
        h.assertTrue(button.targets().size()==1,"Linking item did not connect");
        tool.onItemUseFirst(stack,context);h.assertTrue(button.targets().isEmpty(),"Second click did not unlink");
        stack.getOrCreateTag().getCompound("Selection").putString("Dimension","minecraft:the_nether");
        tool.onItemUseFirst(stack,context);h.assertTrue(button.targets().isEmpty(),"Cross-dimension selection linked");h.succeed();
    }
    @GameTest(template="empty")
    public static void ownershipPreventsConfiguration(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.WOODEN);d.setOwner(UUID.randomUUID());
        h.assertTrue(d.configure(d.settings(),h.makeMockSurvivalPlayer()).equals("not_owner"),"Non-owner changed configuration");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void obstructionReopensClosingDoor(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.WOODEN);d.setOpen(true);
        h.runAfterDelay(20,()->{
            h.assertTrue(d.progress(1)>.99,"Door was not initially open: "+d.progress(1));
            var stand=h.spawn(EntityType.ARMOR_STAND,CENTER);stand.setNoGravity(true);
            boolean overlap=DoorGeometry.boxes(d.style(),d.settings(),0,d.facing()).stream().anyMatch(b->b.move(d.getBlockPos()).intersects(stand.getBoundingBox()));
            h.assertTrue(overlap,"Obstruction not positioned in closed leaf: "+stand.getBoundingBox());d.setOpen(false);
            h.runAfterDelay(20,()->{h.assertTrue(open(d),"Closing door ignored obstruction; progress="+d.progress(1)+", entity="+stand.getBoundingBox()+", alive="+stand.isAlive());h.succeed();});
        });
    }
    @GameTest(template="empty")
    public static void breakingPartRemovesEntireDoor(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.WOODEN);Set<BlockPos> parts=DoorGeometry.footprint(d.getBlockPos(),d.facing(),d.style(),d.settings());
        BlockPos part=parts.stream().filter(p->!p.equals(d.getBlockPos())).findFirst().orElseThrow();
        h.getLevel().destroyBlock(part,false);
        for(BlockPos p:parts)h.assertTrue(h.getLevel().getBlockState(p).isAir(),"Breaking a part left an orphan");h.succeed();
    }
    @GameTest(template="empty")
    public static void controlAndHomeNetLinksPersist(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.WOODEN);BlockPos p=new BlockPos(16,2,16);h.setBlock(p,ManyDoors.CONTROLS.get(ControlBlock.Kind.BUTTON).get());
        var control=(ControlBlockEntity)h.getBlockEntity(p);control.toggle(LinkTarget.of(d));
        var copy=new ControlBlockEntity(control.getBlockPos(),control.getBlockState());copy.load(control.saveWithoutMetadata());
        h.assertTrue(copy.targets().size()==1&&copy.targets().get(0).resolve(h.getLevel())==d,"Control links lost on reload");
        if(ModList.get().isLoaded("qwertys_homenet")){
            h.setBlock(p,ForgeRegistries.BLOCKS.getValue(new ResourceLocation("qwertys_homenet","lobby_phone")));var source=h.getBlockEntity(p);
            var data=HomeNetLinks.get(h.getLevel());data.toggle(source,LinkTarget.of(d));
            var restored=HomeNetLinks.load(data.save(new CompoundTag()));restored.pulse(source);h.assertTrue(open(d),"HomeNet saved links did not restore");
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void survivalRecipesAreLoaded(GameTestHelper h){
        for(DoorStyle style:DoorStyle.values())h.assertTrue(h.getLevel().getRecipeManager().byKey(new ResourceLocation("manydoors",style.id)).isPresent(),"Missing door recipe: "+style);
        for(String id:new String[]{"door_linker","door_settings","button","motion_sensor","pressure_sensor","contact_sensor","status_lamp","emergency_button"})h.assertTrue(h.getLevel().getRecipeManager().byKey(new ResourceLocation("manydoors",id)).isPresent(),"Missing device recipe: "+id);
        h.succeed();
    }


    @GameTest(template="empty",timeoutTicks=120)
    public static void revolvingDoorRunsUntilReleased(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.REVOLVING);d.setOpen(true);
        h.runAfterDelay(60,()->{h.assertTrue(d.progress(1)>1,"Rotor stopped after its first angle");d.setOpen(false);float angle=d.progress(1);
            h.runAfterDelay(10,()->{h.assertTrue(Math.abs(d.progress(1)-angle)<.001,"Released rotor did not stop");h.succeed();});});
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void pressureSensorTriggersLinkedDoor(GameTestHelper h){
        var d=door(h,new BlockPos(8,2,8),DoorStyle.WOODEN);d.configure(settings(d,DoorSettings.Mode.AUTOMATIC,40),null);
        BlockPos p=new BlockPos(14,2,14);h.setBlock(p,ManyDoors.CONTROLS.get(ControlBlock.Kind.PRESSURE_SENSOR).get());
        ((ControlBlockEntity)h.getBlockEntity(p)).toggle(LinkTarget.of(d));var entity=h.spawn(EntityType.ARMOR_STAND,p);entity.setNoGravity(true);
        h.runAfterDelay(10,()->{h.assertTrue(open(d),"Pressure sensor did not trigger door");h.succeed();});
    }

    @GameTestGenerator
    public static Collection<TestFunction> homeNetTests(){
        return ModList.get().isLoaded("qwertys_homenet")?List.of(new TestFunction("homenet","manydoors.homenet_commands","manydoors:empty",Rotation.NONE,100,0,true,DoorGameTests::homeNetCallerAndReceiverCommands)):List.of();
    }
    public static void homeNetCallerAndReceiverCommands(GameTestHelper h){
        var d=door(h,new BlockPos(8,2,8),DoorStyle.WOODEN);
        String[] callers={"door_station","door_phone","lobby_phone"};
        try{
            for(String id:callers){
                BlockPos p=new BlockPos(14,2,14);Block block=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("qwertys_homenet",id));h.assertTrue(block!=null,"Missing HomeNet "+id);
                h.setBlock(p,block);BlockEntity source=h.getBlockEntity(p);
                h.assertTrue(HomeNetLinks.supported(source),"Unsupported caller "+id);HomeNetLinks.get(h.getLevel()).toggle(source,LinkTarget.of(d));
                source.getClass().getMethod("openDoors").invoke(source);h.assertTrue(open(d),"Caller command did not reach linked door: "+id);d.setOpen(false);
                // HomeNet's own linker/API must also recognize OPEN on our anchor.
                Object result=source.getClass().getMethod("toggleDoor",BlockPos.class).invoke(source,d.getBlockPos());
                h.assertTrue(result.toString().equals("LINKED"),"Native HomeNet linker rejected door");
            }
            for(String id:new String[]{"wallpad","video_phone","interphone","guard_master","guard_console"}){
                BlockPos p=new BlockPos(16,2,14);Block block=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("qwertys_homenet",id));
                h.setBlock(p,block);BlockEntity source=h.getBlockEntity(p);h.assertTrue(HomeNetLinks.supported(source),"Unsupported receiver "+id);
                HomeNetLinks.get(h.getLevel()).toggle(source,LinkTarget.of(d));
                source.getClass().getMethod("openDoor").invoke(source);h.assertTrue(!open(d),"Idle receiver bypassed HomeNet guard: "+id);
                source.getClass().getMethod("startRinging",BlockPos.class,String.class).invoke(source,h.absolutePos(new BlockPos(14,2,14)),"test");
                source.getClass().getMethod("openDoor").invoke(source);h.assertTrue(open(d),"Receiver door command did not reach link: "+id);d.setOpen(false);
            }
        }catch(ReflectiveOperationException e){throw new RuntimeException("HomeNet integration failed",e);}
        h.succeed();
    }
}
