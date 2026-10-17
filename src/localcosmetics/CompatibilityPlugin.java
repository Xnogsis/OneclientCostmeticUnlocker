package localcosmetics;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.service.MixinService;

/** Check the cosmetic API before applying either mixin, without loading game classes. */
public final class CompatibilityPlugin implements IMixinConfigPlugin {
    private Boolean compatible;
    public interface Bytecode { ClassNode read(String name) throws Exception; }
    private static final String ROOT = "org/polyfrost/polyplus/client/";
    private static final String RESP = ROOT + "network/http/responses/";
    private static final String CAT = ROOT + "cosmetics/CosmeticCatalog";
    private static final String SLOT = "L" + RESP + "BodySlot;";
    private static void method(ClassNode node, String name, String desc) {
        long matches = node.methods.stream().filter(m -> m.name.matches(name) && m.desc.equals(desc)
            && (m.access & Opcodes.ACC_STATIC)==0).count();
        if (matches != 1) throw new IllegalStateException(node.name + ": incompatible method " + name + desc);
        if (name.contains("?:") && node.methods.stream().filter(m -> m.name.matches(name)).count()!=1)
            throw new IllegalStateException(node.name + ": ambiguous hook " + name);
    }
    private static void field(ClassNode node, String name, String desc) {
        if (node.fields.stream().noneMatch(f -> f.name.equals(name) && f.desc.equals(desc)
            && (f.access & Opcodes.ACC_STATIC)!=0)) throw new IllegalStateException(node.name + ": incompatible field " + name);
    }
    public static void validate(Bytecode bytes) throws Exception {
        ClassNode slot = bytes.read(RESP + "BodySlot");
        if (!"java/lang/Enum".equals(slot.superName))
            throw new IllegalStateException("BodySlot must be an enum for saved selections");
        ClassNode catalog = bytes.read(CAT);
        for (String name : List.of("ownedIds", "ownedCosmeticIds", "ownedEmoteIds")) method(catalog,name,"()Ljava/util/Set;");
        method(catalog,"ownedCosmeticGroupIds","()Ljava/util/List;");
        method(catalog,"setSelectedEmote","(Ljava/lang/Integer;)V");
        method(catalog,"localEquipped","()L" + RESP + "EquippedCosmetics;");
        method(catalog,"getRemoteEquipped","(Ljava/util/UUID;)Ljava/util/Map;");
        method(catalog,"getActiveId","(Ljava/util/UUID;" + SLOT + ")Ljava/lang/Integer;");
        method(catalog,"setEquipped(?:-[A-Za-z0-9_-]+)?","(L" + RESP + "PartialEquippedCosmetics;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;");
        method(catalog,"reset","()V"); method(catalog,"allDefinitions","()Ljava/util/Collection;");
        field(catalog,"ownedCosmeticIds","Ljava/util/Set;"); field(catalog,"ownedEmoteIds","Ljava/util/Set;");
        field(catalog,"localEquipped","L" + RESP + "EquippedCosmetics;");
        ClassNode equipped = bytes.read(RESP + "EquippedCosmetics");
        method(equipped,"<init>","(Ljava/util/Map;)V"); method(equipped,"getEquipped","()Ljava/util/Map;");
        method(bytes.read(RESP + "PartialEquippedCosmetics"),"getEquipped","()Ljava/util/Map;");
        ClassNode definition = bytes.read(RESP + "CosmeticDefinition");
        method(definition,"getId","()I"); method(definition,"getType","()L" + RESP + "CosmeticType;");
        ClassNode platform = bytes.read(ROOT + "utils/ClientPlatform");
        field(platform,"INSTANCE","L" + ROOT + "utils/ClientPlatform;");
        method(platform,"localPlayerUuid","()Ljava/util/UUID;");
        ClassNode sync = bytes.read(ROOT + "cosmetics/CosmeticSync");
        field(sync,"INSTANCE","L" + ROOT + "cosmetics/CosmeticSync;");
        method(sync,"handleEmotePlay","(Ljava/lang/String;I)V"); method(sync,"handleEmoteStop","(Ljava/lang/String;)V");
        method(bytes.read(ROOT + "network/websocket/ServerboundPacket$PlayEmote"),"getEmoteId","()I");
        method(bytes.read(ROOT + "network/websocket/PolyConnection"),"sendPacket(?:-[A-Za-z0-9_-]+)?",
            "(L" + ROOT + "network/websocket/ServerboundPacket;)Ljava/lang/Object;");
    }
    @Override public synchronized boolean shouldApplyMixin(String target, String mixin) {
        if (compatible == null) {
            try {
                validate(name -> MixinService.getService().getBytecodeProvider().getClassNode(name.replace('/','.'), false));
                compatible = true;
            } catch (Exception | LinkageError failure) {
                compatible = false;
                System.err.println("[OneClient Local Cosmetics] Disabled: incompatible PolyPlus cosmetic API: " + failure.getMessage());
            }
        }
        return compatible;
    }
}
