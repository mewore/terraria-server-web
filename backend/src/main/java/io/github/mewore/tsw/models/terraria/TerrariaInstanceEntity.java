package io.github.mewore.tsw.models.terraria;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Max;
import javax.validation.constraints.Positive;
import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.file.OperatingSystem;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Setter
@Entity
@Table(name = "terraria_instance", uniqueConstraints = {@UniqueConstraint(columnNames = {"host_id", "uuid"})})
@DynamicUpdate
public class TerrariaInstanceEntity {

    private static final String OUTPUT_FILE_NAME = "output.log";

    private static final String T_MOD_LOADER_SERVER_NAME_UNIX = "tModLoaderServer";

    private static final String T_MOD_LOADER_SERVER_NAME_WINDOWS = "tModLoaderServer.exe";

    @Id
    @GeneratedValue
    private Long id;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private @NonNull UUID uuid = UUID.randomUUID();

    @Column
    private @Nullable String terrariaVersion;

    @Column
    private @Nullable String modLoaderVersion;

    @Column(length = 1023)
    private @Nullable String modLoaderReleaseUrl;

    @Column(length = 1023)
    private @Nullable String modLoaderArchiveUrl;

    @Column(length = 1023)
    private @Nullable String error;

    @Column
    @Enumerated(EnumType.STRING)
    private @Nullable TerrariaInstanceAction pendingAction;

    @Setter(AccessLevel.PRIVATE)
    @Column
    private @Nullable Instant actionExecutionStartTime;

    @Column(nullable = false, length = 1023)
    private @NonNull Path location;

    @Column(nullable = false)
    private @NonNull String name;

    @Column(nullable = false, length = 1023)
    private @NonNull String terrariaServerUrl;

    @Column(nullable = false)
    private @NonNull Long modLoaderReleaseId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private @NonNull TerrariaInstanceState state;

    @JsonIgnore
    @ManyToOne(optional = false)
    private @NonNull HostEntity host;

    @Setter(AccessLevel.PRIVATE)
    @Column
    @Enumerated(EnumType.STRING)
    private @Nullable TerrariaInstanceAction currentAction;

    @JsonIgnore
    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private @NonNull Long nextOutputBytePosition = 0L;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("8")
    private @Positive @Max(1000) @NonNull Integer maxPlayers = 8;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("7777")
    private @Positive @NonNull Integer port = 7777;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("false")
    private @NonNull Boolean automaticallyForwardPort = true;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("''")
    private @NonNull String password = "";

    /**
     * The detected numerical options, entering which should be possible at the next prompt.
     */
    @Setter(AccessLevel.PRIVATE)
    @Builder.Default
    @JsonIgnore
    @Column(nullable = false)
    @ColumnDefault("'{}'")
    private @NonNull Map<Integer, String> pendingOptions = Collections.emptyMap();

    /**
     * The currently known numerical options that can be entered, along with their corresponding text. These can be
     * world names or mods.
     */
    @Setter(AccessLevel.PRIVATE)
    @Builder.Default
    @JsonIgnore
    @Column(nullable = false)
    @ColumnDefault("'{}'")
    private @NonNull Map<Integer, String> options = Collections.emptyMap();

    @Builder.Default
    @JsonIgnore
    @Column(nullable = false)
    @ColumnDefault("'[]'")
    private @NonNull Set<String> modsToEnable = Collections.emptySet();

    /**
     * The mods that are currently loaded in this instance if it is running.
     */
    @Builder.Default
    @JsonIgnore
    @Column(nullable = false)
    @ColumnDefault("'[]'")
    private @NonNull Set<String> loadedMods = Collections.emptySet();

    @JsonIgnore
    @OneToOne
    private @Nullable TerrariaWorldEntity world;

    @JsonIgnore
    public File getOutputFile() {
        return getLocation().resolve(OUTPUT_FILE_NAME).toFile();
    }

    @JsonIgnore
    public File getModLoaderServerFile() {
        return getLocation().resolve(getHost().getOs() == OperatingSystem.WINDOWS
                ? T_MOD_LOADER_SERVER_NAME_WINDOWS
                : T_MOD_LOADER_SERVER_NAME_UNIX).toFile();
    }

    public void setState(final TerrariaInstanceState newState) {
        if (newState == state) {
            return;
        }
        state = newState;
        options = pendingOptions;
        pendingOptions = Collections.emptyMap();
        if (!newState.isActive()) {
            options = Collections.emptyMap();
            loadedMods = Collections.emptySet();
            world = null;
        }
    }

    public void startAction() {
        currentAction = pendingAction;
        pendingAction = null;
        actionExecutionStartTime = Instant.now();
    }

    public void completeAction() {
        currentAction = null;
        actionExecutionStartTime = null;
    }

    public void acknowledgeMenuOption(final int key, final String value) {
        final Map<Integer, String> newPendingOptions = new HashMap<>(pendingOptions);
        newPendingOptions.put(key, value);
        pendingOptions = Collections.unmodifiableMap(newPendingOptions);
    }
}
