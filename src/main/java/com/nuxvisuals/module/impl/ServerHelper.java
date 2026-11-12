package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.InvUtil;
import com.nuxvisuals.module.Keys;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Hand;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import java.util.*;

/** Configurable client-side hotbar binds. Does not bypass server cooldowns or restrictions. */
public class ServerHelper extends Module {
    private record Entry(ServerItemProfiles.Profile profile, String title, KeySetting key, ModeSetting mode, StringSetting names, Item item, boolean fallback) {}
    private final ModeSetting server = add(new ModeSetting("Сервер", "Авто", "Авто", "FunTime", "HolyWorld", "Любой"));
    private final BoolSetting byItem = add(new BoolSetting("Detect By Item", true).visibleIf(()->activeProfile()==ServerItemProfiles.Profile.FUNTIME));
    private final BoolSetting notify = add(new BoolSetting("Notify", true));
    private final SliderSetting repeat = add(new SliderSetting("Повтор при удержании", 12, 5, 40, 1, " тик").visibleIf(()->activeProfile()==ServerItemProfiles.Profile.HOLYWORLD));
    private final List<Entry> entries = new ArrayList<>();
    private int previousSlot=-1, usingSlot=-1, useTicks;
    private ClientWorld usingWorld;
    private boolean ownsUseKey;
    private ServerItemProfiles.Profile lastProfile;
    private final Set<Integer> blockedKeys=new HashSet<>();

