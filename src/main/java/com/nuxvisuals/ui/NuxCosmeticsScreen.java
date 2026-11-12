package com.nuxvisuals.ui;

import com.nuxvisuals.cosmetics.Category;
import com.nuxvisuals.cosmetics.Cosmetic;
import com.nuxvisuals.cosmetics.CosmeticData;
import com.nuxvisuals.cosmetics.Cosmetics;
import com.nuxvisuals.cosmetics.CustomCosmetics;
import com.nuxvisuals.cosmetics.Draw;
import com.nuxvisuals.cosmetics.Rig;
import com.nuxvisuals.module.impl.ClickGuiModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Меню 3D-косметики: вкладки сверху, сетка карточек с живыми 3D-превью слева, «Твой образ» справа
public class NuxCosmeticsScreen extends Screen {
    private static final int BG = 0xE010141E;
    private static final int CARD = 0xFF181D29;
    private static final int CARD_HOVER = 0xFF1F2534;
    private static final int PANEL = 0xFF141925;
    private static final int TEXT = 0xFFEDF1FA;
    private static final int SOFT = 0xFFC3CBDB;
    private static final int MUTED = 0xFF7C869A;
    private static final int DIM = 0xFFAFB8CC;

    // пустые «спрайты» 7x7 — только как ключи для PNG-иконок
    private static final String[] I_ALL = key();
    private static final String[] I_REFRESH = key();
    private static final String[] I_SAVE = key();
    private static final String[] I_TRASH = key();
    private static final String[] I_CHECK = key();
    private static final Map<Category, String[]> I_CAT = new EnumMap<>(Category.class);

    static {
        NuxIcons.register(I_ALL, "display");
        NuxIcons.register(I_REFRESH, "refresh");
        NuxIcons.register(I_SAVE, "save");
        NuxIcons.register(I_TRASH, "trash");
        NuxIcons.register(I_CHECK, "check");
        for (Category c : Category.values()) {
            String[] k = key();
            NuxIcons.register(k, c.icon);
            I_CAT.put(c, k);
        }
    }

    private static String[] key() {
        return new String[]{".......", ".......", ".......", ".......", ".......", ".......", "......."};
    }

    private static Category tab; // null = «Все»
    private final Screen parent;
    private double scroll;
    private double scrollAnim;
    private int maxScroll;
    private int mouseX;
    private int mouseY;
    private int[] clip;
    private String message;
    private long messageTime;
    private final List<Hit> hits = new ArrayList<>();

