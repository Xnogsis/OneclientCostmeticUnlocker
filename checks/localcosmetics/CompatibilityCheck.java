package localcosmetics;

import java.io.*;
import java.util.zip.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public final class CompatibilityCheck {
    public static void main(String[] args) throws Exception {
        for (String path : args) {
            try (ZipFile jar = new ZipFile(path)) {
                CompatibilityPlugin.Bytecode bytes = name -> {
                    ZipEntry entry = jar.getEntry(name + ".class");
                    if (entry == null) throw new IOException("Missing class: " + name);
                    try (InputStream input = jar.getInputStream(entry)) {
                        ClassNode node = new ClassNode();
                        new ClassReader(input).accept(node, ClassReader.SKIP_CODE);
                        return node;
                    }
                };
                CompatibilityPlugin.validate(bytes);
                try {
                    CompatibilityPlugin.validate(name -> {
                        ClassNode node = bytes.read(name);
                        if (name.endsWith("/BodySlot")) node.superName = "java/lang/Object";
                        return node;
                    });
                    throw new AssertionError("Non-enum slots accepted");
                } catch (IllegalStateException expected) {
                    assert expected.getMessage().contains("must be an enum");
                }
                System.out.println("PASS: compatible PolyPlus API and incompatible slot rejection: " + path);
            }
        }
    }
}
