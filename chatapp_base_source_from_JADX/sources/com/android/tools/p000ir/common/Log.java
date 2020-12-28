package com.android.tools.p000ir.common;

import java.util.logging.Level;

/* renamed from: com.android.tools.ir.common.Log */
public class Log {
    public static Logging logging = null;

    /* renamed from: com.android.tools.ir.common.Log$Logging */
    public interface Logging {
        boolean isLoggable(Level level);

        void log(Level level, String str);

        void log(Level level, String str, Throwable th);
    }
}
