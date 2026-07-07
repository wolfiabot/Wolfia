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

package space.npstr.wolfia.ecs.popcorn;

import java.util.ArrayList;
import java.util.List;
import space.npstr.wolfia.ecs.OutputMessage;
import space.npstr.wolfia.ecs.OutputSink;

/**
 * Captures all output messages for test assertions.
 */
public class TestOutputSink implements OutputSink {

    private final List<OutputMessage> messages = new ArrayList<>();

    @Override
    public void send(List<OutputMessage> messages) {
        this.messages.addAll(messages);
    }

    public List<OutputMessage> messages() {
        return List.copyOf(messages);
    }

    public List<String> channelMessages() {
        return messages.stream()
                .filter(m -> !m.isDm())
                .map(OutputMessage::getContent)
                .toList();
    }

    public List<String> dmMessagesTo(long userId) {
        return messages.stream()
                .filter(m -> m.isDm() && m.getTargetUserId() == userId)
                .map(OutputMessage::getContent)
                .toList();
    }

    public boolean anyChannelMessageContains(String text) {
        return channelMessages().stream().anyMatch(m -> m.contains(text));
    }

    public void clear() {
        messages.clear();
    }
}
