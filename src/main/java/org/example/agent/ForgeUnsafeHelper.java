package org.example.agent;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ForgeUnsafeHelper extends UnsafeHelper {

    private static final Class<?> moduleClassLoader;

    static {
        try {
            moduleClassLoader = Class.forName("cpw.mods.cl.ModuleClassLoader");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandle getFallbackClassLoader;;
    private static final MethodHandle setFallbackClassLoader;

    static {
        try {
            getFallbackClassLoader = IMPL_LOOKUP.findGetter(moduleClassLoader, "fallbackClassLoader", ClassLoader.class);
            setFallbackClassLoader = IMPL_LOOKUP.findSetter(moduleClassLoader, "fallbackClassLoader", ClassLoader.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void addToFallbackClassloader(Path path) throws Throwable {
        UnsafeFallbackClassLoader u = makeFallbackClassloader();
        u.addURL(path.toUri().toURL());
    }

    public static UnsafeFallbackClassLoader makeFallbackClassloader() throws Throwable {
        List<ClassLoader> loaders = new ArrayList<>();
        ClassLoader l = ForgeUnsafeHelper.class.getClassLoader();
        if (!moduleClassLoader.isAssignableFrom(l.getClass())) {
            return new UnsafeFallbackClassLoader(l);
        }
        while (l != null && moduleClassLoader.isAssignableFrom(l.getClass())) {
            loaders.add(l);
            l = (ClassLoader) getFallbackClassLoader.invoke(l);
        }
        if (l != null && l instanceof UnsafeFallbackClassLoader) {
            return (UnsafeFallbackClassLoader) l;
        }
        UnsafeFallbackClassLoader u = new UnsafeFallbackClassLoader(l);
        setFallbackClassLoader.invoke(loaders.getLast(), (ClassLoader) u);
        return u;
    }

    private static final MethodHandle implAddReadsAllUnnamed;
    static {
        try {
            implAddReadsAllUnnamed = IMPL_LOOKUP.findVirtual(Module.class, "implAddReadsAllUnnamed", MethodType.methodType(void.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void addReadsAllUnnamed(Module module) throws Throwable {
        implAddReadsAllUnnamed.invokeExact(module);
    }

    public static class UnsafeFallbackClassLoader extends URLClassLoader {
        public UnsafeFallbackClassLoader(ClassLoader parent) {
            super(new URL[0], parent);
        }

        @Override
        public void addURL(URL url) {
            super.addURL(url);
        }

    }

}