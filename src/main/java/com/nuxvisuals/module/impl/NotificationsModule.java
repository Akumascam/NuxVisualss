package com.nuxvisuals.module.impl;

import com.nuxvisuals.module.Category;
import com.nuxvisuals.module.Module;

// Уведомления о включении/выключении модулей (рисует hud/Notifications)
public class NotificationsModule extends Module {
    public NotificationsModule() {
        super("alerts", "Notifications", Category.Hud, true);
    }
}
