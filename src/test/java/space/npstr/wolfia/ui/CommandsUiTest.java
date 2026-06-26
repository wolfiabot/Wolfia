/*
 * Copyright (C) 2016-2026 the original author or authors
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

import com.codeborne.selenide.ElementsCollection;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

class CommandsUiTest extends BaseUiTest {

    @UiTest
    void showsAllCommandCategories() {
        open("/commands");

        // the categories are fetched from /public/commands and rendered as sections
        $$(".category").shouldHave(size(5));
        $(byText("Starting a game")).should(appear);
        $(byText("Game actions")).should(appear);
        $(byText("Settings")).should(appear);
        $(byText("Statistics")).should(appear);
        $(byText("Other Commands")).should(appear);
    }

    @UiTest
    void showsKnownCommand() {
        open("/commands");

        // proves the endpoint + help() description parsing make it to the rendered table
        $$("td.trigger code").findBy(exactText("w.in")).should(appear);
    }

    @UiTest
    void showsCommandExamples() {
        open("/commands");

        // guards the help() example-line parsing all the way through to the DOM
        ElementsCollection examples = $$(".examples code");
        examples.shouldHave(sizeGreaterThan(0));
        examples.findBy(text("w.channelsettings")).should(appear);
    }
}
