package localcosmetics;

import java.lang.reflect.*;
import java.util.*;

/** Minecraft-independent PolyPlus adapter. All overrides live in this JVM only. */
public final class LocalCosmetics {
    private static final String ROOT = "org.polyfrost.polyplus.client.";
    private static final Map<Object,Integer> overrides = new HashMap<>();
    private static Class<?> type(String name) throws Exception {
        return Class.forName(name, true, LocalCosmetics.class.getClassLoader());
    }
    private static Object singleton(String name) throws Exception { return type(name).getField("INSTANCE").get(null); }
    private static Object call(Object receiver, String name, Object... args) throws Exception {
        for (Method method : receiver.getClass().getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                method.setAccessible(true);
                return method.invoke(receiver, args);
            }
        }
        throw new NoSuchMethodException(name);
    }
    private static Field field(Object receiver, String name) throws Exception {
        Field f = receiver.getClass().getDeclaredField(name); f.setAccessible(true); return f;
    }
    public static Object unit() {
        try { return type("kotlin.Unit").getField("INSTANCE").get(null); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static void exposeCatalog(Object catalog) {
        try {
            Set<Integer> cosmetics = new HashSet<>(), emotes = new HashSet<>();
            for (Object definition : (Collection<?>) call(catalog, "allDefinitions")) {
                int id = (Integer) call(definition, "getId");
                if (call(definition, "getType").toString().equalsIgnoreCase("Emote")) emotes.add(id);
                else cosmetics.add(id);
            }
            field(catalog, "ownedCosmeticIds").set(null, cosmetics);
            field(catalog, "ownedEmoteIds").set(null, emotes);
        } catch (Exception e) { throw new IllegalStateException("PolyPlus catalog adapter failed", e); }
    }
    @SuppressWarnings("unchecked")
    public static synchronized Object localEquipped(Object catalog) {
        try {
            Object current = field(catalog, "localEquipped").get(null);
            Map<Object,Integer> equipped = new HashMap<>((Map<Object,Integer>) call(current, "getEquipped"));
            for (Map.Entry<Object,Integer> entry : overrides.entrySet()) {
                if (entry.getValue() == null) equipped.remove(entry.getKey());
                else equipped.put(entry.getKey(), entry.getValue());
            }
            Object result = current.getClass().getConstructor(Map.class).newInstance(equipped);
            field(catalog, "localEquipped").set(null, result);
            return result;
        } catch (Exception e) { throw new IllegalStateException("PolyPlus equipment adapter failed", e); }
    }
    @SuppressWarnings("unchecked")
    public static synchronized Object equip(Object catalog, Object request) {
        try {
            overrides.putAll((Map<Object,Integer>) call(request, "getEquipped"));
            localEquipped(catalog);
            return unit();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static synchronized void reset() { overrides.clear(); }
    public static synchronized boolean overridesPlayer(UUID uuid) {
        if (overrides.isEmpty()) return false;
        try { return uuid.equals(call(singleton(ROOT + "utils.ClientPlatform"), "localPlayerUuid")); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static Object localMap(Object catalog) {
        try { return call(localEquipped(catalog), "getEquipped"); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static boolean localPacket(Object packet) {
        String name = packet.getClass().getSimpleName();
        if (!Arrays.asList("SetEquippedCosmetic", "PlayEmote", "StopEmote", "SetParticleColor").contains(name)) return false;
        try {
            if (name.equals("PlayEmote") || name.equals("StopEmote")) {
                Object uuid = call(singleton(ROOT + "utils.ClientPlatform"), "localPlayerUuid");
                if (uuid != null) {
                    Object sync = singleton(ROOT + "cosmetics.CosmeticSync");
                    if (name.equals("PlayEmote")) call(sync, "handleEmotePlay", uuid.toString(), call(packet, "getEmoteId"));
                    else call(sync, "handleEmoteStop", uuid.toString());
                }
            }
            return true;
        } catch (Exception e) { throw new IllegalStateException("PolyPlus local emote adapter failed", e); }
    }
}
