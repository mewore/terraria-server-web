package io.github.mewore.tsw.models;

import java.time.Duration;
import java.util.UUID;

import io.github.mewore.tsw.models.file.OperatingSystem;

public class HostFactory {

    public static final UUID HOST_UUID = UUID.fromString("e0f245dc-e6e4-4f8a-982b-004cbb04e505");

    public static final String HOST_URL = "host-url";

    public static HostEntity makeHost() {
        return makeHostBuilder().build();
    }

    public static HostEntity.HostEntityBuilder makeHostBuilder() {
        return HostEntity.builder()
                .uuid(HOST_UUID)
                .url(HOST_URL)
                .alive(true)
                .heartbeatDuration(Duration.ZERO)
                .os(OperatingSystem.LINUX);
    }

}
