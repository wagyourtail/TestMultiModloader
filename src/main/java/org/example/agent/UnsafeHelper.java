package org.example.agent;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;

public class UnsafeHelper {
    protected static final Unsafe UNSAFE = getUnsafe();
    protected static final MethodHandles.Lookup IMPL_LOOKUP = getImplLookup();

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new UnsupportedOperationException("Unable to get Unsafe instance", e);
        }
    }

    private static MethodHandles.Lookup getImplLookup() {
        try {
            // ensure lookup is initialized
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            // get the impl_lookup field
            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Unsafe unsafe = getUnsafe();
            MethodHandles.Lookup IMPL_LOOKUP;
            IMPL_LOOKUP = (MethodHandles.Lookup) unsafe.getObject(MethodHandles.Lookup.class, unsafe.staticFieldOffset(implLookupField));
            if (IMPL_LOOKUP != null) return IMPL_LOOKUP;
            throw new NullPointerException();
        } catch (Throwable e) {
            try {
                // try to create a new lookup
                Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                constructor.setAccessible(true);
                return constructor.newInstance(Object.class, -1);
            } catch (Throwable e2) {
                e.addSuppressed(e2);
            }
            throw new UnsupportedOperationException("Unable to get MethodHandles.Lookup.IMPL_LOOKUP", e);
        }
    }

    private static final MethodHandle getUCP;
    private static final MethodHandle addUrl;

    static {
        try {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            Field ucpField = null;
            try {
                ucpField = systemClassLoader.getClass().getDeclaredField("ucp");
            } catch (NoSuchFieldException ignored) {
            }
            if (ucpField == null) ucpField = systemClassLoader.getClass().getSuperclass().getDeclaredField("ucp");
            getUCP = IMPL_LOOKUP.unreflectGetter(ucpField).asType(MethodType.methodType(Object.class, ClassLoader.class));
            Method addURLMd = ucpField.getType().getDeclaredMethod("addURL", URL.class);
            addUrl = IMPL_LOOKUP.unreflect(addURLMd).asType(MethodType.methodType(void.class, Object.class, URL.class));
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addToSystemClassLoader(URL url) throws Throwable {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        Object urlClassPath = getUCP.invokeExact(systemClassLoader);
        addUrl.invokeExact(urlClassPath, url);
    }



}
