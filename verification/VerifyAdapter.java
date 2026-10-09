import java.lang.reflect.*;
import java.util.*;
import localcosmetics.LocalCosmetics;

public class VerifyAdapter {
    static final String BASE = "org.polyfrost.polyplus.client.";
    static Class<?> t(String n) throws Exception { return Class.forName(BASE+n); }
    static Field f(Class<?> c, String name) throws Exception { var f=c.getDeclaredField(name);f.setAccessible(true);return f; }
    static void check(boolean v,String message) { if(!v) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Class<?> catalogClass=t("cosmetics.CosmeticCatalog");
        Object catalog=catalogClass.getField("INSTANCE").get(null);
        Class<?> type=t("network.http.responses.CosmeticType"), slot=t("network.http.responses.BodySlot");
        Object cape=type.getField("Cape").get(null), emote=type.getField("Emote").get(null);
        Object bodySlot=slot.getEnumConstants()[0];
        Constructor<?> ctor=Arrays.stream(t("network.http.responses.CosmeticDefinition").getConstructors()).filter(c -> c.getParameterCount()==10).findFirst().orElseThrow();
        Object definition=ctor.newInstance(101,cape,"fixture cape","https://example.invalid/asset","",List.of(bodySlot),101,"fixture","default",null);
        Object emoteDefinition=ctor.newInstance(202,emote,"fixture emote","https://example.invalid/asset","",List.of(),202,"fixture","default",null);
        ((Map)f(catalogClass,"cosmeticDefinitions").get(null)).put(101,definition);
        ((Map)f(catalogClass,"emoteDefinitions").get(null)).put(202,emoteDefinition);
        LocalCosmetics.exposeCatalog(catalog);
        check(((Set)f(catalogClass,"ownedCosmeticIds").get(null)).contains(101),"cape ownership");
        check(((Set)f(catalogClass,"ownedEmoteIds").get(null)).contains(202),"emote ownership");
        Class<?> partial=t("network.http.responses.PartialEquippedCosmetics");
        LocalCosmetics.equip(catalog,partial.getConstructor(Map.class).newInstance(Map.of(bodySlot,101)));
        check(((Map)LocalCosmetics.localMap(catalog)).get(bodySlot).equals(101),"local equip");
        f(catalogClass,"localEquipped").set(null,t("network.http.responses.EquippedCosmetics").getConstructor(Map.class).newInstance(Map.of()));
        check(((Map)LocalCosmetics.localMap(catalog)).get(bodySlot).equals(101),"override survives server refresh");
        Map<Object,Integer> clear=new HashMap<>();clear.put(bodySlot,null);
        LocalCosmetics.equip(catalog,partial.getConstructor(Map.class).newInstance(clear));
        check(!((Map)LocalCosmetics.localMap(catalog)).containsKey(bodySlot),"local unequip");
        LocalCosmetics.reset();
        check(!LocalCosmetics.localPacket(new Object()),"unrelated packet untouched");
        System.out.println("PASS actual PolyPlus adapter: catalog ownership, equip, refresh retention, unequip, reset, packet filtering; no network requests");
    }
}
