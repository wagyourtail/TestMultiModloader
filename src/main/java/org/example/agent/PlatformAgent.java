package org.example.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class PlatformAgent {

    static final String platformName = System.getProperty("platform.agent");

    public static void premain(String args, Instrumentation inst) throws UnmodifiableClassException, ClassNotFoundException {
        agentmain(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) throws ClassNotFoundException, UnmodifiableClassException {
        inst.addTransformer(new PlatformTransformer());
        if (!inst.isRedefineClassesSupported()) {
            System.err.println("Retransform classes not supported");
            System.exit(1);
        }
        inst.retransformClasses(Class.forName("org.spongepowered.asm.mixin.transformer.MixinInfo"));
    }

}