    public ServerHelper() {
        super("server-helper", "Server Helper", Category.Utilities, false);
        entry(ServerItemProfiles.Profile.FUNTIME,"Трапка", "Trap Key", Items.NETHERITE_SCRAP, true, "трап|ловуш");
        entry(ServerItemProfiles.Profile.FUNTIME,"Пласт", "Plast Key", Items.DRIED_KELP, true, "пласт");
        entry(ServerItemProfiles.Profile.FUNTIME,"Дезориентация", "Disorientation Key", Items.ENDER_EYE, true, "дезориент|оглуш");
        entry(ServerItemProfiles.Profile.FUNTIME,"Явная пыль", "Dust Key", Items.SUGAR, true, "явная пыль|пыль");
        entry(ServerItemProfiles.Profile.FUNTIME,"Огненный смерч", "Fire Tornado Key", Items.FIRE_CHARGE, true, "смерч");
        entry(ServerItemProfiles.Profile.FUNTIME,"Снежок заморозки", "Freeze Key", Items.SNOWBALL, true, "заморо|снеж");
        entry(ServerItemProfiles.Profile.FUNTIME,"Божья аура", "Aura Key", Items.PHANTOM_MEMBRANE, true, "аура");
        entry(ServerItemProfiles.Profile.FUNTIME,"Жемчуг", "Pearl Key", Items.ENDER_PEARL, true, "жемч|перл");
        entry(ServerItemProfiles.Profile.FUNTIME,"Хорус", "Chorus Key", Items.CHORUS_FRUIT, true, "хорус");
        entry(ServerItemProfiles.Profile.FUNTIME,"Заряд ветра", "Wind Charge Key", Items.WIND_CHARGE, true, "заряд ветра|wind charge");
        entry(ServerItemProfiles.Profile.FUNTIME,"Донат-зелье 1", "Donat Potion 1 Key", Items.POTION, false, "сил");
        entry(ServerItemProfiles.Profile.FUNTIME,"Донат-зелье 2", "Donat Potion 2 Key", Items.POTION, false, "скорост");
        entry(ServerItemProfiles.Profile.FUNTIME,"Донат-зелье 3", "Donat Potion 3 Key", Items.POTION, false, "невид");
        holy("Взрывная трапка","взрывная трапка|взрывная ловушка");
        holy("Трапка","трапка|ловушка");
        holy("Снежок","снежок");
        holy("Стан","стан|оглушение");
        holy("Взрывная штучка","взрывная штучка|взрывная штука");
        holy("Тыква Джейка","тыква джейка|тыква джека");
    }
    private void holy(String title,String words){
        entry(ServerItemProfiles.Profile.HOLYWORLD,title,"HolyWorld: "+title+" — клавиша",null,false,words);
    }
    private void entry(ServerItemProfiles.Profile profile,String title,String keyName,Item item,boolean fallback,String words) {
        boolean ft=profile==ServerItemProfiles.Profile.FUNTIME;
        String prefix=ft?title:"HolyWorld: "+title;
        KeySetting key=add(new KeySetting(keyName,-1).visibleIf(()->activeProfile()==profile));
        // FunTime has no mode setting at all: legacy saved hold values are ignored.
        ModeSetting mode=ft?null:add(new ModeSetting(prefix+" — режим","Нажатие","Нажатие","Удержание").visibleIf(()->activeProfile()==profile&&key.get()>=0));
        // Keep old name aliases loadable, but show only the item/key row on FunTime.
        StringSetting names=add(new StringSetting(prefix+" — названия",words,180).visibleIf(()->!ft&&activeProfile()==profile&&key.get()>=0));
        entries.add(new Entry(profile,title,key,mode,names,item,fallback));
    }
    private ServerItemProfiles.Profile activeProfile(){
        var info=mc().getCurrentServerEntry();
        return ServerItemProfiles.resolve(server.get(),info==null?"":info.address);
    }
    private static String name(ItemStack stack) {
        return stack.getName().getString().replaceAll("§.","").toLowerCase(Locale.ROOT);
    }
    public static boolean isCustomConsumable(ItemStack stack) {
        if(stack.isEmpty()) return false;
        String n=name(stack);
        for(String word:new String[]{"трап","ловуш","пласт","дезориент","оглуш","смерч","заморо","аура","пыль"})
            if(n.contains(word)) return true;
        return ServerItemProfiles.matches(ServerItemProfiles.Profile.HOLYWORLD,"","снежок|стан|оглушение|взрывная штучка|взрывная штука|тыква джейка|тыква джека",n);
    }
    private boolean matchesName(Entry e,ItemStack stack) {
        if(stack.isEmpty()) return false;
        if(e.item()==Items.POTION && !stack.contains(DataComponentTypes.POTION_CONTENTS)) return false;
        return ServerItemProfiles.matches(e.profile(),e.title(),e.names().get(),stack.getName().getString());
    }
    private boolean permitted(MinecraftClient client) {
        if(server.is("Любой")) return true;
        var info=client.getCurrentServerEntry();
        String address=info==null?"":info.address.toLowerCase(Locale.ROOT);
        if(server.is("Авто")) return ServerItemProfiles.knownAddress(address) || info==null;
        return true; // An explicitly selected profile also works through alternate server addresses.
    }
    @Override public void onTick(MinecraftClient client) {
        ServerItemProfiles.Profile profile=activeProfile();
        if(profile!=lastProfile){
            finish(client);blockedKeys.clear();
            // Changing profiles must never consume a key that is already held down.
            for(Entry e:entries)if(e.profile()==profile&&e.key().get()>=0&&Keys.isDown(e.key().get()))blockedKeys.add(e.key().get());
            lastProfile=profile;
        }
        blockedKeys.removeIf(key->!Keys.isDown(key));
        if(usingSlot>=0) {
            useTicks++;
            if(client.player==null || client.world!=usingWorld || client.currentScreen!=null || !client.isWindowFocused()
                    || client.player.getInventory().selectedSlot!=usingSlot || useTicks>100
                    || (useTicks>2 && !client.player.isUsingItem())) finish(client);
        }
        boolean used=false;
        for(Entry e:entries) {
            boolean holy=e.profile()==ServerItemProfiles.Profile.HOLYWORLD;
            boolean hold=holy&&e.mode()!=null&&e.mode().is("Удержание");
            // Edge-only on FunTime: execute in this tick, with no added delay or queued repeat.
            boolean trigger=e.key().trigger(hold,holy?repeat.getInt():1);
            // Poll hidden keys too, but never execute entries belonging to the other server.
            if(e.profile()!=profile || blockedKeys.contains(e.key().get()) || !trigger || used || usingSlot>=0 || client.player==null || client.interactionManager==null
                    || client.currentScreen!=null || !client.isWindowFocused()) continue;
            if(!permitted(client)) { if(notify.get())InvUtil.actionBar("§eВыбери профиль FunTime или HolyWorld вручную");continue; }
            if(client.player.isUsingItem()) continue;
            int slot=InvUtil.findHotbar(s->matchesName(e,s));
            if(slot<0 && e.profile()==ServerItemProfiles.Profile.FUNTIME && byItem.get() && e.fallback() && e.item()!=null) slot=InvUtil.findHotbar(s->s.isOf(e.item()));
            if(slot<0) { if(notify.get())InvUtil.actionBar("§cНет в хотбаре: §f"+e.title());continue; }
            ItemStack stack=client.player.getInventory().getStack(slot);
            if(client.player.getItemCooldownManager().isCoolingDown(stack)) {
                if(notify.get())InvUtil.actionBar("§eПредмет ещё перезаряжается");continue;
            }
            previousSlot=client.player.getInventory().selectedSlot;usingSlot=slot;usingWorld=client.world;useTicks=0;
            client.player.getInventory().selectedSlot=slot;
            var result=client.interactionManager.interactItem(client.player,Hand.MAIN_HAND);
            used=true;
            if(result.isAccepted()) client.player.swingHand(Hand.MAIN_HAND);
            boolean sustained=stack.contains(DataComponentTypes.CONSUMABLE) && client.player.isUsingItem();
            if(sustained) { ownsUseKey=true;client.options.useKey.setPressed(true); }
            else finish(client);
        }
    }
    private void finish(MinecraftClient client) {
        if(ownsUseKey) {
            client.options.useKey.setPressed(client.currentScreen==null && client.isWindowFocused() && Keys.isBindingDown(client.options.useKey));
            if(client.player!=null && client.world==usingWorld && client.interactionManager!=null && client.player.isUsingItem())
                client.interactionManager.stopUsingItem(client.player);
        }
        if(client.player!=null && client.world==usingWorld && previousSlot>=0
                && client.player.getInventory().selectedSlot==usingSlot) {
            client.player.getInventory().selectedSlot=previousSlot;
            client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
        }
        previousSlot=usingSlot=-1;usingWorld=null;ownsUseKey=false;useTicks=0;
    }
    @Override public void onDisable() { finish(mc());lastProfile=null;blockedKeys.clear(); }
}
