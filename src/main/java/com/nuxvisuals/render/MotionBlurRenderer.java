package com.nuxvisuals.render;

import com.nuxvisuals.NuxVisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33C.*;

/**
 * One RGB history texture and one fullscreen triangle. Never reads pixels back to the CPU.
 * Runs on the render thread only. Raw GL state is restored to keep RenderSystem's cache valid.
 */
public final class MotionBlurRenderer {
    private static int texture, program, vao, opacityUniform;
    private static int width, height;
    private static boolean ready, failed;
    private static long lastFrame;
    private static Object world;
    private static Vec3d lastCamera;
    private static final String VERTEX = """
            #version 150
            out vec2 uv;
            void main() {
                vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                uv = p;
                gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
            }
            """;
    private static final String FRAGMENT = """
            #version 150
            uniform sampler2D History;
            uniform float Retention;
            in vec2 uv;
            out vec4 fragColor;
            void main() { fragColor = vec4(texture(History, uv).rgb, Retention); }
            """;

    public static void reset() {
        ready = false;
        world = null;
        lastCamera = null;
        lastFrame = 0;
    }

    public static void render(MinecraftClient client, float strength, double resetDistance) {
        if (failed) return;
        int w = client.getFramebuffer().textureWidth;
        int h = client.getFramebuffer().textureHeight;
        if (w < 1 || h < 1) { reset(); return; }
        // Skip non-main render targets (shader mods may render auxiliary passes).
        int framebuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        if (framebuffer != client.getFramebuffer().fbo) { reset(); return; }
        Vec3d camera = client.gameRenderer.getCamera().getPos();
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000.0;
        if (world != client.world || lastCamera == null
                || lastCamera.squaredDistanceTo(camera) > resetDistance * resetDistance || dt > 0.25) {
            ready = false;
        }
        // Save state before even allocating GL objects (allocation changes bindings too).
        int active = glGetInteger(GL_ACTIVE_TEXTURE);
        glActiveTexture(GL_TEXTURE0);
        int tex = glGetInteger(GL_TEXTURE_BINDING_2D);
        int sampler = glGetInteger(GL_SAMPLER_BINDING);
        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int previousVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int readFramebuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int srcRgb = glGetInteger(GL_BLEND_SRC_RGB), dstRgb = glGetInteger(GL_BLEND_DST_RGB);
        int srcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA), dstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
        int eqRgb = glGetInteger(GL_BLEND_EQUATION_RGB), eqAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA);
        boolean blend = glIsEnabled(GL_BLEND), depth = glIsEnabled(GL_DEPTH_TEST);
        boolean cull = glIsEnabled(GL_CULL_FACE), scissor = glIsEnabled(GL_SCISSOR_TEST);
        boolean depthMask = glGetBoolean(GL_DEPTH_WRITEMASK);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewport = stack.mallocInt(4);
            glGetIntegerv(GL_VIEWPORT, viewport);
            ByteBuffer mask = stack.malloc(4);
            glGetBooleanv(GL_COLOR_WRITEMASK, mask);
            try {
                init();
                glBindTexture(GL_TEXTURE_2D, texture);
                glBindSampler(0, 0);
                if (width != w || height != h) {
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, w, h, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
                    width = w;
                    height = h;
                    ready = false;
                }
                glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);
                if (ready && dt > 0) {
                    // Same decay time at 30/60/144+ FPS rather than a fixed per-frame ghost trail.
                    float retention = (float) Math.pow(strength, dt * 60.0);
                    glViewport(0, 0, w, h);
                    glDisable(GL_DEPTH_TEST);
                    glDisable(GL_CULL_FACE);
                    glDisable(GL_SCISSOR_TEST);
                    glDepthMask(false);
                    glColorMask(true, true, true, false);
                    glEnable(GL_BLEND);
                    glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
                    glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ZERO, GL_ONE);
                    glUseProgram(program);
                    glUniform1f(opacityUniform, retention);
                    glBindVertexArray(vao);
                    glDrawArrays(GL_TRIANGLES, 0, 3);
                }
                // Store the accumulated world image. The HUD has not been rendered yet.
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
                ready = true;
                world = client.world;
                lastCamera = camera;
                lastFrame = now;
            } catch (RuntimeException e) {
                failed = true;
                reset();
                NuxVisuals.LOGGER.warn("MotionBlur disabled: OpenGL initialization/rendering failed", e);
            } finally {
                glBindFramebuffer(GL_READ_FRAMEBUFFER, readFramebuffer);
                glUseProgram(previousProgram);
                glBindVertexArray(previousVao);
                glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
                glBlendEquationSeparate(eqRgb, eqAlpha);
                restore(GL_BLEND, blend);
                restore(GL_DEPTH_TEST, depth);
                restore(GL_CULL_FACE, cull);
                restore(GL_SCISSOR_TEST, scissor);
                glDepthMask(depthMask);
                glColorMask(mask.get(0) != 0, mask.get(1) != 0, mask.get(2) != 0, mask.get(3) != 0);
                glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                glBindSampler(0, sampler);
                glBindTexture(GL_TEXTURE_2D, tex);
                glActiveTexture(active);
            }
        }
    }

    private static void restore(int capability, boolean enabled) {
        if (enabled) glEnable(capability); else glDisable(capability);
    }

    private static void init() {
        if (program != 0) return;
        int vertex = 0, fragment = 0, linked = 0;
        try {
            vertex = compile(GL_VERTEX_SHADER, VERTEX);
            fragment = compile(GL_FRAGMENT_SHADER, FRAGMENT);
            linked = glCreateProgram();
            glAttachShader(linked, vertex);
            glAttachShader(linked, fragment);
            glLinkProgram(linked);
            if (glGetProgrami(linked, GL_LINK_STATUS) == GL_FALSE) {
                throw new IllegalStateException(glGetProgramInfoLog(linked));
            }
            glUseProgram(linked);
            glUniform1i(glGetUniformLocation(linked, "History"), 0);
            opacityUniform = glGetUniformLocation(linked, "Retention");
            vao = glGenVertexArrays();
            texture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            program = linked;
            linked = 0;
        } finally {
            if (vertex != 0) glDeleteShader(vertex);
            if (fragment != 0) glDeleteShader(fragment);
            if (linked != 0) glDeleteProgram(linked);
        }
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException(log);
        }
        return shader;
    }

    public static void close() {
        if (texture != 0) glDeleteTextures(texture);
        if (program != 0) glDeleteProgram(program);
        if (vao != 0) glDeleteVertexArrays(vao);
        texture = program = vao = width = height = 0;
        failed = false;
        reset();
    }
    private MotionBlurRenderer() { }
}
