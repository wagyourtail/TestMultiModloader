package org.example.agent;

import com.github.difflib.patch.PatchFailedException;
import io.github.prcraftmc.classdiff.ClassPatcher;
import io.github.prcraftmc.classdiff.format.DiffReader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PlatformTransformer implements ClassFileTransformer {
    // modify Lorg/spongepowered/asm/mixin/transformer/MixinInfo;loadMixinClass(Ljava/lang/String;)Lorg/objectweb/asm/tree/ClassNode;
    // to call Lorg/example/agent/PlatformTransformer;transform(Lorg/objectweb/asm/tree/ClassNode;Ljava/lang/String;)Lorg/objectweb/asm/tree/ClassNode;
    // before return

    public static ClassNode transformMixinInfo(ClassNode clazz) throws IOException {
        for (MethodNode method : clazz.methods) {
            if (method.name.equals("loadMixinClass") && method.desc.equals("(Ljava/lang/String;)Lorg/objectweb/asm/tree/ClassNode;")) {
                // find areturn
                AbstractInsnNode prev = null;
                for (AbstractInsnNode node : method.instructions) {
                    if (node.getOpcode() == Opcodes.ARETURN) {
                        // insert before return
                        InsnList insnList = new InsnList();
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/example/agent/PlatformTransformer", "transform", "(Lorg/objectweb/asm/tree/ClassNode;Ljava/lang/String;)Lorg/objectweb/asm/tree/ClassNode;", false));


                        method.instructions.insertBefore(node, insnList);
                        // insert before areturn

                        //result
                        // aload 2
                        // aload 1
                        // invokestatic

                        break;
                    }
                    prev = node;
                }

                // write class
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                clazz.accept(classWriter);
                Files.newOutputStream(Path.of("testout.class"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).write(classWriter.toByteArray());
                return clazz;
            }
        }

        return clazz;
    }


    private static Map<String, byte[]> transformList = null;

    private static Map<String, byte[]> getTransformList() {
        System.out.println("Loading " + PlatformAgent.platformName + " patches");
        try (ZipInputStream zis = new ZipInputStream(PlatformTransformer.class.getResourceAsStream("/" + PlatformAgent.platformName + ".patches.zip"))) {

            Map<String, byte[]> transformList = new HashMap<>();
            transformList.put("org/spongepowered/asm/mixin/transformer/MixinInfo", new byte[0]);

            for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                if (entry.getName().endsWith(".class.cdiff")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    zis.transferTo(baos);
                    transformList.put(entry.getName().substring(0, entry.getName().length() - 12), baos.toByteArray());
                }
                zis.closeEntry();
            }

            return transformList;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (transformList == null) {
            synchronized (PlatformTransformer.class) {
                if (transformList == null) {
                    transformList = getTransformList();
                }
            }
        }
        if (!transformList.containsKey(className)) {
            return null;
        }
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(classfileBuffer);
        int writerArgs = 0;

        classReader.accept(classNode, 0);
        if (className.equals("org/spongepowered/asm/mixin/transformer/MixinInfo")) {
            try {
                classNode = transformMixinInfo(classNode);
                writerArgs = ClassWriter.COMPUTE_FRAMES;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            classNode = transform(classNode, className);
        }

        if (classNode == null) {
            return null;
        }

        ClassWriter classWriter = new ClassWriter(writerArgs);
        classNode.accept(classWriter);
        classfileBuffer = classWriter.toByteArray();

        return classfileBuffer;
    }

    public static DiffReader getPatch(String className) {
        System.out.println(className);
        if (transformList == null) {
            synchronized (PlatformTransformer.class) {
                if (transformList == null) {
                    transformList = getTransformList();
                }
            }
        }
        if (!transformList.containsKey(className)) {
            return null;
        }
        return new DiffReader(transformList.get(className));
    }

    public static ClassNode transform(ClassNode classNode, String className) {
        try {
            DiffReader dr = getPatch(className.replace(".", "/"));
            if (dr == null) {
                return classNode;
            }
            ClassPatcher.patch(classNode, dr);
        } catch (Throwable e) {
            e.printStackTrace(System.out);
            System.out.flush();
        }
        return classNode;
    }
}
