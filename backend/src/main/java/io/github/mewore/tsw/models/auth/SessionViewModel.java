package io.github.mewore.tsw.models.auth;

import java.util.UUID;

import org.springframework.lang.Nullable;

import io.github.mewore.tsw.models.AccountTypeEntity;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SessionViewModel {

    private final @NonNull UUID token;

    @Nullable
    private final AccountTypeEntity role;
}
