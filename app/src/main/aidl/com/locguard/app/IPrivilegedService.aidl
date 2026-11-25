package com.locguard.app;

interface IPrivilegedService {
    void destroy() = 16777114;
    boolean setLocationEnabled(boolean enabled) = 1;
    boolean isLocationEnabled() = 2;
    String getForegroundPackage() = 3;
    boolean grantRuntimePermissions(String packageName) = 4;
    String runCommand(String command) = 5;
}
