package io.github.mewore.tsw.services;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mewore.tsw.exceptions.NotFoundException;
import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.repositories.HostRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
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
    void testGetHost() throws NotFoundException {
        final HostEntity result = mock(HostEntity.class);
        when(hostRepository.findById(1L)).thenReturn(Optional.of(result));

        assertSame(result, hostService.getHost(1));
        verify(hostRepository, only()).findById(1L);
    }

    @Test
    void testGetHost_notFound() {
        when(hostRepository.findById(1L)).thenReturn(Optional.empty());

        final Exception exception = assertThrows(NotFoundException.class, () -> hostService.getHost(1));
        assertEquals("A host with ID 1 does not exist", exception.getMessage());
    }

    @Test
    void testGetAllHosts() {
        final List<HostEntity> result = Collections.emptyList();
        when(hostRepository.findAll()).thenReturn(result);

        assertSame(result, hostService.getAllHosts());
        verify(hostRepository, only()).findAll();
    }
}