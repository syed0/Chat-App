package com.android.tools.p000ir.runtime;

/* renamed from: com.android.tools.ir.runtime.PatchesLoaderDumper */
public class PatchesLoaderDumper {
    public static void main(String[] args) {
        try {
            ((PatchesLoader) Class.forName("com.android.tools.ir.runtime.AppPatchesLoaderImpl").newInstance()).load();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e2) {
            e2.printStackTrace();
        } catch (IllegalAccessException e3) {
            e3.printStackTrace();
        }
    }
}
