package io.github.mewore.tsw.services.util;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface InputStreamSupplier {

    InputStream supplyStream() throws IOException;
}
