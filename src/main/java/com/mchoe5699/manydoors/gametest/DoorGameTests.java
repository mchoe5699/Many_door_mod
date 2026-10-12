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
    private static void control(GameTestHelper h,BlockPos p,ControlBlock.Kind kind){
        h.setBlock(kind==ControlBlock.Kind.PRESSURE_SENSOR?p.below():p.south(),Blocks.STONE);
        h.setBlock(p,ManyDoors.CONTROLS.get(kind).get());
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
        for(BlockPos p:footprint)if(!p.equals(d.getBlockPos()))h.assertTrue(h.getLevel().getBlockState(p).isAir(),"Resize reserved surrounding blocks");
        h.assertTrue(d.configure(DoorSettings.defaults(DoorStyle.WOODEN),null).equals("ok"),"Shrink failed");h.succeed();
    }
    @GameTest(template="empty")
    public static void blockedLobbyColumnResizeIsAtomic(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.LOBBY_AUTOMATIC);var before=d.settings();
        h.setBlock(CENTER.above(3),Blocks.DIAMOND_BLOCK);
        var next=new DoorSettings(3,4,.125,90,DoorSettings.Mode.BOTH,60,false);
        h.assertTrue(d.configure(next,null).equals("no_space"),"Resize overwrote another block");
        h.assertTrue(d.settings().equals(before),"Failed resize changed settings");
        h.assertBlockPresent(Blocks.DIAMOND_BLOCK,CENTER.above(3));h.succeed();
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
        control(h,new BlockPos(5,2,14),ControlBlock.Kind.BUTTON);
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
        BlockPos p=new BlockPos(12,2,12);control(h,p,ControlBlock.Kind.MOTION_SENSOR);
        ((ControlBlockEntity)h.getBlockEntity(p)).toggle(LinkTarget.of(d));h.spawn(EntityType.ARMOR_STAND,p.above());
        h.runAfterDelay(10,()->{h.assertTrue(open(d),"Motion sensor did not detect living entity");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void doorContactOutputsRedstone(GameTestHelper h){
        var d=door(h,new BlockPos(8,2,8),DoorStyle.WOODEN);BlockPos p=new BlockPos(12,2,12);
        control(h,p,ControlBlock.Kind.CONTACT_SENSOR);
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
        BlockPos buttonPos=new BlockPos(14,2,14);control(h,buttonPos,ControlBlock.Kind.BUTTON);
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
    public static void breakingLobbyColumnRemovesEntireDoor(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.LOBBY_AUTOMATIC);Set<BlockPos> parts=DoorBlockEntity.mountingCells(d.getBlockPos(),d.style(),d.settings());
        BlockPos part=parts.stream().filter(p->!p.equals(d.getBlockPos())).findFirst().orElseThrow();
        h.getLevel().destroyBlock(part,false);
        for(BlockPos p:parts)h.assertTrue(h.getLevel().getBlockState(p).isAir(),"Breaking a part left an orphan");h.succeed();
    }
    @GameTest(template="empty")
    public static void controlAndHomeNetLinksPersist(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.WOODEN);BlockPos p=new BlockPos(16,2,16);control(h,p,ControlBlock.Kind.BUTTON);
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
        BlockPos p=new BlockPos(14,2,14);control(h,p,ControlBlock.Kind.PRESSURE_SENSOR);
        ((ControlBlockEntity)h.getBlockEntity(p)).toggle(LinkTarget.of(d));var entity=h.spawn(EntityType.ARMOR_STAND,p);entity.setNoGravity(true);
        h.runAfterDelay(10,()->{h.assertTrue(open(d),"Pressure sensor did not trigger door");h.succeed();});
    }


    @GameTest(template="empty",timeoutTicks=80)
    public static void movingLeavesPassThroughPlacedWalls(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.SLIDING);
        h.setBlock(CENTER.east(),Blocks.DIAMOND_BLOCK);h.setBlock(CENTER.east(2),Blocks.DIAMOND_BLOCK);
        h.assertTrue(d.configure(new DoorSettings(3,3,.2,120,DoorSettings.Mode.BOTH,0,false),null).equals("ok"),"Wall blocked size setting");d.setOpen(true);
        h.runAfterDelay(20,()->{h.assertTrue(d.progress(1)>.99,"Wall stopped door movement");h.assertBlockPresent(Blocks.DIAMOND_BLOCK,CENTER.east());h.assertBlockPresent(Blocks.DIAMOND_BLOCK,CENTER.east(2));h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void oldSweepBlocksAreMigratedWithoutLosingLink(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.WOODEN);UUID id=d.identity();BlockPos partPos=CENTER.east();
        h.setBlock(partPos,ManyDoors.DOOR_PART.get());((DoorPartEntity)h.getBlockEntity(partPos)).bind(d.getBlockPos(),id);
        CompoundTag old=d.saveWithoutMetadata();old.remove("Schema");old.putLongArray("Occupied",new long[]{d.getBlockPos().asLong(),h.absolutePos(partPos).asLong()});d.load(old);
        h.runAfterDelay(25,()->{h.assertTrue(h.getBlockState(partPos).isAir(),"Old placeholder survived migration");h.assertTrue(DoorBlockEntity.at(h.getLevel(),d.getBlockPos())==d&&d.identity().equals(id),"Migration destroyed door identity");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void collisionHelpersDoNotBlockBuildingAndAreNotSaved(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.WOODEN);
        h.runAfterDelay(3,()->{
            var entities=h.getLevel().getEntitiesOfClass(DoorColliderEntity.class,new net.minecraft.world.phys.AABB(d.getBlockPos()).inflate(5));
            h.assertTrue(!entities.isEmpty(),"No collision/picking helpers spawned");
            for(var e:entities){h.assertTrue(!e.blocksBuilding&&!e.shouldBeSaved(),"Helper reserves placement or persists independently");h.assertTrue(e.getPickResult().is(ManyDoors.DOOR_ITEMS.get(d.style()).get()),"Visual door has wrong pick item");}
            var collisions=h.getLevel().getEntityCollisions(h.makeMockPlayer(),new net.minecraft.world.phys.AABB(d.getBlockPos()).deflate(.1));h.assertTrue(!collisions.isEmpty(),"Closed door does not collide with player");
            h.getLevel().destroyBlock(d.getBlockPos(),false);h.runAfterDelay(2,()->{h.assertTrue(h.getLevel().getEntitiesOfClass(DoorColliderEntity.class,new net.minecraft.world.phys.AABB(d.getBlockPos()).inflate(5)).isEmpty(),"Destroyed door left collision entities");h.succeed();});
        });
    }
    @GameTest(template="empty")
    public static void customDesignPersistsAndRejectsBadData(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.WOODEN);int[] pixels=new int[DoorDesign.PIXELS];Arrays.fill(pixels,0xFF1255EE);pixels[12]=0;
        h.assertTrue(d.applyDesign(pixels,null).equals("ok"),"Design rejected");pixels[0]=0;h.assertTrue(d.designPixels()[0]==0xFF1255EE,"Caller mutated saved pixels");
        var copy=new DoorBlockEntity(d.getBlockPos(),d.getBlockState());copy.load(d.saveWithoutMetadata());h.assertTrue(Arrays.equals(copy.designPixels(),d.designPixels()),"Design did not persist");
        h.assertTrue(d.applyDesign(new int[DoorDesign.PIXELS+1],null).equals("invalid_design"),"Oversized design accepted");
        CompoundTag corrupt=d.saveWithoutMetadata();corrupt.putFloat("Progress",Float.NaN);copy.load(corrupt);h.assertTrue(Float.isFinite(copy.progress(1)),"Corrupt motion data propagated NaN");
        h.assertTrue(d.applyDesign(new int[0],null).equals("ok")&&!d.hasDesign(),"Original design reset failed");h.succeed();
    }
    @GameTest(template="empty")
    public static void designPacketFitsAndDecodesWithinPayloadLimit(GameTestHelper h){
        int[] pixels=new int[DoorDesign.PIXELS];Arrays.fill(pixels,0xFFFFFFFF);
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{var packet=new com.mchoe5699.manydoors.network.DoorNetwork.DesignApply(BlockPos.ZERO,UUID.randomUUID(),pixels);com.mchoe5699.manydoors.network.DoorNetwork.DesignApply.encode(packet,buffer);
            h.assertTrue(buffer.readableBytes()<32767,"Design exceeds client/server payload size");var decoded=com.mchoe5699.manydoors.network.DoorNetwork.DesignApply.decode(buffer);h.assertTrue(Arrays.equals(pixels,decoded.pixels()),"Design packet round trip corrupted pixels");
        }finally{buffer.release();}h.succeed();
    }
    @GameTest(template="empty")
    public static void wallDeviceMountingAndOffsetsPersist(GameTestHelper h){
        BlockPos p=new BlockPos(14,2,14);control(h,p,ControlBlock.Kind.BUTTON);var be=(ControlBlockEntity)h.getBlockEntity(p);var player=h.makeMockPlayer();
        var mount=new MountSettings(.21,-.17,.08,1.2,7);h.assertTrue(be.configure(mount,player).equals("ok"),"Offset configuration failed");
        var copy=new ControlBlockEntity(be.getBlockPos(),be.getBlockState());copy.load(be.saveWithoutMetadata());h.assertTrue(copy.mount().equals(mount),"Mounting position did not persist");
        h.assertTrue(((ControlBlock)be.getBlockState().getBlock()).canSurvive(be.getBlockState(),h.getLevel(),be.getBlockPos()),"Wall device has no supporting wall");
        for(Direction facing:Direction.Plane.HORIZONTAL){var bounds=mount.bounds(facing,ControlBlock.Kind.BUTTON);h.assertTrue(Double.isFinite(bounds.minX)&&bounds.getYsize()>.8,"Invalid wall selection shape");}
        h.assertTrue(be.configure(new MountSettings(Double.NaN,0,0,1,3),player).equals("invalid_settings"),"NaN offset accepted");h.succeed();
    }
    @GameTest(template="empty")
    public static void lobbyAutomaticDoorHasRealPhoneMount(GameTestHelper h){
        var d=door(h,CENTER,DoorStyle.LOBBY_AUTOMATIC);BlockPos mounting=CENTER.above();
        h.assertTrue(h.getBlockState(mounting).is(ManyDoors.LOBBY_MOUNT.get()),"Phone mounting post missing");
        h.assertTrue(h.getBlockState(mounting).isFaceSturdy(h.getLevel(),h.absolutePos(mounting),Direction.NORTH),"Phone mount is not a support surface");
        h.setBlock(CENTER.east(3),Blocks.STONE);h.assertTrue(d.activate(false),"Lobby door is not automatic");
        if(ModList.get().isLoaded("qwertys_homenet")){
            BlockPos phonePos=mounting.north();var block=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("qwertys_homenet","lobby_phone"));h.setBlock(phonePos,block);var phone=h.getBlockEntity(phonePos);h.assertTrue(HomeNetLinks.supported(phone),"Lobby phone did not mount");HomeNetLinks.get(h.getLevel()).toggle(phone,LinkTarget.of(d));d.setOpen(false);
            try{phone.getClass().getMethod("openDoors").invoke(phone);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}h.assertTrue(open(d),"Mounted lobby phone did not open door");
        }h.succeed();
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
                var nativeTool=ForgeRegistries.ITEMS.getValue(new ResourceLocation("qwertys_homenet","door_linker"));h.assertTrue(nativeTool!=null&&nativeTool!=net.minecraft.world.item.Items.AIR,"Native linker item missing");
                var player=h.makeMockPlayer();var stack=new net.minecraft.world.item.ItemStack(nativeTool);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
                var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(source.getBlockPos()),Direction.NORTH,source.getBlockPos(),false);
                nativeTool.onItemUseFirst(stack,new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
                var visual=ManyDoors.COLLIDER.get().create(h.getLevel());visual.bind(d,0);visual.updateBox(d);visual.interact(player,net.minecraft.world.InteractionHand.MAIN_HAND);
                var linked=(java.util.List<?>)source.getClass().getMethod("getLinkedDoors").invoke(source);h.assertTrue(linked.isEmpty()&&!open(d),"Visual leaf failed native linker interaction");
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
