package io.github.mewore.tsw.models.terraria;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Max;
import javax.validation.constraints.Positive;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.file.OperatingSystem;
import io.github.mewore.tsw.models.terraria.world.TerrariaWorldEntity;
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
public class TerrariaInstanceEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String OUTPUT_FILE_NAME = "output.log";

    private static final String T_MOD_LOADER_SERVER_NAME_UNIX = "tModLoaderServer";

    private static final String T_MOD_LOADER_SERVER_NAME_WINDOWS = "tModLoaderServer.exe";

    @Setter(AccessLevel.NONE)
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
    private @NonNull String location;

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

    @Builder.Default
    @JsonIgnore
    @Column(nullable = false)
    @ColumnDefault("0")
    private @NonNull Long nextOutputBytePosition = 0L;

    @Builder.Default
    @JsonIgnore
    @Column(nullable = false)
    @ColumnDefault("8")
    private @Positive @Max(1000) @NonNull Integer maxPlayers = 8;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("7777")
    private @Positive @NonNull Integer port = 7777;

    @Builder.Default
    @JsonIgnore
    @Column(nullable = false)
    @ColumnDefault("false")
    private @NonNull Boolean automaticallyForwardPort = true;

    @Builder.Default
    @JsonIgnore
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
    @Column(nullable = false)
    @ColumnDefault("'[]'")
    private @NonNull Set<String> loadedMods = Collections.emptySet();

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    private @Nullable TerrariaWorldEntity world;

    public Path getLocation() {
        return Path.of(location);
    }

    public void setLocation(final Path newLocation) {
        location = newLocation.toAbsolutePath().toString();
    }

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

    public int getOptionKey(final String optionLabel) {
        return options.entrySet()
                .stream()
                .filter(option -> option.getValue().equals(optionLabel))
                .findAny()
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("There is no option \"%s\" in the known options:%n%s", optionLabel,
                                options.entrySet()
                                        .stream()
                                        .sorted(Comparator.comparingInt(Map.Entry::getKey))
                                        .map(option -> option.getKey() + "\t\t" + option.getValue())
                                        .collect(Collectors.joining("\n")))));
    }

    public void setState(final TerrariaInstanceState newState) {
        if (newState == getState()) {
            return;
        }
        state = newState;
        setOptions(getPendingOptions());
        setPendingOptions(Collections.emptyMap());
        if (!newState.isActive()) {
            setOptions(Collections.emptyMap());
            setLoadedMods(Collections.emptySet());
            setWorld(null);
        }
    }

    public void startAction() {
        setCurrentAction(getPendingAction());
        setPendingAction(null);
        setActionExecutionStartTime(Instant.now());
    }

    public void completeAction() {
        setCurrentAction(null);
        setActionExecutionStartTime(null);
    }

    public void acknowledgeMenuOption(final int key, final String value) {
        final Map<Integer, String> newPendingOptions = new HashMap<>(getPendingOptions());
        newPendingOptions.put(key, value);
        setPendingOptions(Collections.unmodifiableMap(newPendingOptions));
    }

    @SuppressWarnings("unused")
    public @Nullable Long getWorldId() {
        final @Nullable TerrariaWorldEntity world = getWorld();
        return world == null ? null : world.getId();
    }
}
