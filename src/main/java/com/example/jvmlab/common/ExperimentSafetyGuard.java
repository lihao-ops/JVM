package com.example.jvmlab.common;

public final class ExperimentSafetyGuard {
    private ExperimentSafetyGuard() {}
    public static boolean isDangerEnabled() {
        String sys = System.getProperty("jvm.lab.enableDanger", "false");
        String env = System.getenv("JVM_LAB_ENABLE_DANGER");
        String val = env != null ? env : sys;
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }
    public static void assertEnabled() {
        if (!isDangerEnabled()) {
            throw new IllegalStateException("Dangerous experiment disabled. Set JVM_LAB_ENABLE_DANGER=true or -Djvm.lab.enableDanger=true");
        }
    }
}
