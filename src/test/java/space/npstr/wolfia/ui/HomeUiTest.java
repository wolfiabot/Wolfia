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

import com.codeborne.selenide.WebDriverRunner;

import static com.codeborne.selenide.Condition.matchesText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static org.assertj.core.api.Assertions.assertThat;

class HomeUiTest extends BaseUiTest {

    @UiTest
    void homeHasButtons() {
        open("/");
        var buttons = $$(".Home .buttons .button");
        buttons.shouldHaveSize(2);
        assertThat(buttons)
                .anySatisfy(button -> button.should(text("Add to Discord")))
                .anySatisfy(button -> button.should(text("See Commands")));
    }

    @UiTest
    void addToDiscordButtonRedirectsToDashboard() {
        open("/");
        var buttons = $$(".Home .buttons .button");

        var button = buttons.findBy(matchesText("Add to Discord"));
        button.click();

        $(".Dashboard").should(visible);
        assertThat(WebDriverRunner.url()).endsWith("/dashboard");
    }

}
