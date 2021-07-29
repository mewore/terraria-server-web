package io.github.mewore.tsw.services.terraria;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import io.github.mewore.tsw.events.Subscription;
import io.github.mewore.tsw.events.TerrariaInstanceUpdatedEvent;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceEntity;
import io.github.mewore.tsw.models.terraria.TerrariaInstanceState;
import io.github.mewore.tsw.repositories.terraria.TerrariaInstanceRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class TerrariaInstanceEventService implements ApplicationListener<TerrariaInstanceUpdatedEvent> {

    private final Map<Long, List<QueueSubscription<TerrariaInstanceEntity>>> instanceSubscriptionMap = new HashMap<>();

    private final TerrariaInstanceRepository terrariaInstanceRepository;

    private int allSubscriptions = 0;

    private int closedSubscriptions = 0;

    /**
     * Acknowledge an event that represents an updated Terraria instance entity.
     *
     * @param event The changed instance.
     * @deprecated This method is meant to be used only by Spring itself.
     */
    @Deprecated
    @Synchronized
    @Override
    public void onApplicationEvent(final TerrariaInstanceUpdatedEvent event) {
        final TerrariaInstanceEntity instance = event.getChangedInstance();
        final @Nullable List<QueueSubscription<TerrariaInstanceEntity>> subscriptionList = instanceSubscriptionMap.get(
                instance.getId());
        if (subscriptionList != null) {
            for (final QueueSubscription<TerrariaInstanceEntity> subscription : subscriptionList) {
                if (subscription.isOpen()) {
                    subscription.accept(instance);
                }
            }
        }
    }

    @Synchronized
    public Subscription<TerrariaInstanceEntity> subscribe(final TerrariaInstanceEntity instance) {
        final QueueSubscription<TerrariaInstanceEntity> subscription = new QueueSubscription<>(this::unsubscribe,
                LogManager.getLogger("Subscription:TerrariaInstance:" + instance.getUuid()),
                () -> terrariaInstanceRepository.getOne(instance.getId()));
        instanceSubscriptionMap.compute(instance.getId(), (id, subscriptionList) -> {
            final List<QueueSubscription<TerrariaInstanceEntity>> result =
                    subscriptionList == null ? new ArrayList<>(1) : subscriptionList;
            result.add(subscription);
            return result;
        });
        allSubscriptions++;
        return subscription;
    }

    @Synchronized
    private void unsubscribe() {
        closedSubscriptions++;
        if (closedSubscriptions < allSubscriptions / 2) {
            return;
        }
        if (closedSubscriptions == allSubscriptions) {
            instanceSubscriptionMap.clear();
        } else {
            instanceSubscriptionMap.values().removeIf(subscriptionList -> {
                subscriptionList.removeIf(QueueSubscription::isClosed);
                return subscriptionList.isEmpty();
            });
        }
        allSubscriptions -= closedSubscriptions;
        closedSubscriptions = 0;
    }

    public TerrariaInstanceEntity waitForInstanceState(final TerrariaInstanceEntity instance,
            final Subscription<TerrariaInstanceEntity> subscription,
            final Duration timeout,
            final TerrariaInstanceState... desiredStates) throws IllegalStateException, InterruptedException {
        final Set<TerrariaInstanceState> stateSet = Set.of(desiredStates);
        final @Nullable TerrariaInstanceEntity result = subscription.waitFor(
                newInstance -> stateSet.contains(newInstance.getState()), timeout);
        if (result == null) {
            throw new IllegalStateException(String.format(
                    "The instance %s did not reach the state(s) %s within a timeout of %s; instead, its state is %s.",
                    instance.getUuid(),
                    Arrays.stream(desiredStates).map(Objects::toString).collect(Collectors.joining("/")), timeout,
                    instance.getState()));
        }
        return result;
    }

    @RequiredArgsConstructor
    private static class QueueSubscription<T> implements Consumer<@NonNull T>, Subscription<T> {

        private static final int QUEUE_CAPACITY = 10;

        @Getter
        private final BlockingQueue<@NonNull T> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        private final Runnable onClosed;

        private final Logger logger;

        /**
         * Used as a last-ditch attempt if waiting for the value fails.
         */
        private final Supplier<T> valueSupplier;

        @Getter
        private boolean open = true;

        public boolean isClosed() {
            return !open;
        }

        @Override
        public void accept(final @NonNull T value) {
            if (!queue.offer(value)) {
                logger.warn("The subscription blocking queue for has been overfilled! Skipping the next value.");
            }
        }

        @Override
        public @Nullable T waitFor(final Predicate<T> predicate, final Duration timeout) throws InterruptedException {
            final Instant deadline = Instant.now().plus(timeout);
            while (true) {
                final @Nullable T result = queue.poll(Instant.now().until(deadline, ChronoUnit.MILLIS),
                        TimeUnit.MILLISECONDS);
                if (result == null) {
                    break;
                }
                if (predicate.test(result)) {
                    return result;
                }
            }
            final T result = valueSupplier.get();
            return predicate.test(result) ? result : null;
        }

        @Override
        public void close() {
            if (open) {
                open = false;
                onClosed.run();
            }
        }
    }
}
