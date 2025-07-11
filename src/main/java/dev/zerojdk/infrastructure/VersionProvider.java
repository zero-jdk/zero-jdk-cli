package dev.zerojdk.infrastructure;

public class VersionProvider {
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }
}
