package org.example.client.controllers.admin;

import org.example.common.Message;

public interface AdminChildController {
    void setup(AdminDashboardController dashboardController);
    void handleServerMessage(Message message);
}
