/*
 * Copyright (C) 2017 Dennis Neufeld
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

package space.npstr.wolfia.commands.debug;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ICommand;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.SetupEntity;
import space.npstr.wolfia.game.Games;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by napster on 27.05.17.
 * <p>
 * run js code in the bot
 */
public class EvalCommand implements ICommand, IOwnerRestricted {

    public static final String COMMAND = "eval";
    private static final Logger log = LoggerFactory.getLogger(EvalCommand.class);

    private Future lastTask;

    //Thanks Fred & Dinos!
    private final ScriptEngine engine;

    public EvalCommand() {
        this.engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            this.engine.eval("var imports = new JavaImporter(java.io, java.lang, java.util);");

        } catch (final ScriptException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) {
        final long started = System.currentTimeMillis();
        final Guild guild = commandInfo.event.getGuild();
        final TextChannel channel = commandInfo.event.getTextChannel();
        final Message message = commandInfo.event.getMessage();
        final Member author = commandInfo.event.getMember();
        final JDA jda = guild.getJDA();

        String source = commandInfo.beheaded.substring(commandInfo.command.length()).trim();

        if (commandInfo.args.length > 0 && (commandInfo.args[0].equals("-k") || commandInfo.args[0].equals("kill"))) {
            if (this.lastTask != null) {
                if (this.lastTask.isDone() || this.lastTask.isCancelled()) {
                    Wolfia.handleOutputMessage(channel, "Task isn't running.");
                } else {
                    this.lastTask.cancel(true);
                    Wolfia.handleOutputMessage(channel, "Task killed.");
                }
            } else {
                Wolfia.handleOutputMessage(channel, "No task found to kill.");
            }
            return true;
        }

        channel.sendTyping().queue(null, Wolfia.defaultOnFail);

        final int timeOut;
        if (commandInfo.args.length > 1 && (commandInfo.args[0].equals("-t") || commandInfo.args[0].equals("timeout"))) {
            timeOut = Integer.parseInt(commandInfo.args[1]);
            source = source.replaceFirst(commandInfo.args[0], "");
            source = source.replaceFirst(commandInfo.args[1], "");
        } else timeOut = -1;

        final String finalSource = source.trim();

        this.engine.put("jda", jda);
        this.engine.put("api", jda);
        this.engine.put("channel", channel);
        this.engine.put("author", author);
        this.engine.put("bot", jda.getSelfUser());
        this.engine.put("member", guild.getSelfMember());
        this.engine.put("message", message);
        this.engine.put("guild", guild);
        this.engine.put("game", Games.get(channel.getIdLong()));
        this.engine.put("setup", DbWrapper.getEntity(commandInfo.event.getChannel().getIdLong(), SetupEntity.class));

        final Future<?> future = Wolfia.executor.submit(() -> {

            final Object out;
            try {
                out = this.engine.eval(
                        "(function() {"
                                + "with (imports) {\n" + finalSource + "\n}"
                                + "})();");

            } catch (final Exception ex) {
                Wolfia.handleOutputMessage(channel, "`%s`\n\n`%sms`",
                        ex.getMessage(), System.currentTimeMillis() - started);
                log.error("Error occurred in eval", ex);
                return;
            }

            final String outputS;
            if (out == null) {
                outputS = ":ok_hand::skin-tone-3:";
            } else if (out.toString().contains("\n")) {
                outputS = "\nEvalCommand: ```\n" + out.toString() + "```";
            } else {
                outputS = "\nEvalCommand: `" + out.toString() + "`";
            }

            Wolfia.handleOutputMessage(channel, "```java\n%s```\n%s `%sms`",
                    finalSource, outputS, System.currentTimeMillis() - started);

        });
        this.lastTask = future;

        final Thread script = new Thread("EvalCommand") {
            @Override
            public void run() {
                try {
                    if (timeOut > -1) {
                        future.get(timeOut, TimeUnit.SECONDS);
                    }
                } catch (final TimeoutException ex) {
                    future.cancel(true);
                    Wolfia.handleOutputMessage(channel, "Task exceeded time limit of %s seconds.", timeOut);
                } catch (final Exception ex) {
                    Wolfia.handleOutputMessage(channel, "`%s`\n\n`%sms`",
                            ex.getMessage(), System.currentTimeMillis() - started);
                }
            }
        };
        script.start();
        return true;
    }

    @Override
    public String help() {
        return "Run js eval code on the bot";
    }
}
