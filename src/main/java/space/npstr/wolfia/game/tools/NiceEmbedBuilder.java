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

package space.npstr.wolfia.game.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import space.npstr.wolfia.commands.MessageContext;

/**
 * An embed builder that accepts ChunkingFields which respect value length and chunk themselves up into acceptable lengths
 */
public class NiceEmbedBuilder extends EmbedBuilder {

    /**
     * @return a general purpose preformatted nice builder for embeds
     */
    public static NiceEmbedBuilder defaultBuilder() {
        NiceEmbedBuilder neb = new NiceEmbedBuilder();
        neb.setColor(MessageContext.BLACKIA);
        return neb;
    }

    //default trim is true
    public NiceEmbedBuilder addField(ChunkingField field, boolean... trim) {

        boolean tr = true;
        if (trim.length > 0 && !trim[0]) tr = false;

        //only the first one gets the name
        String content = field.content.get(0).toString();
        if (tr) content = content.trim();
        addField(field.name, content, field.inline);

        for (int i = 1; i < field.content.size(); i++) {
            content = field.content.get(i).toString();
            if (tr) content = content.trim();
            addField("", content, field.inline);
        }

        return this;
    }


    public static class ChunkingField {

        private final String name;
        private final boolean inline;

        private final List<StringBuilder> content = new ArrayList<>();
        private StringBuilder current;

        public ChunkingField(String name, boolean inline) {
            this.name = name;
            this.inline = inline;
            this.current = new StringBuilder();
            this.content.add(this.current);
        }

        //will add the str to the field, if the field would go over the allowed limit, it will create a new field
        //newLine adds a new line at the end of the string, default is false
        public ChunkingField add(String str, boolean... newLine) {

            String toBeAdded = str;
            if (newLine.length > 0 && newLine[0]) toBeAdded += "\n";

            if (this.current == null || this.current.length() + toBeAdded.length() > MessageEmbed.VALUE_MAX_LENGTH) {
                this.current = new StringBuilder();
                this.content.add(this.current);
            }

            this.current.append(toBeAdded);

            return this;
        }

        public ChunkingField addAll(Collection<String> strings, boolean... newLine) {
            for (String str : strings) {
                add(str, newLine);
            }
            return this;
        }
    }
}
