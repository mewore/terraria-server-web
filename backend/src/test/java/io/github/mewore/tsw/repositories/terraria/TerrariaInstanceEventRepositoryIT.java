package io.github.mewore.tsw.repositories.terraria;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEventType;
import io.github.mewore.tsw.repositories.HostRepository;

import static io.github.mewore.tsw.models.HostFactory.makeHost;
import static io.github.mewore.tsw.models.terraria.TerrariaInstanceFactory.makeInstanceBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class TerrariaInstanceEventRepositoryIT {

    @Autowired
    private TerrariaInstanceEventRepository terrariaInstanceEventRepository;

    @Autowired
    private TerrariaInstanceRepository terrariaInstanceRepository;

    @Autowired
    private HostRepository hostRepository;

    @Test
    void deleteByInstance() {
        final HostEntity host = hostRepository.save(makeHost());
        final TerrariaInstanceEntity instance = terrariaInstanceRepository.save(
                makeInstanceBuilder().host(host).build());
        terrariaInstanceEventRepository.save(makeEvent(instance));
        terrariaInstanceEventRepository.save(makeEvent(instance));
        terrariaInstanceEventRepository.save(makeEvent(instance));

        assertEquals(3, terrariaInstanceEventRepository.findAll().size());
        assertEquals(3L, terrariaInstanceEventRepository.deleteByInstance(instance));
        assertEquals(0, terrariaInstanceEventRepository.findAll().size());
    }

    @Test
    void findTop100ByInstanceOrderByIdAsc() {
        final HostEntity host = hostRepository.save(makeHost());
        final TerrariaInstanceEntity instance = terrariaInstanceRepository.save(
                makeInstanceBuilder().host(host).build());
        final List<TerrariaInstanceEventEntity> events = terrariaInstanceEventRepository.saveAll(
                List.of(makeEvent(instance), makeEvent(instance), makeEvent(instance)));

        final List<TerrariaInstanceEventEntity> loadedEvents =
                terrariaInstanceEventRepository.findTop100ByInstanceOrderByIdDesc(
                instance);

        assertEquals(3, loadedEvents.size());
        assertEquals(List.of(events.get(2).getId(), events.get(1).getId(), events.get(0).getId()),
                List.of(loadedEvents.get(0).getId(), loadedEvents.get(1).getId(), loadedEvents.get(2).getId()));
    }

    private TerrariaInstanceEventEntity makeEvent(final TerrariaInstanceEntity instance) {
        return TerrariaInstanceEventEntity.builder().instance(instance).type(TerrariaInstanceEventType.OUTPUT).build();
    }
}