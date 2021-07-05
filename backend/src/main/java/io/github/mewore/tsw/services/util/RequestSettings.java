package io.github.mewore.tsw.services.util;

import org.springframework.http.HttpMethod;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class RequestSettings {

    @Builder.Default
    private final @NonNull HttpMethod method = HttpMethod.GET;
}
