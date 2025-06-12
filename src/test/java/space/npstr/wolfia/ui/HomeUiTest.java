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

package space.npstr.wolfia.ui;

import com.codeborne.selenide.WebDriverRunner;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static org.assertj.core.api.Assertions.assertThat;

class HomeUiTest extends BaseUiTest {

    @UiTest
    void hasButtons() {
        open("/");

        $("#dashboard_button").should(appear);
        $("#documentation_button").should(appear);
    }

    @UiTest
    void whenClickOnDashboardButton_redirectedToDashboard() {
        open("/");
        var button = $("#dashboard_button");
        button.click();

        $(".Dashboard").should(appear); //await navigation to be done
        assertThat(WebDriverRunner.url()).endsWith("/dashboard");
    }

}
