///*
// * Copyright (C) 2017 Dennis Neufeld
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published
// * by the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//package space.npstr.wolfia.commands;
//
//import net.dv8tion.jda.core.entities.MessageChannel;
//import net.dv8tion.jda.core.entities.MessageEmbed;
//import net.dv8tion.jda.core.entities.TextChannel;
//import net.dv8tion.jda.core.entities.User;
//import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
//import space.npstr.wolfia.Config;
//import space.npstr.wolfia.Wolfia;
//
//import javax.annotation.CheckReturnValue;
//import javax.annotation.Nonnull;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//
///**
// * Created by npstr on 23.08.2016
// */
//public class CommandParser {
//
//    public static CommandContainer parse(final String rw, final MessageReceivedEvent e, final long received) {
//        final ArrayList<String> split = new ArrayList<>();
//        final String beheaded = rw.substring(Config.PREFIX.length()).trim();
//        final String[] splitBeheaded = beheaded.split("\\s+");
//        Collections.addAll(split, splitBeheaded);
//        final String command = split.get(0).toLowerCase();
//        final String argsRaw = rw.substring(Config.PREFIX.length() + command.length()).trim();
//        final String[] args = new String[split.size() - 1];
//        split.subList(1, split.size()).toArray(args);
//
//        return new CommandContainer(rw, beheaded, splitBeheaded, command, argsRaw, args, e, received);
//    }
//
//    public static class CommandContainer {
//        public final String raw; //the full thing
//        public final String beheaded; //without the prefix
//        public final String[] splitBeheaded; //without the prefix, split by " "
//        public final String command; // the actual command, in lower case
//        public final String argsRaw; //the unsplit arguments
//        public final String[] args; // the actual arguments
//        public final MessageReceivedEvent event; //underlying event
//        public final long received; //earliest moment of when the event entered our bots' code
//        public final User invoker;
//        public final MessageChannel channel;
//
//        public CommandContainer(final String rw, final String beheaded, final String[] splitBeheaded,
//                                final String command, final String argsRaw, final String[] args,
//                                final MessageReceivedEvent e, final long received) {
//            this.raw = rw;
//            this.beheaded = beheaded;
//            this.splitBeheaded = Arrays.copyOf(splitBeheaded, splitBeheaded.length);
//            this.command = command;
//            this.argsRaw = argsRaw;
//            this.args = Arrays.copyOf(args, args.length);
//            this.event = e;
//            this.received = received;
//            this.invoker = e.getAuthor();
//            this.channel = e.getChannel();
//        }
//
//
//        //todo introduce this to the code everywhere
//        public void reply(final String reply) {
//            if (this.event.getPrivateChannel() != null) {
//                Wolfia.handlePrivateOutputMessage(this.event.getAuthor().getIdLong(), Wolfia.defaultOnFail(), reply);
//            } else {
//                Wolfia.handleOutputMessage(this.event.getTextChannel(), reply);
//            }
//        }
//
//        public void reply(final String message, final Object... args) {
//            reply(String.format(message, args));
//        }
//
//        public void reply(final MessageEmbed embed) {
//            if (this.event.getPrivateChannel() != null) {
//                Wolfia.handlePrivateOutputEmbed(this.event.getAuthor().getIdLong(), Wolfia.defaultOnFail(), embed);
//            } else {
//                Wolfia.handleOutputEmbed(this.event.getTextChannel(), embed);
//            }
//        }
//
//        public void replyWithName(@Nonnull final String message) {
//            reply(getEffectiveName() + ", " + message);
//        }
//
//        public void replyWithMention(@Nonnull final String message) {
//            reply(this.invoker.getAsMention() + ", " + message);
//        }
//
//        //name or nickname of the author issuing the command
//        @Nonnull
//        @CheckReturnValue
//        public String getEffectiveName() {
//            return this.channel instanceof TextChannel
//                    ? ((TextChannel) this.channel).getGuild().getMember(this.invoker).getEffectiveName()
//                    : this.invoker.getName();
//        }
//    }
//}
