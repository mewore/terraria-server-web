package io.github.mewore.tsw.events;

import java.util.function.Consumer;

interface ManagedSubscription<T> extends Subscription<T>, Consumer<T> {

}
