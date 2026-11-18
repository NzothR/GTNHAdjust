package com.gtnhadjust.machine.proxy;

/** Shared locator inventory and operating modes for both proxy controllers. */
public interface ProxyLocatorHost {

    boolean canEditLocators();

    ProxyLocatorInventory getLocatorInventory();

    ProxyLocatorPool getLocatorPool();

    void onLocatorInventoryChanged();

    int getOperationMode();

    void setClientOperationMode(int mode);

    void cycleOperationMode();
}
