package com.nuxvisuals.module.setting;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
public final class ActionSetting extends Setting {
 private final Runnable action;
 public ActionSetting(String name,Runnable action){super(name);this.action=action;}
 public void activate(){action.run();}
 @Override public JsonElement save(){return new JsonPrimitive(false);}
 @Override public void load(JsonElement element){}
}
