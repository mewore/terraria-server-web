package io.github.mewore.tsw.repositories.terraria;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaWorldEntity;

@Transactional
public interface TerrariaWorldRepository extends JpaRepository<TerrariaWorldEntity, Long> {

    List<TerrariaWorldEntity> findByHost(final HostEntity host);

    @Transactional
    default void setHostWorlds(final HostEntity host, final List<TerrariaWorldEntity> newWorlds) {

        final Map<String, TerrariaWorldEntity> currentWorlds = findByHost(host)
                .stream()
                .collect(Collectors.toUnmodifiableMap(TerrariaWorldEntity::getName, Function.identity()));

        final List<TerrariaWorldEntity> worldsToSave = newWorlds.stream().map(world -> {
            final TerrariaWorldEntity existingWorld = currentWorlds.get(world.getName());
            return existingWorld == null ? world : world.withId(existingWorld.getId());
        }).collect(Collectors.toList());

        final Set<String> newWorldNames =
                newWorlds.stream().map(TerrariaWorldEntity::getName).collect(Collectors.toUnmodifiableSet());

        deleteAll(currentWorlds
                .values()
                .stream()
                .filter(world -> !newWorldNames.contains(world.getName()))
                .collect(Collectors.toList()));
        saveAll(worldsToSave);
    }
}
