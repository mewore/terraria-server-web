package io.github.mewore.tsw.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.resource.PathResourceResolver;

class AngularUiResourceResolver extends PathResourceResolver {

    private static final String INDEX_FILE_NAME = "index.html";

    private final List<String> nonUiPaths;

    AngularUiResourceResolver(final String... nonUiPaths) {
        this.nonUiPaths = Arrays.stream(nonUiPaths).collect(Collectors.toUnmodifiableList());
    }

    @Override
    protected @Nullable Resource getResource(@NonNull final String resourcePath, @NonNull final Resource location)
            throws IOException {

        // Paths that are reserved for other kinds of resources. By this point, the controllers corresponding to
        // these resources should have been matched but haven't, so this is a NOT_FOUND case.
        if (nonUiPaths.stream().anyMatch(resourcePath::startsWith)) {
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
