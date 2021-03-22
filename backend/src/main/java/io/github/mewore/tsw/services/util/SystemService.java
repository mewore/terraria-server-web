package io.github.mewore.tsw.services.util;

import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.file.OperatingSystem;
import lombok.NonNull;

@Service
public class SystemService {

    private static final String OS_STRING = System.getProperty("os.name").toLowerCase();

    private static final OperatingSystem OPERATING_SYSTEM;

    static {
        if (OS_STRING.contains("win")) {
            OPERATING_SYSTEM = OperatingSystem.WINDOWS;
        } else if (OS_STRING.contains("mac")) {
            OPERATING_SYSTEM = OperatingSystem.MAC;
        } else if (OS_STRING.contains("nix") || OS_STRING.contains("nux") || OS_STRING.contains("aix")) {
            OPERATING_SYSTEM = OperatingSystem.LINUX;
        } else if (OS_STRING.contains("sunos")) {
            OPERATING_SYSTEM = OperatingSystem.SOLARIS;
        } else {
            OPERATING_SYSTEM = OperatingSystem.UNKNOWN;
        }
    }

    public @NonNull OperatingSystem getOs() {
        return OPERATING_SYSTEM;
    }
}
