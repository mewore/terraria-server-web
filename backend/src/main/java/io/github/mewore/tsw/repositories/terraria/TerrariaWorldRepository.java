package io.github.mewore.tsw.repositories.terraria;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;

@Transactional
public interface TerrariaWorldRepository extends JpaRepository<TerrariaWorldEntity, Long> {

    List<TerrariaWorldEntity> findByHost(final HostEntity host);

    @Query("SELECT w FROM TerrariaWorldEntity w JOIN FETCH w.data d WHERE w.host = :host")
    List<TerrariaWorldEntity> findByHostWithData(final HostEntity host);

    List<TerrariaWorldEntity> findByHostIdOrderByIdAsc(final long hostId);

    @Transactional
    default void setHostWorlds(final HostEntity host, final List<TerrariaWorldEntity> newWorlds) {
        final Map<String, TerrariaWorldEntity> currentWorlds = findByHostWithData(host).stream()
                .collect(Collectors.toUnmodifiableMap(TerrariaWorldEntity::getName, Function.identity()));

        final List<TerrariaWorldEntity> worldsToSave = new ArrayList<>();

        for (final TerrariaWorldEntity newWorld : newWorlds) {
            final @Nullable TerrariaWorldEntity currentWorld = currentWorlds.get(newWorld.getName());
            if (currentWorld == null) {
                worldsToSave.add(newWorld);
                continue;
            }
            final boolean dataIsChanged = !Arrays.equals(currentWorld.getData().getContent(),
                    newWorld.getData().getContent());
            if (currentWorld.getLastModified().equals(newWorld.getLastModified()) && !dataIsChanged) {
                continue;
            }
            if (dataIsChanged) {
                currentWorld.setData(newWorld.getData());
                currentWorld.setMods(null);
            }
            currentWorld.setLastModified(newWorld.getLastModified());
            worldsToSave.add(currentWorld);
        }

        final Set<String> newWorldNames = newWorlds.stream()
                .map(TerrariaWorldEntity::getName)
                .collect(Collectors.toUnmodifiableSet());

        deleteAll(currentWorlds.values()
                .stream()
                .filter(world -> !newWorldNames.contains(world.getName()))
                .collect(Collectors.toList()));
        saveAll(worldsToSave);
    }
}
