import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.selectors.*;

/** Validate the compiled injection descriptors against the actual installed bytecode. */
public class VerifyHooks {
    static ClassNode read(JarFile jar, String path) throws Exception {
        var entry = jar.getJarEntry(path + ".class");
        if (entry == null) throw new AssertionError("Missing class: " + path);
        ClassNode node = new ClassNode();
        new ClassReader(jar.getInputStream(entry)).accept(node, 0);
        return node;
    }
    static Object value(AnnotationNode annotation, String key) {
        for (int i=0;i<annotation.values.size();i+=2)
            if (annotation.values.get(i).equals(key)) return annotation.values.get(i+1);
        return null;
    }
    static void has(ClassNode cls, String name) {
        if (cls.methods.stream().noneMatch(m -> m.name.equals(name))) throw new AssertionError(cls.name + ": " + name);
    }
    public static void main(String[] args) throws Exception {
        try (JarFile mod = new JarFile(args[0])) {
            for (int j=1;j<args.length;j++) try (JarFile original = new JarFile(args[j])) {
                int checked = 0;
                for (String mixin : List.of("CatalogMixin", "ConnectionMixin")) {
                    ClassNode mix = read(mod, "localcosmetics/mixin/" + mixin);
                    AnnotationNode annotation = mix.invisibleAnnotations.stream().filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
                    String targetName = ((List<String>) value(annotation, "targets")).get(0);
                    ClassNode target = read(original, targetName.replace('.', '/'));
                    for (MethodNode handler : mix.methods) {
                        if (handler.visibleAnnotations == null) continue;
                        for (AnnotationNode injection : handler.visibleAnnotations) {
                            if (!injection.desc.endsWith("/Inject;")) continue;
                            for (String name : (List<String>) value(injection, "method")) {
                                ITargetSelector selector = TargetSelector.parseAndValidate(name, null);
                                List<MethodNode> candidates = target.methods.stream().filter(m -> selector.match(ElementNode.of(target, m)).isMatch()).toList();
                                if (candidates.size()!=1) throw new AssertionError("Ambiguous/missing hook: " + name);
                                MethodNode method = candidates.get(0);
                                Type[] hargs = Type.getArgumentTypes(handler.desc), targs = Type.getArgumentTypes(method.desc);
                                String cb = Type.getReturnType(method.desc).getSort()==Type.VOID ? "CallbackInfo" : "CallbackInfoReturnable";
                                if (!hargs[hargs.length-1].getClassName().endsWith('.'+cb)) throw new AssertionError("Wrong callback for " + name);
                                if (hargs.length != 1 && hargs.length != targs.length+1) throw new AssertionError("Wrong arity: " + name);
                                if (hargs.length>1) for (int p=0;p<targs.length;p++) {
                                    if (hargs[p].equals(targs[p])) continue;
                                    List<AnnotationNode> pa = handler.invisibleParameterAnnotations == null ? null : handler.invisibleParameterAnnotations[p];
                                    if (pa==null || pa.stream().noneMatch(a -> a.desc.endsWith("/Coerce;"))) throw new AssertionError("Missing Coerce: " + name);
                                    if (!hargs[p].equals(Type.getType(Object.class))) throw new AssertionError("Invalid coercion: " + name);
                                }
                                checked++;
                            }
                        }
                    }
                }
                ClassNode catalog = read(original, "org/polyfrost/polyplus/client/cosmetics/CosmeticCatalog");
                for (String f : List.of("ownedCosmeticIds","ownedEmoteIds","localEquipped"))
                    if (catalog.fields.stream().noneMatch(x -> x.name.equals(f) && (x.access & Opcodes.ACC_STATIC)!=0)) throw new AssertionError("Missing static field " + f);
                has(catalog,"allDefinitions");
                has(read(original,"org/polyfrost/polyplus/client/network/http/responses/CosmeticDefinition"),"getId");
                has(read(original,"org/polyfrost/polyplus/client/network/http/responses/CosmeticDefinition"),"getType");
                has(read(original,"org/polyfrost/polyplus/client/network/http/responses/EquippedCosmetics"),"<init>");
                has(read(original,"org/polyfrost/polyplus/client/network/http/responses/PartialEquippedCosmetics"),"getEquipped");
                has(read(original,"org/polyfrost/polyplus/client/network/websocket/ServerboundPacket$PlayEmote"),"getEmoteId");
                ClassNode sync = read(original,"org/polyfrost/polyplus/client/cosmetics/CosmeticSync");
                has(sync,"handleEmotePlay"); has(sync,"handleEmoteStop");
                System.out.println("PASS " + args[j] + " (" + checked + " injection targets, callback types, coercions, adapter members)");
                if (checked!=11) throw new AssertionError("Unexpected hook count: " + checked);
            }
        }
    }
}
