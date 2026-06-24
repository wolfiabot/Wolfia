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

package space.npstr.wolfia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensure that the context loads eagerly. At least one eager test should help identify problems in loading the context
 * before they happen in prod.
 *
 * <p>This is the only test that runs in a second, separate application context (the "eager" profile). That context
 * keeps its own {@code @Scheduled} pollers (e.g. {@link space.npstr.wolfia.domain.setup.lastactive.LastActiveRepository})
 * running against the shared test database. Left cached, it races other contexts' schedulers over the same rows, which
 * made {@code AutoOuterTest} flaky. Closing this context after the class keeps exactly one scheduler alive at a time.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("eager")
class EagerLauncherTest extends LauncherTest {

    @Test
    void givenEager_lazyInitShouldBeFalse() {
        assertThat(this.applicationContext.getBeanFactoryPostProcessors())
                .filteredOnAssertions(b -> assertThat(b).isInstanceOf(LazyInitializationBeanFactoryPostProcessor.class))
                .isEmpty();
    }

}
