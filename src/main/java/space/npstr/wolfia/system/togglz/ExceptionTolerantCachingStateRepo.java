/*
 * Copyright (C) 2016-2025 the original author or authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.npstr.wolfia.system.togglz;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.lang.Nullable;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.repository.cache.CachingStateRepository;

/**
 * Copy pasta of {@link CachingStateRepository} with a more tolerant approach
 * to dealing with exceptions thrown by the delegate, for example when a database were not available, by
 * keeping returning the cached value even though it is expired, until the delegate will respond again without exceptions
 * <p>
 * To achieve this, we introduce a "stale mode" where requests are exclusively served from the cache.
 * Stale mode gets turned on by the delegate throwing an exception, and is automatically over after a period of time.
 */
public class ExceptionTolerantCachingStateRepo implements StateRepository {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExceptionTolerantCachingStateRepo.class);

    private final StateRepository delegate;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Clock clock;
    private final long ttl;
    private final Duration stalePeriod;

    private Instant staleModeStarted = Instant.MIN;

    /**
     * Creates a caching facade for the supplied {@link StateRepository}. The cached state of a feature will expire after the
     * supplied TTL or if {@link #setFeatureState(FeatureState)} is invoked.
     *
     * @param delegate The repository to delegate invocations to
     * @param ttl      The time in milliseconds after which a cache entry will expire
     * @throws IllegalArgumentException if the specified ttl is negative
     */
    public ExceptionTolerantCachingStateRepo(StateRepository delegate, long ttl, Clock clock, Duration stalePeriod) {
        if (ttl < 0) {
            throw new IllegalArgumentException("Negative TTL value: " + ttl);
        }

        this.delegate = delegate;
        this.ttl = ttl;
        this.clock = clock;
        this.stalePeriod = stalePeriod;
    }

    /**
     * Creates a caching facade for the supplied {@link StateRepository}. The cached state of a feature will expire after the
     * supplied TTL rounded down to milliseconds or if {@link #setFeatureState(FeatureState)} is invoked.
     *
     * @param delegate    The repository to delegate invocations to
     * @param ttl         The time in a given {@code ttlTimeUnit} after which a cache entry will expire
     * @param ttlTimeUnit The unit that {@code ttl} is expressed in
     */
    public ExceptionTolerantCachingStateRepo(StateRepository delegate, long ttl, TimeUnit ttlTimeUnit) {
        this(delegate, ttlTimeUnit.toMillis(ttl), Clock.systemUTC(), Duration.ofMinutes(1));
    }

    public ExceptionTolerantCachingStateRepo(StateRepository delegate, Duration ttl, Clock clock, Duration stalePeriod) {
        this(delegate, ttl.toMillis(), clock, stalePeriod);
    }

    @Nullable
    @Override
    public FeatureState getFeatureState(Feature feature) {

        // first try to find it from the cache
        CacheEntry entry = cache.get(feature.name());
        if (entry != null && (!isExpired(entry) || isStaleMode())) {
            return entry.getState() != null ? entry.getState().copy() : null;
        }

        // no cache hit
        FeatureState featureState;
        try {
            featureState = delegate.getFeatureState(feature);
        } catch (Exception e) {
            this.staleModeStarted = this.clock.instant();
            if (entry != null) {
                log.warn("Delegate threw exception, turning on stale mode", e);
                return entry.getState() != null ? entry.getState().copy() : null;
            }
            throw e;
        }

        // cache the result (may be null)
        cache.put(feature.name(), new CacheEntry(featureState != null ? featureState.copy() : null));

        // return the result
        return featureState;

    }

    @Override
    public void setFeatureState(FeatureState featureState) {
        delegate.setFeatureState(featureState);
        cache.remove(featureState.getFeature().name());
    }

    /**
     * Checks whether this supplied {@link CacheEntry} should be ignored.
     */
    private boolean isExpired(CacheEntry entry) {
        if (ttl == 0) {
            return false;
        }

        return entry.getTimestamp() + ttl < System.currentTimeMillis();
    }

    private boolean isStaleMode() {
        Instant staleModeEnds = this.staleModeStarted.plus(this.stalePeriod);
        return this.clock.instant().isBefore(staleModeEnds);
    }

    /**
     * This class represents a cached repository lookup
     */
    private static class CacheEntry {

        private final FeatureState state;

        private final long timestamp;

        public CacheEntry(FeatureState state) {
            this.state = state;
            this.timestamp = System.currentTimeMillis();
        }

        public FeatureState getState() {
            return state;
        }

        public long getTimestamp() {
            return timestamp;
        }

    }
}
