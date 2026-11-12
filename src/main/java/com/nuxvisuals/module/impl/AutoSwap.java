package com.nuxvisuals.module.impl;
import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.InvUtil;
import com.nuxvisuals.module.Keys;
import com.nuxvisuals.module.Module;
import com.nuxvisuals.module.setting.*;
import com.nuxvisuals.ui.AutoSwapConfigScreen;
import com.nuxvisuals.ui.AutoSwapWheelScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import java.util.Locale;
/** Hold-to-open item wheel plus optional legacy two-item cycle. */
public class AutoSwap extends Module {
 private static final String[] CHOICES={"Ball","Talisman","Totem","Shield","Golden Apple"};
 private static AutoSwap instance;
 private final KeySetting key=add(new KeySetting("Swap Key",-1));
 private final ModeSetting cycle=add(new ModeSetting("Управление","Круг","Круг","Двойной"));
 private final SliderSetting sectors=add(new SliderSetting("Ячеек круга",3,3,8,1).visibleIf(this::isWheel));
 private final ActionSetting configure=add(new ActionSetting("Настроить предметы круга",this::openConfiguration).visibleIf(this::isWheel));
 private final ItemChoiceSetting[] choices=new ItemChoiceSetting[8];
 private final ModeSetting first=add(new ModeSetting("First Item","Ball",CHOICES).visibleIf(()->!isWheel()));
 private final ModeSetting second=add(new ModeSetting("Second Item","Talisman",CHOICES).visibleIf(()->!isWheel()));
 private final BoolSetting scroll=add(new BoolSetting("Колёсико",true).visibleIf(()->!isWheel()));
 private final KeySetting scrollKey=add(new KeySetting("Клавиша прокрутки",341).visibleIf(()->!isWheel()&&scroll.get()));
 private final BoolSetting invert=add(new BoolSetting("Инверсия колёсика",false).visibleIf(()->!isWheel()&&scroll.get()));
 private final BoolSetting skip=add(new BoolSetting("Пропускать отсутствующие",true).visibleIf(()->!isWheel()));
 private final BoolSetting openInventory=add(new BoolSetting("Open Inventory",true).visibleIf(()->!isWheel()));
 private final SliderSetting delay=add(new SliderSetting("Доп. задержка",0,0,10,1," тик"));
 private final BoolSetting notify=add(new BoolSetting("Notify",true));
 private int stage=-1,timer,slotToSwap=-1,targetIndex;
 private String targetName="";
 private ClientWorld swapWorld;
 private net.minecraft.client.network.ClientPlayerEntity swapPlayer;
 private SwapSettlement settlement;
 private InventoryScreen ownedScreen;
 private ItemStack expected=ItemStack.EMPTY,offBefore=ItemStack.EMPTY;
 private boolean keyWasDown;
 private double wheelRemainder;
 private long nextScrollAt;
 public AutoSwap(){super("auto-swap","Auto Swap",Category.Utilities,false);instance=this;
  for(int i=0;i<choices.length;i++)choices[i]=add(new ItemChoiceSetting("Круг: предмет "+(i+1)).visibleIf(()->false));
 }
 public boolean isWheel(){return cycle.is("Круг");}
 public int wheelSlots(){return sectors.getInt();}
 public boolean wheelKeyHeld(){return key.get()>=0&&Keys.isDown(key.get());}
 public ItemStack wheelItem(int i){return i>=0&&i<choices.length?choices[i].get():ItemStack.EMPTY;}
 public void setWheelItem(int i,ItemStack item){if(i<0||i>=choices.length)return;choices[i].set(item);com.nuxvisuals.config.Config.save();}
 public void swapWheelItems(int a,int b){if(a<0||b<0||a>=choices.length||b>=choices.length||a==b)return;
  ItemStack first=choices[a].get().copy(),second=choices[b].get().copy();choices[a].set(second);choices[b].set(first);com.nuxvisuals.config.Config.save();}
 public void openConfiguration(){var c=mc();if(c.player==null){message("§eСначала зайди в мир");return;}c.setScreen(new AutoSwapConfigScreen(c.currentScreen,this));}
 public int available(int index){var c=mc();if(c.player==null||index<0||index>=choices.length)return 0;
  int n=0;for(int i=0;i<36;i++){ItemStack s=c.player.getInventory().getStack(i);if(choices[index].matches(s))n+=s.getCount();}
  if(choices[index].matches(c.player.getOffHandStack()))n+=c.player.getOffHandStack().getCount();return n;}
 public void chooseWheelItem(int index){var c=mc();if(index<0||index>=wheelSlots()||!allowed(c)||stage>=0)return;
  ItemStack pattern=wheelItem(index);if(pattern.isEmpty()){message("§eСначала назначь предмет этому сектору");return;}
  if(choices[index].matches(c.player.getOffHandStack())){message("§eПредмет уже во второй руке");return;}
  int slot=InvUtil.findInventory(choices[index]::matches);if(slot<0){message("§cЭтого предмета нет в инвентаре");return;}
  start(c,slot,pattern.getName().getString(),index,true);
 }
 private static String name(ItemStack s){return s.getName().getString().replaceAll("§.","").toLowerCase(Locale.ROOT);}
 private static boolean talisman(ItemStack s){String n=name(s);return n.contains("талисман")||n.contains("амулет");}
 private static boolean fits(String mode,ItemStack s){if(s.isEmpty())return false;String n=name(s);return switch(mode){
  case "Ball"->s.isOf(Items.PLAYER_HEAD)||n.contains("шар")||n.contains("сфер");case "Talisman"->talisman(s);
  case "Totem"->s.isOf(Items.TOTEM_OF_UNDYING)&&!talisman(s);case "Shield"->s.isOf(Items.SHIELD);
  case "Golden Apple"->s.isOf(Items.GOLDEN_APPLE)||s.isOf(Items.ENCHANTED_GOLDEN_APPLE);default->false;};}
 private static String label(String s){return switch(s){case "Ball"->"шар";case "Talisman"->"талисман";case "Totem"->"тотем";case "Shield"->"щит";default->"яблоко";};}
 private String[] sequence(){return new String[]{first.get(),second.get()};}
 private void message(String s){if(notify.get())InvUtil.actionBar(s);}
 private boolean allowed(MinecraftClient c){return c.player!=null&&c.world!=null&&c.interactionManager!=null&&c.isWindowFocused()
  &&!c.player.isSpectator()&&!c.player.isUsingItem()&&c.player.currentScreenHandler==c.player.playerScreenHandler&&c.player.playerScreenHandler.getCursorStack().isEmpty();}
 public static boolean onScroll(double amount){AutoSwap s=instance;var c=MinecraftClient.getInstance();
  if(s==null||!s.isEnabled()||s.isWheel()||!s.scroll.get()||c.currentScreen!=null||!s.allowed(c)||s.scrollKey.get()<0
   ||!Keys.isDown(s.scrollKey.get())||!Double.isFinite(amount)||amount==0)return false;
  long now=System.nanoTime();if(s.stage>=0||now<s.nextScrollAt)return true;
  s.wheelRemainder=Math.max(-4,Math.min(4,s.wheelRemainder+amount));if(Math.abs(s.wheelRemainder)<1)return true;
  int direction=s.wheelRemainder>0?1:-1;s.wheelRemainder=0;s.nextScrollAt=now+50_000_000L;s.begin(c,s.invert.get()?-direction:direction);return true;}
 private void begin(MinecraftClient c,int direction){if(stage>=0||c.currentScreen!=null||!allowed(c))return;
  String[] seq=sequence();ItemStack off=c.player.getOffHandStack();int cur=-1;
  for(int i=0;i<seq.length;i++)if(fits(seq[i],off)){cur=i;break;}int target=cur;
  for(int attempt=0;attempt<seq.length;attempt++){target=SwapCycle.next(target,seq.length,direction);String want=seq[target];if(fits(want,off))continue;
   int slot=InvUtil.findInventory(stack->fits(want,stack));if(slot<0){if(skip.get())continue;message("§cНет в инвентаре: §f"+label(want));return;}
   start(c,slot,label(want),target,openInventory.get());return;}
  message("§eНет другого предмета для свапа");}
 /** The radial selection uses the same visible inventory flow as the legacy cycle. */
 private void start(MinecraftClient c,int slot,String title,int index,boolean showInventory){
  if(stage>=0||slot<0||slot>=36||c.currentScreen!=null||!allowed(c))return;
  slotToSwap=slot;targetIndex=index;targetName=title;swapWorld=c.world;swapPlayer=c.player;
  expected=c.player.getInventory().getStack(slot).copy();offBefore=c.player.getOffHandStack().copy();
  if(expected.isEmpty()){reset(c);return;}
  stage=0;timer=delay.getInt();settlement=null;
  if(showInventory){ownedScreen=new InventoryScreen(c.player);c.setScreen(ownedScreen);}
 }
 private static boolean unchanged(ItemStack actual,ItemStack snapshot){
  return actual.getCount()==snapshot.getCount()&&ItemStack.areItemsAndComponentsEqual(actual,snapshot);
 }
 /** Ignore tool wear only, never item names, potion effects, enchantments or server data. */
 private static boolean settledStack(ItemStack actual,ItemStack snapshot){
  if(actual.isEmpty()||snapshot.isEmpty())return actual.isEmpty()&&snapshot.isEmpty();
  if(actual.getCount()!=snapshot.getCount()||!actual.isOf(snapshot.getItem()))return false;
  if(ItemStack.areItemsAndComponentsEqual(actual,snapshot))return true;
  ItemStack a=actual.copy(),b=snapshot.copy();
  a.remove(net.minecraft.component.DataComponentTypes.DAMAGE);b.remove(net.minecraft.component.DataComponentTypes.DAMAGE);
  a.remove(net.minecraft.component.DataComponentTypes.REPAIR_COST);b.remove(net.minecraft.component.DataComponentTypes.REPAIR_COST);
  return ItemStack.areItemsAndComponentsEqual(a,b);
 }
 @Override public void onTick(MinecraftClient c){boolean down=wheelKeyHeld(),pressed=down&&!keyWasDown;keyWasDown=down;
  if(c.currentScreen instanceof AutoSwapWheelScreen wheel&&wheel.owner()==this&&(!down||!isWheel()||!allowed(c)))wheel.close();
  if(stage<0){if(pressed&&c.currentScreen==null&&allowed(c)){if(isWheel())c.setScreen(new AutoSwapWheelScreen(this));else begin(c,1);}return;}
  boolean ownScreen=ownedScreen==null?c.currentScreen==null:c.currentScreen==ownedScreen;
  if(!allowed(c)||c.world!=swapWorld||c.player!=swapPlayer||!ownScreen){reset(c);return;}
  if(timer>0){--timer;return;}
  if(stage==0){
   ItemStack actual=c.player.getInventory().getStack(slotToSwap),off=c.player.getOffHandStack();
   if(!unchanged(actual,expected)||!unchanged(off,offBefore)){
    message("§eСвап отменён: инвентарь изменился");reset(c);return;
   }
   // Exactly one ordinary F-swap. Never retry automatically: a second click could undo it.
   SlotLock.runUnlocked(()->c.interactionManager.clickSlot(c.player.playerScreenHandler.syncId,InvUtil.screenSlot(slotToSwap),40,SlotActionType.SWAP,c.player));
   settlement=new SwapSettlement();stage=1;return;
  }
  // Check on the next client tick. A matching predicted swap can finish immediately;
  // mismatches still wait up to the deadline instead of producing a false one-shot error.
  // This is local inventory state, NOT an explicit server acknowledgement.
  boolean swapped=settledStack(c.player.getOffHandStack(),expected)
   &&settledStack(c.player.getInventory().getStack(slotToSwap),offBefore);
  SwapSettlement.Result result=settlement.tick(swapped);
  if(result==SwapSettlement.Result.SETTLED){
   message("§b["+(targetIndex+1)+"/"+(isWheel()?wheelSlots():sequence().length)+"] §f"+targetName);reset(c);
  }else if(result==SwapSettlement.Result.TIMED_OUT){
   message("§eОбмен не завершён. Проверь предметы и ограничения сервера.");reset(c);
  }
 }
 private void reset(MinecraftClient c){
  // Do not close a chest, another screen, a new player's handler, or a cursor-held item.
  if(ownedScreen!=null&&c.currentScreen==ownedScreen&&c.player==swapPlayer&&c.world==swapWorld
   &&c.player!=null&&c.player.currentScreenHandler==c.player.playerScreenHandler&&c.player.playerScreenHandler.getCursorStack().isEmpty())c.player.closeHandledScreen();
  stage=-1;timer=0;slotToSwap=-1;expected=offBefore=ItemStack.EMPTY;ownedScreen=null;swapWorld=null;swapPlayer=null;settlement=null;
 }
 @Override public void onDisable(){var c=mc();if(c.currentScreen instanceof AutoSwapWheelScreen wheel&&wheel.owner()==this)wheel.close();reset(c);wheelRemainder=0;nextScrollAt=0;}
}
