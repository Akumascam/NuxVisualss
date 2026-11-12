package com.nuxvisuals.render;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nuxvisuals.module.impl.ShaderSky;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
/** Drawn during the vanilla sky stage, at far depth; never replaces world geometry. */
public final class ShaderSkyRenderer {
 private static final ShaderProgramKey KEY=new ShaderProgramKey(Identifier.of("nuxvisuals","core/shader_sky"),VertexFormats.POSITION,Defines.EMPTY);
 private static long retryAt;
 private static boolean applied;
 public static boolean applied(){return applied&&ShaderSky.active();}
 private ShaderSkyRenderer(){}
 public static void retry(){retryAt=0;}
 public static boolean render(){
  applied=false;
  if(!ShaderSky.active()||System.nanoTime()<retryAt)return false;
  var m=ShaderSky.INSTANCE;ShaderProgram previous=RenderSystem.getShader();
  try{
   ShaderProgram shader=RenderSystem.setShader(KEY);
   if(shader==null){retryAt=System.nanoTime()+5_000_000_000L;return false;}
   shader.getUniformOrDefault("InvProjection").set(new Matrix4f(RenderSystem.getProjectionMatrix()).invert());
   shader.getUniformOrDefault("ViewRotation").set(new Matrix4f().rotation(MinecraftClient.getInstance().gameRenderer.getCamera().getRotation()));
   shader.getUniformOrDefault("NuxTime").set(m.seconds());
   shader.getUniformOrDefault("Effect").set(m.effect());
   shader.getUniformOrDefault("Octaves").set(m.octaves());
   shader.getUniformOrDefault("Brightness").set(m.brightness());
   shader.getUniformOrDefault("Stars").set(m.stars());
   shader.getUniformOrDefault("HorizonBlend").set(m.horizon());
   int tint=m.tint();shader.getUniformOrDefault("Tint").set(((tint>>16)&255)/255f,((tint>>8)&255)/255f,(tint&255)/255f);
   RenderSystem.enableDepthTest();RenderSystem.depthFunc(515);RenderSystem.depthMask(false);
   RenderSystem.disableCull();RenderSystem.disableBlend();
   BufferBuilder b=Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION);
   b.vertex(-1,-1,0);b.vertex(1,-1,0);b.vertex(1,1,0);b.vertex(-1,1,0);
   BufferRenderer.drawWithGlobalProgram(b.end());applied=true;return true;
  }catch(Exception e){
   retryAt=System.nanoTime()+5_000_000_000L;com.nuxvisuals.NuxVisuals.LOGGER.warn("ShaderSky could not render; using vanilla sky",e);return false;
  }finally{
   RenderSystem.depthMask(true);RenderSystem.depthFunc(515);RenderSystem.enableCull();RenderSystem.disableBlend();
   RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShader(previous);
  }
 }
}
