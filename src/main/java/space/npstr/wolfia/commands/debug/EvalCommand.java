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
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.db.entities.SetupEntity;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.utils.discord.Emojis;

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
public class EvalCommand extends BaseCommand implements IOwnerRestricted {

    private static final Logger log = LoggerFactory.getLogger(EvalCommand.class);

    private Future lastTask;

    //Thanks Fred & Dinos!
    private final ScriptEngine engine;

    public EvalCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
        this.engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            this.engine.eval("var imports = new JavaImporter("
                    + "java.io"
                    + ",java.lang"
                    + ",java.util"
                    + ",Packages.space.npstr.wolfia"
                    + ",Packages.space.npstr.wolfia.charts"
                    + ",Packages.space.npstr.wolfia.commands"
                    + ",Packages.space.npstr.wolfia.commands.debug"
                    + ",Packages.space.npstr.wolfia.commands.game"
                    + ",Packages.space.npstr.wolfia.commands.ingame"
                    + ",Packages.space.npstr.wolfia.commands.stats"
                    + ",Packages.space.npstr.wolfia.commands.util"
                    + ",Packages.space.npstr.wolfia.db"
                    + ",Packages.space.npstr.wolfia.db.entities"
                    + ",Packages.space.npstr.wolfia.db.entities.stats"
                    + ",Packages.space.npstr.wolfia.events"
                    + ",Packages.space.npstr.wolfia.db"
                    + ",Packages.space.npstr.wolfia.game"
                    + ",Packages.space.npstr.wolfia.definitions"
                    + ",Packages.space.npstr.wolfia.mafia"
                    + ",Packages.space.npstr.wolfia.popcorn"
                    + ",Packages.space.npstr.wolfia.tools"
                    + ",Packages.space.npstr.wolfia.listings"
                    + ",Packages.space.npstr.wolfia.utils"
                    + ",Packages.space.npstr.wolfia.utils.discord"
                    + ",Packages.space.npstr.wolfia.utils.img"
                    + ",Packages.space.npstr.wolfia.utils.log"
                    + ");");

        } catch (final ScriptException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String help() {
        return "Run js code with the Nashorn engine. By default no timeout is set for the task, set a timeout by "
                + "passing -t as the first argument and the amount of seconds to wait for the task to finish as the second argument. "
                + "Run with -k or kill as first argument to stop the last submitted eval task if it's still ongoing.";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) throws DatabaseException {
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

        channel.sendTyping().queue(null, Wolfia.defaultOnFail());

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
        this.engine.put("setup", SetupEntity.load(commandInfo.event.getChannel().getIdLong()));
        this.engine.put("games", Games.class);//access the static methods like this from eval: games.static.myStaticMethod()

        final Future<?> future = Wolfia.executor.submit(() -> {

            final Object out;
            try {
                out = this.engine.eval(
                        "(function() {"
                                + "with (imports) {\n" + finalSource + "\n}"
                                + "})();");

            } catch (final Exception ex) {
                commandInfo.event.getMessage().addReaction(Emojis.X).queue(null, Wolfia.defaultOnFail());
                Wolfia.handleOutputMessage(channel, "`%s`\n\n`%sms`",
                        ex.getMessage(), System.currentTimeMillis() - started);
                log.error("Error occurred in eval", ex);
                return;
            }

            final String output;
            if (out == null) {
                output = "";
            } else if (out.toString().contains("\n")) {
                output = "EvalCommand: ```\n" + out.toString() + "```";
            } else {
                output = "EvalCommand: `" + out.toString() + "`";
            }
            commandInfo.event.getMessage().addReaction(Emojis.OK_HAND).queue(null, Wolfia.defaultOnFail());
            Wolfia.handleOutputMessage(channel, "```java\n%s```\n%s\n`%sms`",
                    finalSource, output, System.currentTimeMillis() - started);

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
}
