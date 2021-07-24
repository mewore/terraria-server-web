package io.github.mewore.tsw.models.terraria;

import java.nio.file.Path;
import java.util.UUID;

import io.github.mewore.tsw.models.HostEntity;

import static org.mockito.Mockito.mock;

public class TerrariaInstanceFactory {

    public static final long INSTANCE_ID = 1;

    public static final UUID INSTANCE_UUID = UUID.fromString("aaa24aaa-e6e4-4f8a-982b-004cbb04e505");

    public static TerrariaInstanceEntity makeInstanceWithState(final TerrariaInstanceState state) {
        return makeInstanceBuilder().state(state).build();
    }

    public static TerrariaInstanceEntity makeInstanceWithId(final long id) {
        return makeInstanceBuilder().id(id).build();
    }

    public static TerrariaInstanceEntity makeInstance() {
        return makeInstanceBuilder().build();
    }

    public static TerrariaInstanceEntity.TerrariaInstanceEntityBuilder makeInstanceBuilder() {
        return TerrariaInstanceEntity.builder()
                .id(INSTANCE_ID)
                .uuid(INSTANCE_UUID)
                .location(Path.of("instance-dir"))
                .name("Instance Name")
                .terrariaServerUrl("server-url")
                .modLoaderReleaseId(1L)
                .state(TerrariaInstanceState.IDLE)
                .host(mock(HostEntity.class));
    }
}
