package com.android.tools.p000ir.runtime;

import com.android.tools.p000ir.common.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/* renamed from: com.android.tools.ir.runtime.AbstractPatchesLoaderImpl */
public abstract class AbstractPatchesLoaderImpl implements PatchesLoader {
    private final Method get = AtomicReference.class.getMethod("get", new Class[0]);
    private final Method set = AtomicReference.class.getMethod("set", new Class[]{Object.class});

    public abstract String[] getPatchedClasses();

    public boolean load() {
        Object previous;
        Field isObsolete;
        AbstractPatchesLoaderImpl abstractPatchesLoaderImpl = this;
        String[] patchedClasses = getPatchedClasses();
        int length = patchedClasses.length;
        int i = 0;
        while (i < length) {
            String className = patchedClasses[i];
            try {
                ClassLoader cl = getClass().getClassLoader();
                Object o = cl.loadClass(className + "$override").newInstance();
                Class<?> originalClass = cl.loadClass(className);
                Field changeField = originalClass.getDeclaredField("$change");
                changeField.setAccessible(true);
                if (originalClass.isInterface()) {
                    previous = abstractPatchesLoaderImpl.patchInterface(changeField, o);
                } else {
                    previous = abstractPatchesLoaderImpl.patchClass(changeField, o);
                }
                if (!(previous == null || (isObsolete = previous.getClass().getDeclaredField("$obsolete")) == null)) {
                    isObsolete.set((Object) null, true);
                }
                if (Log.logging != null && Log.logging.isLoggable(Level.FINE)) {
                    Log.logging.log(Level.FINE, String.format("patched %s", new Object[]{className}));
                }
                i++;
                abstractPatchesLoaderImpl = this;
            } catch (Exception e) {
                Exception e2 = e;
                if (Log.logging == null) {
                    return false;
                }
                Log.logging.log(Level.SEVERE, String.format("Exception while patching %s", new Object[]{className}), e2);
                return false;
            }
        }
        return true;
    }

    private Object patchInterface(Field changeField, Object patch) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Object atomicReference = changeField.get((Object) null);
        Object previous = this.get.invoke(atomicReference, new Object[0]);
        this.set.invoke(atomicReference, new Object[]{patch});
        return previous;
    }

    private Object patchClass(Field changeField, Object patch) throws IllegalAccessException {
        Object previous = changeField.get((Object) null);
        changeField.set((Object) null, patch);
        return previous;
    }
}