    private int cachedCatalog=-1;
    private boolean atelierOnly;
    private Category cachedTab;
    private List<Cosmetic> cachedItems=List.of();
    private String hoverId;
    private long hoverStart;
    private float mannequinYaw = 200f;
    private boolean draggingPreview;
    private int[] previewBox = new int[4];

    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
    }

    public NuxCosmeticsScreen(Screen parent) {
        super(Text.literal("Косметика"));
        this.parent = parent;
    }

    private static int accent() {
        return 0xFF000000 | (ClickGuiModule.get()==null ? ClickGuiModule.ACCENT : ClickGuiModule.get().accentColor());
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    private void text(DrawContext context, String s, int x, int y, int color) {
        NuxFont.draw(context, this.textRenderer, s, x, y, color);
    }

    private int width(String s) {
        return NuxFont.width(this.textRenderer, s);
    }

    private String fit(String s, int max) {
        if (width(s) <= max) {
            return s;
        }
        while (s.length() > 1 && width(s + "…") > max) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    private boolean hover(int x1, int y1, int x2, int y2) {
        if (clip != null && (mouseX < clip[0] || mouseX >= clip[2] || mouseY < clip[1] || mouseY >= clip[3])) {
            return false;
        }
        return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
    }

    private void hit(int x1, int y1, int x2, int y2, Runnable action) {
        if (clip != null) {
            x1 = Math.max(x1, clip[0]);
            y1 = Math.max(y1, clip[1]);
            x2 = Math.min(x2, clip[2]);
            y2 = Math.min(y2, clip[3]);
            if (x1 >= x2 || y1 >= y2) {
                return;
            }
        }
        hits.add(new Hit(x1, y1, x2, y2, action));
    }

    private void msg(String s) {
        message = s;
        messageTime = System.currentTimeMillis();
    }

    private static void rect(DrawContext context, int x, int y, int w, int h, int r, int color) {
        NuxShapes.roundedRect(context, x, y, w, h, r, color);
    }

    private static float now() {
        return (System.currentTimeMillis() % 3_600_000L) / 50f;
    }

    // ---------- 3D-превью ----------

    // один предмет, вписанный в прямоугольник
    private void preview(DrawContext context, Cosmetic c, int x, int y, int w, int h, float yaw, boolean animate) {
        float[] b = Cosmetics.bounds(c);
        float cx = (b[0] + b[3]) / 2f;
        float cy = (b[1] + b[4]) / 2f;
        float cz = (b[2] + b[5]) / 2f;
        float dx = b[3] - b[0];
        float dy = b[4] - b[1];
        float dz = b[5] - b[2];
        float horiz = (float) Math.sqrt(dx * dx + dz * dz);
        float s = Math.min(w / Math.max(1f, horiz), h / Math.max(1f, dy * 1.08f)) * 0.95f;
        CosmeticData d = new CosmeticData();
        d.time = animate ? now() : 0;
        MatrixStack ms = context.getMatrices();
        ms.push();
        ms.translate(x + w / 2f, y + h / 2f, 150);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-16));
        ms.scale(s, s, -s);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        ms.translate(-cx, -cy, -cz);
        context.draw(consumers -> {
            Draw r = new Draw(ms, consumers.getBuffer(RenderLayer.getDebugQuads()), 1f, d);
            Cosmetics.renderOne(r, Rig.MANNEQUIN, c);
        });
        ms.pop();
    }

    // манекен со всей надетой косметикой (когда ты не в мире)
    private void mannequin(DrawContext context, int x, int y, int w, int h) {
        float s = Math.min(w / 36f, h / 46f);
        CosmeticData d = new CosmeticData();
        d.time = now();
        MatrixStack ms = context.getMatrices();
        ms.push();
        ms.translate(x + w / 2f, y + h / 2f + 2, 150);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10));
        ms.scale(s, s, -s);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mannequinYaw));
        ms.translate(0, -7, 0);
        context.draw(consumers -> {
            Draw r = new Draw(ms, consumers.getBuffer(RenderLayer.getDebugQuads()), 1f, d);
            Cosmetics.mannequin(r);
            Cosmetics.renderAll(r, Rig.MANNEQUIN);
        });
        ms.pop();
    }

    private static float baseYaw(Category c) {
        return switch (c) {
            case CAPE, WINGS, BACK -> 205f;
            case MODEL -> 20f;
            case SWORD -> 110f;
            default -> 25f;
        };
    }

    // ---------- отрисовка ----------

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.client.world != null) {
            context.fillGradient(0, 0, this.width, this.height, 0x60000000, 0xA0000000);
        } else {
            NuxBackground.render(context, this.width, this.height);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        hits.clear();
        clip = null;
        int accent = accent();

        int w = Math.min(600, this.width - 16);
        int h = Math.min(330, this.height - 16);
        int x0 = this.width / 2 - w / 2;
        int y0 = this.height / 2 - h / 2;

        // вкладки: Все + разделы
        Category[] cats = Category.values();
        int n = cats.length + 1;
        int tw = (w - (n - 1) * 4) / n;
        for (int i = 0; i < n; i++) {
            Category c = i == 0 ? null : cats[i - 1];
            int tx = x0 + i * (tw + 4);
            boolean sel = tab == c;
            boolean hov = hover(tx, y0, tx + tw, y0 + 30);
            rect(context, tx, y0, tw, 30, 7, sel ? NuxShapes.blend(CARD, accent, 0.22f) : hov ? CARD_HOVER : withAlpha(CARD, 0xE0));
            if (sel) {
                rect(context, tx + tw / 2 - 10, y0 + 28, 20, 2, 1, accent);
            }
            String label = fit(c == null ? "Все" : c.label, tw - 6);
            NuxIcons.draw(context, c == null ? I_ALL : I_CAT.get(c), tx + tw / 2 - 4, y0 + 5, 1, sel ? TEXT : MUTED);
            text(context, label, tx + tw / 2 - width(label) / 2, y0 + 18, sel ? TEXT : SOFT);
            hit(tx, y0, tx + tw, y0 + 30, () -> {
                tab = c;
                if(c!=null&&c!=Category.WINGS&&c!=Category.PET&&c!=Category.HAT){atelierOnly=false;cachedCatalog=-1;}
                scroll = scrollAnim = 0;
            });
        }

        int toolbar=y0+34;
        button(context,x0,toolbar,90,18,null,"Создать",false,accent,()->client.setScreen(new CosmeticCreatorScreen(this)));
        button(context,x0+95,toolbar,90,18,null,"Обновить",false,accent,()->{Cosmetics.reloadCustom();msg(CustomCosmetics.lastReport);});
        button(context,x0+190,toolbar,110,18,null,"Папка моделей",false,accent,()->{
            try {java.nio.file.Files.createDirectories(CustomCosmetics.directory());net.minecraft.util.Util.getOperatingSystem().open(CustomCosmetics.directory().toFile());}
            catch(Exception e){msg("Не удалось открыть папку");}
        });
        int top = y0 + 58;
        int bodyH = h - 58;
        int rightW = Math.min(170, w / 3);
        int leftW = w - rightW - 6;
        drawGrid(context, x0, top, leftW, bodyH, accent);
        drawOutfit(context, x0 + leftW + 6, top, rightW, bodyH, accent, mouseX, mouseY);
    }

    private List<Cosmetic> shown() {
        int version=Cosmetics.catalogVersion();
        if(version!=cachedCatalog || cachedTab!=tab) {
            cachedCatalog=version;cachedTab=tab;
            cachedItems=Cosmetics.items().stream().filter(c->(tab==null || c.category()==tab)&&(!atelierOnly || c.id().startsWith("atelier:"))).toList();
        }
        return cachedItems;
    }

    private void drawGrid(DrawContext context, int x, int y, int w, int h, int accent) {
        rect(context, x, y, w, h, 9, BG);
        NuxIcons.draw(context, tab == null ? I_ALL : I_CAT.get(tab), x + 10, y + 9, 1, accent);
        text(context, tab == null ? "Все косметики" : tab.label, x + 22, y + 9, TEXT);
        List<Cosmetic> list = shown();
        String found = (atelierOnly?"Atelier":"Все")+": "+list.size()+"  ›";
        if (tab == com.nuxvisuals.cosmetics.Category.SWORD) {
            // выбор меча, на который надевается скин
            clip = null;
            String label = "На меч: " + Cosmetics.swordTargetLabel() + "  ›";
            int pw = width(label) + 14;
            int px = x + w - 8 - pw;
            boolean ph = hover(px, y + 4, px + pw, y + 20);
            rect(context, px, y + 4, pw, 16, 6, ph ? 0x40FFFFFF : 0x22FFFFFF);
            text(context, label, px + 7, y + 8, ph ? TEXT : accent);
            hit(px, y + 4, px + pw, y + 20, () -> {
                Cosmetics.cycleSwordTarget(hasShiftDown() ? -1 : 1);
                Cosmetics.save();
                msg("Скин меча: " + Cosmetics.swordTargetLabel());
            });
        } else {
            int fx=x+w-16-width(found);
            boolean fh=hover(fx-4,y+3,x+w-6,y+22);
            rect(context,fx-4,y+3,width(found)+14,19,5,fh?0x25FFFFFF:0x12FFFFFF);
            text(context, found, fx, y + 9, atelierOnly?accent:SOFT);
            hit(fx-4,y+3,x+w-6,y+22,()->{
                atelierOnly=!atelierOnly;cachedCatalog=-1;scroll=scrollAnim=0;
                msg(atelierOnly?"Коллекция Atelier":"Все предметы");
            });
        }

        int gx = x + 8;
        int gy = y + 26;
        int gw = w - 16;
        int gh = h - 26 - 8;
        int cols = Math.max(2, (gw + 6) / 100);
        int cw = (gw - (cols - 1) * 6) / cols;
        int ch = 108;

        scrollAnim += (scroll - scrollAnim) * 0.35;
        int off = (int) Math.round(scrollAnim);
        int rows = (list.size() + cols - 1) / cols;
        int content = rows * (ch + 6) - 6;
        maxScroll = Math.max(0, content - gh);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        clip = new int[]{gx, gy, gx + gw, gy + gh};
        context.enableScissor(gx, gy, gx + gw, gy + gh);
        String hovered = null;
        for (int i = 0; i < list.size(); i++) {
            Cosmetic item = list.get(i);
            int cx = gx + (i % cols) * (cw + 6);
            int cy = gy + (i / cols) * (ch + 6) - off;
            if (cy > gy + gh || cy + ch < gy) {
                continue;
            }
            boolean on = Cosmetics.isEquipped(item);
            boolean hov = hover(cx, cy, cx + cw, cy + ch);
            if (hov) {
                hovered = item.id();
            }
            if (on) {
                rect(context, cx - 1, cy - 1, cw + 2, ch + 2, 9, withAlpha(accent, 0x90));
            }
            rect(context, cx, cy, cw, ch, 8, on ? NuxShapes.blend(CARD, accent, 0.14f) : hov ? CARD_HOVER : CARD);
            context.fillGradient(cx + 6, cy + ch / 2 - 6, cx + cw - 6, cy + ch - 28, 0x00000000,
                    withAlpha(on ? accent : 0xFFFFFF, on ? 0x26 : 0x0A));

            // живое 3D-превью (крутится при наведении)
            float yaw = baseYaw(item.category());
            if (hov && item.id().equals(hoverId)) {
                yaw += (System.currentTimeMillis() - hoverStart) * 0.12f;
            }
            preview(context, item, cx + 6, cy + 6, cw - 12, ch - 36, yaw, hov);

            String name = fit(item.name().toUpperCase(), cw - 14);
            text(context, name, cx + 7, cy + ch - 24, TEXT);
            text(context, item.category().single, cx + 7, cy + ch - 13, DIM);
            if (!item.tag().isEmpty()) {
                int tagColor = switch (item.tag()) {
                    case "Хеллоуин" -> 0xFFFF9A3C;
                    case "Зима" -> 0xFF7CC8FF;
                    default -> 0xFFFF8FC8;
                };
                String tg = item.tag();
                text(context, tg, cx + cw - 7 - width(tg), cy + ch - 13, tagColor);
            }
            if (on) {
                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 300);
                NuxShapes.circle(context, cx + 10, cy + 10, 5, accent);
                NuxIcons.draw(context, I_CHECK, cx + 7, cy + 7, 1, 0xFFFFFFFF);
                context.getMatrices().pop();
                rect(context, cx + 7, cy + ch - 3, cw - 14, 2, 1, accent);
            }
            hit(cx, cy, cx + cw, cy + ch, () -> {
                Cosmetics.toggle(item);
                Cosmetics.save();
                msg(Cosmetics.isEquipped(item) ? "Надето: " + item.name() : "Снято: " + item.name());
            });
        }
        if (hovered == null) {
            hoverId = null;
        } else if (!hovered.equals(hoverId)) {
            hoverId = hovered;
            hoverStart = System.currentTimeMillis();
        }
        context.disableScissor();
        clip = null;
        if (list.isEmpty()) {
            String s = "Пусто";
            text(context, s, gx + gw / 2 - width(s) / 2, gy + gh / 2 - 4, DIM);
        }
        if (maxScroll > 0) {
            int barH = Math.max(16, gh * gh / (content + 1));
            int barY = gy + (int) ((gh - barH) * (scrollAnim / maxScroll));
            rect(context, x + w - 5, barY, 2, barH, 1, 0x50FFFFFF);
        }
    }

    private void button(DrawContext context, int x, int y, int w, int h, String[] icon, String label,
                        boolean primary, int accent, Runnable action) {
        boolean hov = hover(x, y, x + w, y + h);
        int bg = primary ? (hov ? NuxShapes.blend(accent, 0xFFFFFFFF, 0.2f) : accent) : (hov ? CARD_HOVER : CARD);
        rect(context, x, y, w, h, 7, bg);
        int lw = (icon != null ? 12 : 0) + width(label);
        int lx = x + w / 2 - lw / 2;
        int color = primary ? ThemeContrast.ink(bg) : hov ? TEXT : SOFT;
        if (icon != null) {
            NuxIcons.draw(context, icon, lx, y + h / 2 - 4, 1, color);
            lx += 12;
        }
        NuxFont.drawOn(context,textRenderer,label,lx,y+h/2-4,color,bg);
        hit(x, y, x + w, y + h, action);
    }

    private void drawOutfit(DrawContext context, int x, int y, int w, int h, int accent, int mouseX, int mouseY) {
        rect(context, x, y, w, h, 9, BG);
        NuxIcons.draw(context, NuxIcons.PERSON, x + 10, y + 9, 1, accent);
        text(context, "Твой образ", x + 24, y + 9, TEXT);
        int rx = x + w - 22;
        boolean rh = hover(rx - 3, y + 5, rx + 13, y + 21);
        if (rh) {
            rect(context, rx - 3, y + 5, 16, 16, 5, 0x18FFFFFF);
        }
        NuxIcons.draw(context, I_REFRESH, rx, y + 9, 1, rh ? TEXT : MUTED);
        hit(rx - 3, y + 5, rx + 13, y + 21, () -> {
            mannequinYaw = 200f;
            msg("Вид сброшен");
        });

        int count = Cosmetics.count();
        int listH = 18 + Math.max(1, count) * 12 + 4;
        int pw = w - 16;
        int pxx = x + 8;
        int pyy = y + 26;
        int ph = Math.max(50, h - 26 - listH - 6 - 44);

        // превью
        rect(context, pxx, pyy, pw, ph, 7, PANEL);
        context.fillGradient(pxx + 4, pyy + ph / 2, pxx + pw - 4, pyy + ph - 2, 0x00000000, withAlpha(accent, 0x22));
        previewBox = new int[]{pxx, pyy, pxx + pw, pyy + ph};
        context.enableScissor(pxx, pyy, pxx + pw, pyy + ph);
        if (this.client.player != null) {
            int size = Math.max(20, Math.min(52, (ph - 16) / 2));
            InventoryScreen.drawEntity(context, pxx, pyy, pxx + pw, pyy + ph, size, 0.0625f,
                    mouseX, mouseY, this.client.player);
        } else {
            if (!draggingPreview) {
                mannequinYaw += 0.6f;
            }
            mannequin(context, pxx, pyy, pw, ph);
        }
        context.disableScissor();

        // надетые предметы
        int iy = pyy + ph + 6;
        rect(context, pxx, iy, pw, listH, 7, PANEL);
        text(context, "Надетые предметы", pxx + 7, iy + 6, SOFT);
        String cnt = count + "/" + Cosmetics.max();
        text(context, cnt, pxx + pw - 7 - width(cnt), iy + 6, MUTED);
        if (count == 0) {
            String t = "Выбери предмет слева";
            text(context, t, pxx + pw / 2 - width(t) / 2, iy + 20, DIM);
        } else {
            int ry = iy + 19;
            for (Category c : Category.values()) {
                Cosmetic item = Cosmetics.equipped(c);
                if (item == null) {
                    continue;
                }
                NuxIcons.draw(context, I_CAT.get(c), pxx + 7, ry, 1, accent);
                text(context, fit(item.name(), pw - 40), pxx + 19, ry, TEXT);
                int xx = pxx + pw - 13;
                boolean xh = hover(xx - 3, ry - 2, xx + 9, ry + 10);
                text(context, "×", xx, ry, xh ? 0xFFFF6B6B : MUTED);
                hit(xx - 3, ry - 2, xx + 9, ry + 10, () -> {Cosmetics.unequip(c);Cosmetics.save();});
                ry += 12;
            }
        }

        int by = iy + listH + 6;
        button(context, pxx, by, pw, 20, I_SAVE, "Сохранить образ", true, accent,
                () -> msg(Cosmetics.save() ? "Образ сохранён" : "Не удалось сохранить"));
        String clear = "Снять всё";
        int clw = 12 + width(clear);
        int clx = x + w / 2 - clw / 2;
        boolean clh = hover(clx - 4, by + 23, clx + clw + 4, by + 37);
        NuxIcons.draw(context, I_TRASH, clx, by + 26, 1, clh ? 0xFFFF6B6B : MUTED);
        text(context, clear, clx + 12, by + 26, clh ? TEXT : MUTED);
        hit(clx - 4, by + 23, clx + clw + 4, by + 37, () -> {
            Cosmetics.clear();
            Cosmetics.save();
            msg("Всё снято");
        });
        if (message != null && System.currentTimeMillis() - messageTime < 2500) {
            String m = fit(message, w - 10);
            int mw = width(m) + 12;
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 400);
            rect(context, x + w / 2 - mw / 2, pyy + 4, mw, 14, 6, 0xE0141925);
            text(context, m, x + w / 2 - mw / 2 + 6, pyy + 7, 0xFF7AA8FF);
            context.getMatrices().pop();
        }
    }

    // ---------- ввод ----------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit hit = hits.get(i);
                if (mx >= hit.x1() && mx < hit.x2() && my >= hit.y1() && my < hit.y2()) {
                    hit.action().run();
                    return true;
                }
            }
            if (this.client.player == null && mx >= previewBox[0] && mx < previewBox[2]
                    && my >= previewBox[1] && my < previewBox[3]) {
                draggingPreview = true;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingPreview) {
            mannequinYaw += (float) dx * 2.5f;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingPreview = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - vertical * 30));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        Cosmetics.save();
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
