package com.nuxvisuals.module.impl;

import com.nuxvisuals.render.Fx;
import com.nuxvisuals.render.RenderUtil;
import net.minecraft.block.*;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import java.util.*;

/** Stable world-coordinate cache, priority fills, gradual refresh. Never mutates world blocks. */
public final class WinterCover {
    private static final Identifier SNOW=Identifier.of("nuxvisuals","textures/environment/winter_snow.png");
    private static final Identifier ICE=Identifier.of("nuxvisuals","textures/environment/winter_ice.png");
    private record Surface(double x1,double z1,double x2,double z2,double y,boolean ice) {}
    private record Column(List<Surface> faces,long scanned,int baseY) {}
    private record Cell(int x,int z) {}
    private final Map<Long,Column> cache=new HashMap<>();
    private List<Cell> order=List.of();
    private ClientWorld world;
    private int radius, anchorX=Integer.MIN_VALUE,anchorY,anchorZ,cursor,refresh;
    private long ticks;
    private static long key(int x,int z) { return ((long)x<<32)^(z&0xFFFFFFFFL); }
    public void clear() { cache.clear();order=List.of();world=null;anchorX=Integer.MIN_VALUE;cursor=refresh=0;ticks=0; }
    public void tick(ClientWorld w,Vec3d camera,int requestedRadius) {
        int cx=(int)Math.floor(camera.x),cy=(int)Math.floor(camera.y),cz=(int)Math.floor(camera.z);
        if(world!=w || radius!=requestedRadius) { clear();world=w;radius=requestedRadius; }
        ticks++;
        if(anchorX==Integer.MIN_VALUE || Math.abs(cx-anchorX)>=4 || Math.abs(cz-anchorZ)>=4 || Math.abs(cy-anchorY)>=6) {
            if(anchorX!=Integer.MIN_VALUE && (Math.abs(cx-anchorX)>radius || Math.abs(cz-anchorZ)>radius)) cache.clear();
            anchorX=cx;anchorY=cy;anchorZ=cz;
            List<Cell> next=new ArrayList<>();int outer=radius+8;
            for(int dx=-outer;dx<=outer;dx++) for(int dz=-outer;dz<=outer;dz++)
                if(dx*dx+dz*dz<=outer*outer) next.add(new Cell(cx+dx,cz+dz));
            next.sort(Comparator.comparingInt(c->(c.x-cx)*(c.x-cx)+(c.z-cz)*(c.z-cz)));
            order=next;cursor=refresh=0;
            cache.entrySet().removeIf(e->{int x=(int)(e.getKey()>>32),z=(int)(long)e.getKey();
                return (x-cx)*(x-cx)+(z-cz)*(z-cz)>(outer+4)*(outer+4);});
        }
        long deadline=System.nanoTime()+3_000_000L;
        int scanned=0,visited=0;
        // New/unloaded columns get priority. Center remains stable while walking: no moving cursor holes.
        while(cursor<order.size() && scanned<160 && System.nanoTime()<deadline) {
            Cell cell=order.get(cursor++);Column old=cache.get(key(cell.x,cell.z));
            if(old!=null && Math.abs(old.baseY-cy)<6) continue;
            if(scan(w,cell,cy)) scanned++;
        }
        // Refresh real blocks/chunks without clearing already visible snow.
        while(!order.isEmpty() && scanned<192 && visited<96 && System.nanoTime()<deadline) {
            Cell cell=order.get(refresh);refresh=(refresh+1)%order.size();visited++;
            Column old=cache.get(key(cell.x,cell.z));
            if(old==null || ticks-old.scanned>30 || Math.abs(old.baseY-cy)>=6) {
                if(scan(w,cell,cy)) scanned++;
            }
        }
    }
    private boolean scan(ClientWorld w,Cell c,int cameraY) {
        if(!w.isChunkLoaded(c.x>>4,c.z>>4)) { cache.remove(key(c.x,c.z));return false; }
        List<Surface> faces=new ArrayList<>();
        Set<Integer> levels=new HashSet<>();
        int top=w.getTopY(Heightmap.Type.MOTION_BLOCKING,c.x,c.z)-1;
        int exposed=w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,c.x,c.z)-1;
        // Landscape/rooftops even when camera is far above/below them, plus nearby indoor surfaces.
        for(int y=top-10;y<=top;y++) levels.add(y);
        for(int y=cameraY-14;y<=cameraY+10;y++) levels.add(y);
        for(int y:levels) {
            if(y<w.getBottomY() || y>w.getTopYInclusive()) continue;
            BlockPos pos=new BlockPos(c.x,y,c.z);BlockState state=w.getBlockState(pos);
            if(WorldCustomizer.icyWater()&&WinterSurfaceRules.ice(y,exposed,state.getFluidState().isIn(FluidTags.WATER),state.getFluidState().isStill(),!w.getBlockState(pos.up()).getFluidState().isEmpty())){
                faces.add(new Surface(c.x,c.z,c.x+1,c.z+1,y+state.getFluidState().getHeight(w,pos)+.008,true));
                continue;
            }
            if(y<exposed&&!state.isIn(BlockTags.LEAVES))continue;
            if(state.isAir() || !state.getFluidState().isEmpty() || state.isOf(Blocks.SNOW)
                    || state.isOf(Blocks.SNOW_BLOCK) || state.isOf(Blocks.BARRIER)) continue;
            BlockState above=w.getBlockState(pos.up());
            if(!WinterSurfaceRules.snow(y,exposed,state.isIn(BlockTags.LEAVES),above.isOpaqueFullCube(),!above.getFluidState().isEmpty()))continue;
            VoxelShape shape=state.getCollisionShape(w,pos);
            if(shape.isEmpty()) continue;
            List<Box> boxes=shape.getBoundingBoxes();
            for(Box box:boxes) {
                if(box.maxX-box.minX<0.12 || box.maxZ-box.minZ<0.12) continue;
                boolean hidden=false;
                for(Box other:boxes) if(other!=box && other.minY>=box.maxY-0.0001 && other.maxY>box.maxY
                        && other.minX<=box.minX && other.maxX>=box.maxX && other.minZ<=box.minZ && other.maxZ>=box.maxZ) {hidden=true;break;}
                if(!hidden) faces.add(new Surface(c.x+box.minX,c.z+box.minZ,c.x+box.maxX,c.z+box.maxZ,y+box.maxY+0.006,false));
            }
        }
        cache.put(key(c.x,c.z),new Column(faces,ticks,cameraY));return true;
    }
    private static void vertex(VertexConsumer vc,Matrix4f m,float x,float y,float z,int color,float u,float v){
        vc.vertex(m,x,y,z).color(color).texture(u,v).light(RenderUtil.FULL_LIGHT);
    }
    public void render(MatrixStack matrices,VertexConsumerProvider providers,Camera camera,ClientWorld w) {
        if(world!=w || cache.isEmpty()) return;
        Vec3d cam=camera.getPos();Matrix4f m=matrices.peek().getPositionMatrix();
        int rgb=RenderUtil.shade(0xFFF2F6FA,1-w.getAmbientDarkness()/15f*0.7f);
        // Immediate may finish its shared BufferBuilder whenever the layer changes.
        // Finish every snow write before requesting ice; never retain two shared consumers.
        for(int pass=0;pass<2;pass++) {
            boolean icePass=pass==1;
            if(icePass&&!WorldCustomizer.icyWater())continue;
            VertexConsumer vc=null;
            for(Column column:cache.values()) for(Surface s:column.faces) {
                if(s.ice!=icePass)continue;
                double dx=(s.x1+s.x2)*0.5-cam.x,dz=(s.z1+s.z2)*0.5-cam.z;
                double distance=Math.sqrt(dx*dx+dz*dz);
                if(distance>radius) continue;
                int base=icePass?RenderUtil.shade(0xFFD3E5FC,1-w.getAmbientDarkness()/15f*.55f):rgb;
                int color=RenderUtil.withAlpha(base,WinterSurfaceRules.edgeAlpha(distance,radius));
                float x1=(float)(s.x1-cam.x),x2=(float)(s.x2-cam.x),z1=(float)(s.z1-cam.z),z2=(float)(s.z2-cam.z),y=(float)(s.y-cam.y);
                float scale=icePass?.25f:1f;
                float u1=WinterSurfaceRules.uv(s.x1,scale);
                float v1=WinterSurfaceRules.uv(s.z1,scale);
                float u2=u1+(float)(s.x2-s.x1)*scale,v2=v1+(float)(s.z2-s.z1)*scale;
                // Lazy acquisition also avoids switching/finishing a buffer for an empty pass.
                if(vc==null)vc=providers.getBuffer(RenderLayer.getText(icePass?ICE:SNOW));
                vertex(vc,m,x1,y,z1,color,u1,v1);vertex(vc,m,x1,y,z2,color,u1,v2);
                vertex(vc,m,x2,y,z2,color,u2,v2);vertex(vc,m,x2,y,z1,color,u2,v1);
            }
        }
    }
}
