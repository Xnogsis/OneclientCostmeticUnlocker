package org.polyfrost.polyplus.client.utils;

import java.util.UUID;

public final class ClientPlatform {
    public static final ClientPlatform INSTANCE = new ClientPlatform();
    public UUID uuid;
    public UUID localPlayerUuid() { return uuid; }
}
