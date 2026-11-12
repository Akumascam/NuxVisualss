package com.nuxvisuals.ui;

import com.nuxvisuals.cosmetics.Category;
import com.nuxvisuals.cosmetics.Cosmetics;
import com.nuxvisuals.cosmetics.CustomCosmetics;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Small built-in template creator. Detailed geometry remains editable/importable as JSON. */
public final class CosmeticCreatorScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget name;
    private int categoryIndex,colorIndex;
    private String status="";
    private static final Category[] TYPES={Category.HAT,Category.WINGS,Category.CAPE,Category.BACK,Category.PET,Category.SWORD};
    private static final int[] COLORS={0xA3ACFF,0x8DD4FF,0xA5E0C7,0xEBA7CF,0xEDC47C,0xECEFF6};
    public CosmeticCreatorScreen(Screen parent) {super(Text.literal("Своя косметика"));this.parent=parent;}
    @Override protected void init() {
        int x=width/2-110,y=height/2-64;
        name=addDrawableChild(new TextFieldWidget(textRenderer,x,y,220,20,Text.literal("Название")));
        name.setMaxLength(48);name.setText("Моя косметика");
        addDrawableChild(ButtonWidget.builder(Text.literal("Раздел: "+TYPES[categoryIndex].label),b->{
            categoryIndex=(categoryIndex+1)%TYPES.length;b.setMessage(Text.literal("Раздел: "+TYPES[categoryIndex].label));
        }).dimensions(x,y+26,220,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Цвет: #"+Integer.toHexString(COLORS[colorIndex]).toUpperCase()),b->{
            colorIndex=(colorIndex+1)%COLORS.length;b.setMessage(Text.literal("Цвет: #"+Integer.toHexString(COLORS[colorIndex]).toUpperCase()));
        }).dimensions(x,y+52,220,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Создать шаблон"),b->{
            try {CustomCosmetics.create(name.getText(),TYPES[categoryIndex],COLORS[colorIndex]);Cosmetics.reloadCustom();
                client.setScreen(parent);
            }catch(Exception e){status="Не удалось сохранить файл";}
        }).dimensions(x,y+82,220,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"),b->close()).dimensions(x,y+108,220,20).build());
        setInitialFocus(name);
    }
    @Override public void render(DrawContext context,int mx,int my,float delta) {
        super.render(context,mx,my,delta);
        NuxFont.drawCentered(context,textRenderer,title,width/2,height/2-92,0xFFFFFFFF);
        String help="Шаблон можно изменить в JSON: размеры, повороты, цвет, анимация";
        NuxFont.drawCentered(context,textRenderer,Text.literal(help),width/2,height/2+88,0xFFD1D9EC);
        if(!status.isEmpty())NuxFont.drawCentered(context,textRenderer,Text.literal(status),width/2,height/2+102,0xFFFF9B9B);
    }
    @Override public void close(){client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
