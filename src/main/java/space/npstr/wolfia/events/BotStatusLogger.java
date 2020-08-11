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

package space.npstr.wolfia.events;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.utils.discord.TextchatUtils;

@Component
public class BotStatusLogger {

    private final Optional<WebhookClient> botStatusWebhook;

    public BotStatusLogger(Optional<WebhookClient> botStatusWebhook) {
        this.botStatusWebhook = botStatusWebhook;
    }

    public CompletionStage<Optional<ReadonlyMessage>> log(String emoji, String message) {
        String zalgoMessage = Zalgo.convert(message);
        String content = TextchatUtils.toUtcTime(System.currentTimeMillis()) + " " + emoji + " " + zalgoMessage;
        return this.botStatusWebhook
                .map(webhookClient -> webhookClient.send(deepFriedBuilder()
                        .setContent(content)
                        .build())
                        .thenApply(Optional::ofNullable)
                )
                .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()));
    }

    private WebhookMessageBuilder deepFriedBuilder() {
        return new WebhookMessageBuilder()
                .setAvatarUrl("https://i.imgur.com/GhyO7Y9.png")
                .setUsername("Deep Fried Wolfia");
    }

    //copy pasta'd from https://github.com/JaneJeon/Zalgo4J/blob/master/Zalgo.java with some adaptations
    private static class Zalgo {
        private static final List<Character> zalgo_up = List.of(
                '\u030d', '\u030e', '\u0304', '\u0305', '\u033f', '\u0311', '\u0306', '\u0310', '\u0352', '\u0357',
                '\u0351', '\u0307', '\u0308', '\u030a', '\u0342', '\u0343', '\u0344', '\u034a', '\u034b', '\u034c',
                '\u0303', '\u0302', '\u030c', '\u0350', '\u0300', '\u0301', '\u030b', '\u030f', '\u0312', '\u0313',
                '\u0314', '\u033d', '\u0309', '\u0363', '\u0364', '\u0365', '\u0366', '\u0367', '\u0368', '\u0369',
                '\u036a', '\u036b', '\u036c', '\u036d', '\u036e', '\u036f', '\u033e', '\u035b', '\u0346', '\u031a'
        );
        private static final List<Character> zalgo_down = List.of(
                '\u0316', '\u0317', '\u0318', '\u0319', '\u031c', '\u031d', '\u031e', '\u031f', '\u0320', '\u0324',
                '\u0325', '\u0326', '\u0329', '\u032a', '\u032b', '\u032c', '\u032d', '\u032e', '\u032f', '\u0330',
                '\u0331', '\u0332', '\u0333', '\u0339', '\u033a', '\u033b', '\u033c', '\u0345', '\u0347', '\u0348',
                '\u0349', '\u034d', '\u034e', '\u0353', '\u0354', '\u0355', '\u0356', '\u0359', '\u035a', '\u0323'
        );

        private static int rand_int(int max) {
            return (int) (Math.random() * max);
        }

        private static char rand_zalgo(List<Character> list) {
            return list.get(rand_int(list.size()));
        }

        private static boolean is_zalgo(char c) {
            return zalgo_up.contains(c) || zalgo_down.contains(c);
        }

        private static boolean isAscii(char c) {
            return StandardCharsets.US_ASCII.newEncoder().canEncode(c);
        }

        public static String convert(String s) {
            StringBuilder result = new StringBuilder();
            char c;
            for (int i = 0; i < s.length(); i++) {
                c = s.charAt(i);
                if (is_zalgo(c)) continue;
                // add the normal character
                result.append(c);
                if (!isAscii(c)) continue;
                int num_up = rand_int(8);
                int num_down = rand_int(8);
                // add the zalgo decorations
                for (int j = 0; j < num_up; j++)
                    result.append(rand_zalgo(zalgo_up));
                for (int j = 0; j < num_down; j++)
                    result.append(rand_zalgo(zalgo_down));
            }
            return result.toString();
        }
    }

}
