package io.github.mewore.tsw.services;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.repositories.HostRepository;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HostServiceTest {

    @InjectMocks
    private HostService hostService;

    @Mock
    private HostRepository hostRepository;

    @Test
    void testGetAllHosts() {
        final List<HostEntity> result = Collections.emptyList();
        when(hostRepository.findAll()).thenReturn(result);

        assertSame(result, hostService.getAllHosts());
        verify(hostRepository, only()).findAll();
    }
}