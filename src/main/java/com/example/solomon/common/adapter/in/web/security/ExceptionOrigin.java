package com.example.solomon.common.adapter.in.web.security;

public final class ExceptionOrigin {

    private ExceptionOrigin() {
    }

    public static String classNameOf(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();

        if (stackTrace.length == 0) {
            return throwable.getClass().getName();
        }

        return stackTrace[0].getClassName();
    }

}