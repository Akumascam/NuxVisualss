package com.nuxvisuals.ui;

import com.nuxvisuals.account.AccountManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

// In-Game Account Switcher: основной аккаунт лаунчера + оффлайн-ники
public class NuxAccountScreen extends Screen {
    private static final int PANEL_WIDTH = 240;
    private static final int PADDING = 8;
    private static final int TITLE_HEIGHT = 16;
    private static final int ROW_HEIGHT = 22;
    private static final int ROW_GAP = 4;
    private static final int VISIBLE_ROWS = 5;
    private static final int INPUT_HEIGHT = 20;
    private static final int ADD_WIDTH = 64;
    private static final int BACK_HEIGHT = 20;

    // синяя палитра
    private static final int PANEL = 0xF00E1526;
    private static final int ROW = 0xFF16203A;
    private static final int ROW_HOVER = 0xFF1D2A4A;
    private static final int TEXT = 0xFFE8EEFF;
    private static final int MUTED = 0xFF8A9BC4;
    private static final int ACCENT = 0xFF3B82F6;
    private static final int ACCENT_LIGHT = 0xFF60A5FA;
    private static final int DANGER = 0xFFFF6B6B;

    private final Screen parent;
    private String input = "";
    private String error;
    private String info;
    private int scroll;
    private long errorTime;

    private record Entry(String name, boolean original) {
    }

    public NuxAccountScreen(Screen parent) {
        super(Text.literal("Аккаунты"));
        this.parent = parent;
    }

