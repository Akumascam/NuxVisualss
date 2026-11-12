package com.nuxvisuals.module.impl;
import com.nuxvisuals.hud.HudModule;
import com.nuxvisuals.hud.HudIcons;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Counts the entire player inventory + offhand, keeps different potion/name components separate. */
public class ConsumablesHud extends HudModule {
    private static final class Row { ItemStack stack; int count; Row(ItemStack s) { stack=s.copy(); count=s.getCount(); } }
    private final List<Row> rows = new ArrayList<>();
    private final SliderSetting columns = add(new SliderSetting("Колонок", 6, 3, 12, 1));
    private final BoolSetting ammo = add(new BoolSetting("Стрелы и фейерверки", true));
    private final BoolSetting custom = add(new BoolSetting("Донатные предметы", true));
    public ConsumablesHud() { super("consumables", "Расходники", Category.Hud, false, 0.0, 0.7); }
    private boolean consumable(ItemStack s) {
        if (s.isEmpty()) return false;
        if (s.contains(DataComponentTypes.CONSUMABLE) || s.contains(DataComponentTypes.FOOD)
                || s.contains(DataComponentTypes.POTION_CONTENTS)) return true;
        if (s.isOf(Items.ENDER_PEARL) || s.isOf(Items.WIND_CHARGE) || s.isOf(Items.SNOWBALL)
                || s.isOf(Items.EGG) || s.isOf(Items.EXPERIENCE_BOTTLE) || s.isOf(Items.TOTEM_OF_UNDYING)) return true;
        if (ammo.get() && (s.isOf(Items.ARROW) || s.isOf(Items.SPECTRAL_ARROW) || s.isOf(Items.TIPPED_ARROW)
                || s.isOf(Items.FIREWORK_ROCKET))) return true;
        return custom.get() && ServerHelper.isCustomConsumable(s);
    }
    private int cachedAt=Integer.MIN_VALUE;
    private net.minecraft.client.network.ClientPlayerEntity cachedPlayer;
    @Override public void onEnable(){cachedAt=Integer.MIN_VALUE;}
    private void collect(MinecraftClient client) {
        if(client.player!=null && cachedPlayer==client.player && client.player.age-cachedAt>=0 && client.player.age-cachedAt<3)return;
        cachedPlayer=client.player;cachedAt=client.player==null?Integer.MIN_VALUE:client.player.age;
        rows.clear();
        if (client.player == null) return;
        for (int i=0;i<client.player.getInventory().size();i++) {
            // Armor isn't consumable; all inventory, hotbar and offhand indices are otherwise included.
            ItemStack s=client.player.getInventory().getStack(i);
            if (!consumable(s)) continue;
            Row found=null;
            for(Row row:rows) if(ItemStack.areItemsAndComponentsEqual(row.stack,s)) { found=row;break; }
            if(found==null) rows.add(new Row(s)); else found.count+=s.getCount();
        }
    }
    @Override protected void measure(MinecraftClient client, boolean editing) {
        collect(client); width=12+columns.getInt()*24; height=24+Math.max(1,(rows.size()+columns.getInt()-1)/columns.getInt())*25;
    }
    @Override protected void draw(DrawContext context,MinecraftClient client,float delta,boolean editing) {
        panel(context,0,0,width,height); header(context,HudIcons.FLASK,"Расходники");
        if(rows.isEmpty()) { text(context,"нет расходников",7,25,0xFFADB7CC);return; }
        for(int i=0;i<rows.size();i++) {
            Row row=rows.get(i);int x=7+(i%columns.getInt())*24,y=23+(i/columns.getInt())*25;
            context.drawItem(row.stack,x,y);
            String n=Integer.toString(row.count);
            context.getMatrices().push();
            try { context.getMatrices().translate(x+8,y+16,200);context.getMatrices().scale(0.7f,0.7f,1);
                context.drawText(client.textRenderer,n,-client.textRenderer.getWidth(n)/2,0,accentColor(0),true);
            } finally {context.getMatrices().pop();}
        }
    }
}
