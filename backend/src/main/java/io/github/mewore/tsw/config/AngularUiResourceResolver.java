package io.github.mewore.tsw.config;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.resource.PathResourceResolver;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class AngularUiResourceResolver extends PathResourceResolver {

    private static final String INDEX_FILE_NAME = "index.html";

    @Builder.Default
    private final String[] nonUiPaths = new String[0];

    @Override
    protected Resource getResource(@NonNull final String resourcePath, @NonNull final Resource location)
            throws IOException {

        // Paths that are reserved for other kinds of resources. By this point, the controllers corresponding to
        // these resources should have been matched but haven't, so this is a NOT_FOUND case.
        if (Arrays.stream(nonUiPaths).anyMatch(resourcePath::startsWith)) {
            return null;
        }

        final Resource requestedResource = location.createRelative(resourcePath);

        // Either a real static resource file which exists and is readable, or an Angular router path.
        // Any UI path which is not a static resource corresponds to the UI index file. The sub-path in the URL
        // itself will be kept so that Angular can use its router to determine what view to show.
        return requestedResource.exists() && requestedResource.isReadable()
                ? requestedResource
                : location.createRelative(INDEX_FILE_NAME);

    }
}
