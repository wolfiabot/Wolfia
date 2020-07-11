/*
 * Copyright (C) 2016-2020 the original author or authors
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

package space.npstr.wolfia.ui;

import com.codeborne.selenide.Browsers;
import com.codeborne.selenide.Configuration;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import space.npstr.wolfia.ApplicationTest;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * All classes extending this class should annotate their tests with {@link UiTest} instead of JUnit5 annotations.
 * <p>
 * This base class will then execute each {@link UiTest} against multiple browsers.
 */
abstract class BaseUiTest extends ApplicationTest {

    protected static final BrowserWebDriverContainer<?> CHROME_CONTAINER = new BrowserWebDriverContainer<>()
            .withLogConsumer(new Slf4jLogConsumer(containerLogger("Chrome")))
            .withCapabilities(new ChromeOptions());

    protected static final BrowserWebDriverContainer<?> FIREFOX_CONTAINER = new BrowserWebDriverContainer<>()
            .withLogConsumer(new Slf4jLogConsumer(containerLogger("Firefox")))
            .withCapabilities(new FirefoxOptions());


    private boolean started = false;

    private synchronized void setupOnce() {
        if (started) return;

        Testcontainers.exposeHostPorts(this.port);
        CHROME_CONTAINER.start();
        FIREFOX_CONTAINER.start();

        // We can't use BrowserWebDriverContainer#getTestHostIpAddress as it requires docker-machine,
        // but there is a different way to expose a host IP to the containers:
        // See https://github.com/testcontainers/testcontainers-java/issues/166
        // and https://github.com/testcontainers/testcontainers-java/releases/tag/1.9.0
        Configuration.baseUrl = "http://host.testcontainers.internal:" + this.port;
        Configuration.headless = true;

        started = true;
    }

    @BeforeEach
    void setUp() {
        // we need to run a non-static setup once due to relying on the random local port
        // which is only available once spring started
        setupOnce();
    }

    @TestFactory
    Stream<DynamicTest> streamTests() {
        return this.getTestMethods().stream()
                .flatMap(method -> {
                    String name = method.getName();
                    return Stream.of(
                            dynamicTest(name + "_onChrome", () -> {
                                useChrome();
                                method.invoke(this);
                            }),
                            dynamicTest(name + "_onFirefox", () -> {
                                useFirefox();
                                method.invoke(this);
                            })
                    );
                });
    }

    private List<Method> getTestMethods() {
        return Arrays.stream(this.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(UiTest.class))
                .collect(Collectors.toList());
    }

    private void useChrome() {
        Configuration.remote = CHROME_CONTAINER.getSeleniumAddress().toString();
        Configuration.browser = Browsers.CHROME;
    }

    private void useFirefox() {
        Configuration.remote = FIREFOX_CONTAINER.getSeleniumAddress().toString();
        Configuration.browser = Browsers.FIREFOX;
    }
}
