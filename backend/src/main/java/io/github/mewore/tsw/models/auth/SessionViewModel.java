package io.github.mewore.tsw.models.auth;

import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.github.mewore.tsw.models.AccountTypeEntity;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class SessionViewModel {

    private final @NonNull UUID token;

    private final @Nullable AccountTypeEntity role;
}