    private List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        list.add(new Entry(AccountManager.original().getUsername(), true));
        for (String nick : AccountManager.offline()) {
            list.add(new Entry(nick, false));
        }
        return list;
    }

    private int listHeight() {
        return VISIBLE_ROWS * ROW_HEIGHT + (VISIBLE_ROWS - 1) * ROW_GAP;
    }

    private int panelHeight() {
        return PADDING + TITLE_HEIGHT + PADDING + listHeight() + PADDING + INPUT_HEIGHT + 14 + BACK_HEIGHT + PADDING;
    }

    private int panelX() {
        return this.width / 2 - PANEL_WIDTH / 2;
    }

    private int panelY() {
        return Math.max(4, this.height / 2 - panelHeight() / 2);
    }

    private int listTop() {
        return panelY() + PADDING + TITLE_HEIGHT + PADDING;
    }

    private int inputTop() {
        return listTop() + listHeight() + PADDING;
    }

    private int innerWidth() {
        return PANEL_WIDTH - PADDING * 2;
    }

    private int maxScroll() {
        return Math.max(0, entries().size() - VISIBLE_ROWS);
    }

    @Override
    protected void init() {
        int x = panelX() + PADDING;
        addDrawableChild(new NuxMenuButton(x + innerWidth() - ADD_WIDTH, inputTop(), ADD_WIDTH, INPUT_HEIGHT,
                "Добавить", this.textRenderer, true, false, b -> addAccount()));
        addDrawableChild(new NuxMenuButton(x, inputTop() + INPUT_HEIGHT + 14, innerWidth(), BACK_HEIGHT,
                "Назад", this.textRenderer, true, false, b -> this.close()));
    }

    private void addAccount() {
        String result = AccountManager.add(input);
        if (result != null) {
            error = result;
            info = null;
            errorTime = System.currentTimeMillis();
        } else {
            info = "Аккаунт " + input.trim() + " добавлен";
            error = null;
            input = "";
            scroll = maxScroll();
        }
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        NuxBackground.render(context, this.width, this.height);
        // панель рисуем здесь: super.render() сначала рисует фон, потом кнопки поверх него
        drawPanel(context, mouseX, mouseY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPanel(DrawContext context, int mouseX, int mouseY) {
        int px = panelX();
        int py = panelY();
        NuxClickGui.roundedRect(context, px, py, PANEL_WIDTH, panelHeight(), 8, PANEL);

        // заголовок
        int x = px + PADDING;
        int titleY = py + PADDING + (TITLE_HEIGHT - 8) / 2;
        NuxFont.draw(context, this.textRenderer, "Аккаунты", x, titleY, TEXT);
        String now = "сейчас: " + AccountManager.current();
        NuxFont.draw(context, this.textRenderer, now,
                x + innerWidth() - NuxFont.width(this.textRenderer, now), titleY, MUTED);

        drawList(context, mouseX, mouseY);
        drawInput(context);
    }

    private void drawList(DrawContext context, int mouseX, int mouseY) {
        List<Entry> entries = entries();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        String current = AccountManager.current();
        int x = panelX() + PADDING;
        int w = innerWidth();

        for (int i = 0; i < VISIBLE_ROWS && i + scroll < entries.size(); i++) {
            Entry entry = entries.get(i + scroll);
            int y = listTop() + i * (ROW_HEIGHT + ROW_GAP);
            boolean hovered = inside(x, y, x + w, y + ROW_HEIGHT, mouseX, mouseY);
            boolean onOriginal = this.client.getSession() == AccountManager.original();
            boolean active = entry.original() ? onOriginal : (!onOriginal && entry.name().equals(current));
            NuxClickGui.roundedRect(context, x, y, w, ROW_HEIGHT, 5, hovered ? ROW_HOVER : ROW);

            // слева — точка-статус
            NuxClickGui.circle(context, x + 9, y + ROW_HEIGHT / 2, 3, active ? ACCENT_LIGHT : 0xFF2A3656);
            int textY = y + (ROW_HEIGHT - 8) / 2;
            NuxFont.draw(context, this.textRenderer, entry.name(), x + 18, textY, TEXT);

            String tag = entry.original()
                    ? (AccountManager.original().getAccountType() == Session.AccountType.MSA ? "Microsoft" : "Лаунчер")
                    : "Offline";
            int tagX = x + 18 + NuxFont.width(this.textRenderer, entry.name()) + 6;
            NuxFont.draw(context, this.textRenderer, tag, tagX, textY, MUTED);

            // справа — кнопки
            int right = x + w - 6;
            if (!entry.original()) {
                int dx = right - 10;
                boolean delHover = inside(dx - 2, y + 3, right + 2, y + ROW_HEIGHT - 3, mouseX, mouseY);
                NuxFont.draw(context, this.textRenderer, "×", dx + 1, textY, delHover ? DANGER : MUTED);
                right = dx - 6;
            }
            if (active) {
                String label = "Активен";
                NuxFont.draw(context, this.textRenderer, label,
                        right - NuxFont.width(this.textRenderer, label), textY, ACCENT_LIGHT);
            } else {
                String label = "Войти";
                int bw = NuxFont.width(this.textRenderer, label) + 14;
                int bx = right - bw;
                boolean loginHover = inside(bx, y + 4, right, y + ROW_HEIGHT - 4, mouseX, mouseY);
                NuxClickGui.roundedRect(context, bx, y + 4, bw, ROW_HEIGHT - 8, 4, loginHover ? ACCENT_LIGHT : ACCENT);
                NuxFont.draw(context, this.textRenderer, label, bx + 7, textY, 0xFFFFFFFF);
            }
        }

        // полоса прокрутки
        if (maxScroll() > 0) {
            int h = listHeight();
            int barH = Math.max(12, h * VISIBLE_ROWS / entries.size());
            int barY = listTop() + (h - barH) * scroll / maxScroll();
            NuxClickGui.roundedRect(context, x + w + 3, barY, 2, barH, 1, MUTED);
        }
    }

    private void drawInput(DrawContext context) {
        int x = panelX() + PADDING;
        int y = inputTop();
        int w = innerWidth() - ADD_WIDTH - 6;
        NuxClickGui.roundedRect(context, x, y, w, INPUT_HEIGHT, 5, 0xFF0A1020);
        NuxClickGui.roundedRect(context, x, y + INPUT_HEIGHT - 1, w, 1, 0, ACCENT);

        int textY = y + (INPUT_HEIGHT - 8) / 2;
        if (input.isEmpty()) {
            NuxFont.draw(context, this.textRenderer, "Ник нового аккаунта", x + 8, textY, 0xFF5B6B92);
        } else {
            NuxFont.draw(context, this.textRenderer, input, x + 8, textY, TEXT);
        }
        // мигающий курсор
        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = x + 8 + (input.isEmpty() ? 0 : NuxFont.width(this.textRenderer, input) + 1);
            context.fill(cx, textY - 1, cx + 1, textY + 9, ACCENT_LIGHT);
        }

        int msgY = y + INPUT_HEIGHT + 3;
        if (error != null && System.currentTimeMillis() - errorTime < 4000) {
            NuxFont.draw(context, this.textRenderer, error, x + 2, msgY, DANGER);
        } else if (info != null) {
            NuxFont.draw(context, this.textRenderer, info, x + 2, msgY, ACCENT_LIGHT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Entry> entries = entries();
        String current = AccountManager.current();
        int x = panelX() + PADDING;
        int w = innerWidth();

        for (int i = 0; i < VISIBLE_ROWS && i + scroll < entries.size(); i++) {
            Entry entry = entries.get(i + scroll);
            int y = listTop() + i * (ROW_HEIGHT + ROW_GAP);
            if (!inside(x, y, x + w, y + ROW_HEIGHT, mouseX, mouseY)) {
                continue;
            }
            int right = x + w - 6;
            if (!entry.original()) {
                int dx = right - 10;
                if (inside(dx - 2, y + 3, right + 2, y + ROW_HEIGHT - 3, mouseX, mouseY)) {
                    if (entry.name().equals(current)) {
                        AccountManager.loginOriginal();
                    }
                    AccountManager.remove(entry.name());
                    info = "Аккаунт " + entry.name() + " удалён";
                    error = null;
                    return true;
                }
            }
            // клик по строке или по «Войти» — вход
            if (entry.original()) {
                AccountManager.loginOriginal();
            } else {
                AccountManager.loginOffline(entry.name());
            }
            info = "Вы вошли как " + entry.name();
            error = null;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(verticalAmount)));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (input.length() < 16 && (Character.isLetterOrDigit(chr) && chr < 128 || chr == '_')) {
            input += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!input.isEmpty()) {
                input = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0 ? "" : input.substring(0, input.length() - 1);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            addAccount();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            String clip = this.client.keyboard.getClipboard();
            StringBuilder sb = new StringBuilder(input);
            for (char c : clip.toCharArray()) {
                if (sb.length() < 16 && (Character.isLetterOrDigit(c) && c < 128 || c == '_')) {
                    sb.append(c);
                }
            }
            input = sb.toString();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean inside(int left, int top, int right, int bottom, double mx, double my) {
        return mx >= left && mx < right && my >= top && my < bottom;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
