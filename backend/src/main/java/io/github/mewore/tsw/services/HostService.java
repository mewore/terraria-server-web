package io.github.mewore.tsw.services;

import java.util.List;

import org.springframework.stereotype.Service;

import io.github.mewore.tsw.models.HostEntity;
import io.github.mewore.tsw.repositories.HostRepository;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Service
public class HostService {

    private final @NonNull HostRepository hostRepository;

    public List<HostEntity> getAllHosts() {
        return hostRepository.findAll();
    }
}
