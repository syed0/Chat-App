package com.android.tools.p000ir.runtime;

import com.android.tools.p000ir.common.Log;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/* renamed from: com.android.tools.ir.runtime.ApplicationPatch */
public class ApplicationPatch {
    public final byte[] data;
    public final String path;

    public ApplicationPatch(String path2, byte[] data2) {
        this.path = path2;
        this.data = data2;
    }

    public String toString() {
        return "ApplicationPatch{path='" + this.path + '\'' + ", data.length='" + this.data.length + '\'' + '}';
    }

    public static List<ApplicationPatch> read(DataInputStream input) throws IOException {
        int changeCount = input.readInt();
        if (Log.logging != null && Log.logging.isLoggable(Level.FINE)) {
            Log.Logging logging = Log.logging;
            Level level = Level.FINE;
            logging.log(level, "Receiving " + changeCount + " changes");
        }
        List<ApplicationPatch> changes = new ArrayList<>(changeCount);
        for (int i = 0; i < changeCount; i++) {
            String path2 = input.readUTF();
            byte[] bytes = new byte[input.readInt()];
            input.readFully(bytes);
            changes.add(new ApplicationPatch(path2, bytes));
        }
        return changes;
    }

    public String getPath() {
        return this.path;
    }

    public byte[] getBytes() {
        return this.data;
    }
}
