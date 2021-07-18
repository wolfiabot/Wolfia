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

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.JDA;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.attributeMatching;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatusUiTest extends BaseUiTest {

    @UiTest
    void givenSingleShardOfVariousStatuses_showAppropriateColoring() {
        Arrays.stream(JDA.Status.values())
                .map(status -> {
                    switch (status) {
                        case CONNECTED:
                            return Map.entry(status, "online");
                        case FAILED_TO_LOGIN:
                        case SHUTDOWN:
                        case SHUTTING_DOWN:
                            return Map.entry(status, "offline");
                        default:
                            return Map.entry(status, "connecting");
                    }
                })
                .forEach(entry -> givenStatus_showColor(entry.getKey(), entry.getValue()));
    }

    private void givenStatus_showColor(JDA.Status status, String expectedClass) {
        doReturn(List.of(mockShardWithStatus(status))).when(shardManager).getShards();

        open("/status");

        $(".statusContent").should(appear);
        ElementsCollection shards = $$(".Shard");
        shards.shouldHave(size(1));
        shards.get(0).should(cssClass(expectedClass));
    }

    @UiTest
    void givenMultipleShards_showMultipleShards() {
        List<JDA> jdas = List.of(
                mockShardWithStatus(JDA.Status.CONNECTED),
                mockShardWithStatus(JDA.Status.ATTEMPTING_TO_RECONNECT),
                mockShardWithStatus(JDA.Status.SHUTDOWN)
        );
        doReturn(jdas).when(shardManager).getShards();

        open("/status");

        $(".statusContent").should(appear);
        ElementsCollection shards = $$(".Shard");
        shards.shouldHave(size(jdas.size()));
        shards.get(0).should(cssClass("online"));
        shards.get(1).should(cssClass("connecting"));
        shards.get(2).should(cssClass("offline"));
    }

    private JDA mockShardWithStatus(JDA.Status status) {
        JDA shard = mock(JDA.class);
        when(shard.getShardInfo()).thenReturn(new JDA.ShardInfo(0, 1));
        when(shard.getStatus()).thenReturn(status);
        return shard;
    }

    @UiTest
    void footerHasInviteLink() {
        open("/status");

        SelenideElement inviteLink = $(".statusFooter a");
        inviteLink.should(appear);
        inviteLink.should(attributeMatching("href", "https://discord.gg/.*"));
    }
}
