package com.locguard.app;

interface IPrivilegedService {
    void destroy() = 16777114;
    boolean setLocationEnabled(boolean enabled);
    boolean isLocationEnabled();
    String getForegroundPackage();
    boolean grantRuntimePermissions(String packageName);
    String runCommand(String command);
}
