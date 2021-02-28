package io.github.mewore.tsw.config;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AngularUiResourceResolverTest {

    private static final String NON_UI_PATH = "non/ui/path/";

    private static final String RESOURCE_PATH = "path/to/resource";

    private static final ResourceResolver RESOURCE_RESOLVER = AngularUiResourceResolver.builder()
            .nonUiPaths(new String[]{NON_UI_PATH})
            .build();

    @Mock
    private Resource location;

    @Mock
    private Resource resource;

    @Mock
    private Resource indexHtmlResource;

    @Mock
    private ResourceResolverChain resolverChain;

    @Test
    void testResolveResource_nonUiPath() {
        assertNull(RESOURCE_RESOLVER.resolveResource(null, "non/ui/path/resource", getLocationList(), resolverChain));
    }

    @Test
    void testResolveResource() throws IOException {
        when(location.createRelative(RESOURCE_PATH)).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.isReadable()).thenReturn(true);
        assertSame(resource, RESOURCE_RESOLVER.resolveResource(null, RESOURCE_PATH, getLocationList(), resolverChain));
    }

    @Test
    void testResolveResource_nonExistent() throws IOException {
        when(location.createRelative(RESOURCE_PATH)).thenReturn(resource);
        when(location.createRelative("index.html")).thenReturn(indexHtmlResource);
        when(resource.exists()).thenReturn(false);
        assertSame(indexHtmlResource,
                RESOURCE_RESOLVER.resolveResource(null, RESOURCE_PATH, getLocationList(), resolverChain));
    }

    @Test
    void testResolveResource_unreadable() throws IOException {
        when(location.createRelative(RESOURCE_PATH)).thenReturn(resource);
        when(location.createRelative("index.html")).thenReturn(indexHtmlResource);
        when(resource.exists()).thenReturn(true);
        when(resource.isReadable()).thenReturn(false);
        assertSame(indexHtmlResource,
                RESOURCE_RESOLVER.resolveResource(null, RESOURCE_PATH, getLocationList(), resolverChain));
    }

    private List<Resource> getLocationList() {
        return Collections.singletonList(location);
    }
}