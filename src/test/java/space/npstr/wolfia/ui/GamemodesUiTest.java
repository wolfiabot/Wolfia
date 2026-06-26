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

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

class GamemodesUiTest extends BaseUiTest {

    @UiTest
    void showsBothGames() {
        open("/gamemodes");

        // one card per game, fetched from /public/gamemodes
        $$(".gamemodecard").shouldHave(size(2));
        $(byText("Mafia")).should(appear);
        $(byText("Popcorn")).should(appear);
    }

    @UiTest
    void showsModesAndPlayerCounts() {
        open("/gamemodes");

        $$(".gamemodecard").shouldHave(size(2));
        // the player counts come straight from the GameInfo implementations
        $$(".player-count").findBy(text("9+ players")).should(appear);
        $$(".player-count").findBy(text("3 to 26 players")).should(appear);
    }

    @UiTest
    void showsRoleIcons() {
        open("/gamemodes");

        // the :role: tokens in the backend prose are mapped to icons by gameText.js
        $$("img.role-icon").shouldHave(sizeGreaterThan(0));
    }
}
