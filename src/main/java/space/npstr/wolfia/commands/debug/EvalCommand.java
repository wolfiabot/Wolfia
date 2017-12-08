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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.db.entities.SetupEntity;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.RestActions;

import javax.annotation.Nonnull;
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

    @Nonnull
    @Override
    public String help() {
        return "Run js code with the Nashorn engine. By default no timeout is set for the task, set a timeout by "
                + "passing -t as the first argument and the amount of seconds to wait for the task to finish as the second argument. "
                + "Run with -k or kill as first argument to stop the last submitted eval task if it's still ongoing.";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context) throws DatabaseException {
        final long started = System.currentTimeMillis();

        String source = context.rawArgs;

        if (context.hasArguments() && (context.args[0].equals("-k") || context.args[0].equals("kill"))) {
            if (this.lastTask != null) {
                if (this.lastTask.isDone() || this.lastTask.isCancelled()) {
                    context.reply("Task isn't running.");
                } else {
                    this.lastTask.cancel(true);
                    context.reply("Task killed.");
                }
            } else {
                context.reply("No task found to kill.");
            }
            return true;
        }

        context.sendTyping();

        final int timeOut;
        if (context.args.length > 1 && (context.args[0].equals("-t") || context.args[0].equals("timeout"))) {
            timeOut = Integer.parseInt(context.args[1]);
            source = source.replaceFirst(context.args[0], "");
            source = source.replaceFirst(context.args[1], "");
        } else timeOut = -1;

        final String finalSource = source.trim();

        this.engine.put("context", context);
        this.engine.put("jda", context.invoker.getJDA());
        this.engine.put("channel", context.channel);
        this.engine.put("author", context.invoker);
        this.engine.put("bot", context.invoker.getJDA().getSelfUser());
        this.engine.put("member", context.getGuild() != null ? context.getGuild().getSelfMember() : null);
        this.engine.put("message", context.msg);
        this.engine.put("guild", context.getGuild());
        this.engine.put("game", Games.get(context.channel.getIdLong()));
        this.engine.put("setup", SetupEntity.load(context.channel.getIdLong()));
        this.engine.put("games", Games.class);//access the static methods like this from eval: games.static.myStaticMethod()

        final Future<?> future = Wolfia.executor.submit(() -> {

            final Object out;
            try {
                out = this.engine.eval(
                        "(function() {"
                                + "with (imports) {\n" + finalSource + "\n}"
                                + "})();");

            } catch (final Exception ex) {
                context.msg.addReaction(Emojis.X).queue(null, RestActions.defaultOnFail());
                context.reply(String.format("`%s`\n\n`%sms`",
                        ex.getMessage(), System.currentTimeMillis() - started));
                log.info("Error occurred in eval", ex);
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
            context.msg.addReaction(Emojis.OK_HAND).queue(null, RestActions.defaultOnFail());
            context.reply(String.format("```java\n%s```\n%s\n`%sms`",
                    finalSource, output, System.currentTimeMillis() - started));

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
                    context.reply("Task exceeded time limit of " + timeOut + " seconds.");
                } catch (final Exception ex) {
                    context.reply(String.format("`%s`\n\n`%sms`",
                            ex.getMessage(), System.currentTimeMillis() - started));
                }
            }
        };
        script.start();
        return true;
    }
}
