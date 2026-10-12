package com.mchoe5699.manydoors.compat;

import com.mchoe5699.manydoors.control.LinkTarget;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;

/** Dimension-local data; no hard dependency on HomeNet classes or uploaded binaries. */
public final class HomeNetLinks extends SavedData {
    private record Entry(UUID source,List<LinkTarget> doors){}
    private final Map<BlockPos,Entry> links=new HashMap<>();
    public static boolean supported(BlockEntity be){
        if(be==null)return false;
        for(Class<?> c=be.getClass();c!=null;c=c.getSuperclass())
            if(c.getName().equals("com.qwerty.homenet.blockentity.CallerBlockEntity")||c.getName().equals("com.qwerty.homenet.blockentity.ReceiverBlockEntity"))return true;
        return false;
    }
    public static HomeNetLinks get(ServerLevel l){return l.getDataStorage().computeIfAbsent(HomeNetLinks::load,HomeNetLinks::new,"manydoors_homenet_links");}
    private static UUID sourceId(BlockEntity be){
        CompoundTag t=be.getPersistentData();
        if(!t.hasUUID("ManyDoorsIdentity")){t.putUUID("ManyDoorsIdentity",UUID.randomUUID());be.setChanged();}
        return t.getUUID("ManyDoorsIdentity");
    }
    public String toggle(BlockEntity source,LinkTarget target){
        UUID id=sourceId(source);BlockPos pos=source.getBlockPos().immutable();
        Entry entry=links.computeIfAbsent(pos,p->new Entry(id,new ArrayList<>()));
        if(!entry.source.equals(id)){entry=new Entry(id,new ArrayList<>());links.put(pos,entry);}
        String result;
        if(entry.doors.remove(target))result="unlinked";
        else if(entry.doors.size()>=16)return "link_full";
        else {entry.doors.add(target);result="linked";}
        setDirty();return result;
    }
    public void clear(BlockPos pos){if(links.remove(pos)!=null)setDirty();}
    public void pulse(BlockEntity source){
        if(!(source.getLevel() instanceof ServerLevel l))return;
        Entry entry=links.get(source.getBlockPos());
        if(entry==null||!entry.source.equals(sourceId(source)))return;
        for(LinkTarget t:entry.doors){var d=t.resolve(l);if(d!=null)d.activate(true);}
        if(entry.doors.removeIf(t->l.hasChunkAt(t.pos())&&t.resolve(l)==null))setDirty();
    }
    public static void onOpen(BlockEntity source){if(source.getLevel() instanceof ServerLevel l)get(l).pulse(source);}
    public static HomeNetLinks load(CompoundTag tag){
        HomeNetLinks data=new HomeNetLinks();
        for(Tag v:tag.getList("Entries",Tag.TAG_COMPOUND)){
            CompoundTag t=(CompoundTag)v;if(!t.hasUUID("Source"))continue;
            List<LinkTarget> targets=new ArrayList<>();
            for(Tag d:t.getList("Doors",Tag.TAG_COMPOUND))if(((CompoundTag)d).hasUUID("Identity")&&targets.size()<16)targets.add(LinkTarget.load((CompoundTag)d));
            data.links.put(BlockPos.of(t.getLong("Pos")),new Entry(t.getUUID("Source"),targets));
        }return data;
    }
    @Override public CompoundTag save(CompoundTag tag){
        ListTag list=new ListTag();links.forEach((p,e)->{CompoundTag t=new CompoundTag();t.putLong("Pos",p.asLong());t.putUUID("Source",e.source);ListTag doors=new ListTag();e.doors.forEach(d->doors.add(d.save()));t.put("Doors",doors);list.add(t);});tag.put("Entries",list);return tag;
    }
}
