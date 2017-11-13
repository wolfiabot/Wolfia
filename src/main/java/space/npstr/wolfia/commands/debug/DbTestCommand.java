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

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.persistence.EntityManager;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by napster on 30.05.17.
 * Stress tests the database
 */
public class DbTestCommand extends BaseCommand implements IOwnerRestricted {

    public DbTestCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    private static final Logger log = LoggerFactory.getLogger(DbTestCommand.class);

    private enum Result {WORKING, SUCCESS, FAILED}

    // the SQL syntax used here works with both SQLite and PostgreSQL, beware when altering
    @SuppressWarnings("FieldCanBeLocal")
    private final String DROP_TEST_TABLE = "DROP TABLE IF EXISTS test;";
    @SuppressWarnings("FieldCanBeLocal")
    private final String CREATE_TEST_TABLE = "CREATE TABLE IF NOT EXISTS test (id SERIAL, val INTEGER, PRIMARY KEY (id));";
    private final String INSERT_TEST_TABLE = "INSERT INTO test (val) VALUES (:val) ";

    @Override
    public String help() {
        return Config.PREFIX + getMainTrigger() + " [n m]\n#Stress test the database with n threads each doing m operations. Results will be shown after max 10 minutes.";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) {
        return invoke(commandInfo.event.getTextChannel(), commandInfo.event.getMember(), commandInfo.args);
    }

    public boolean invoke(final TextChannel channel, final Member invoker, final String[] args) {

        boolean result = false;

        int t = 20;
        int o = 2000;
        if (args.length > 1) {
            t = Integer.parseInt(args[0]);
            o = Integer.parseInt(args[1]);
        }
        final int threads = t;
        final int operations = o;
        if (channel != null && invoker != null) {
            Wolfia.handleOutputMessage(channel, "%s, beginning stress test with %s threads each doing %s operations.",
                    TextchatUtils.userAsMention(invoker.getUser().getIdLong()), threads, operations);
        }

        prepareStressTest();
        final long started = System.currentTimeMillis();
        final Result[] results = new Result[threads];
        final Throwable[] exceptions = new Throwable[threads];

        for (int i = 0; i < threads; i++) {
            results[i] = Result.WORKING;
            new StressTestThread(i, operations, results, exceptions).start();
        }

        //wait for when it's done and report the results
        final int maxTime = 600000; //give it max 10 mins to run
        final int sleep = 10; //ms
        final int maxChecks = maxTime / sleep;
        int c = 0;
        while (!doneYet(results) || c >= maxChecks) {
            c++;
            try {
                Thread.sleep(sleep);
            } catch (final InterruptedException e) {
                //duh
            }
        }

        final StringBuilder out = new StringBuilder("`DB stress test results:");
        for (int i = 0; i < results.length; i++) {
            out.append("\nThread #").append(i).append(": ");
            if (results[i] == Result.WORKING) {
                out.append("failed to get it done in ").append(maxTime / 1000).append(" seconds");
                result = false;
            } else if (results[i] == Result.FAILED) {
                exceptions[i].printStackTrace();
                out.append("failed with an exception: ").append(exceptions[i].toString());
                result = false;
            } else if (results[i] == Result.SUCCESS) {
                out.append("successful");
                result = true;
            }
        }
        out.append("\n Time taken: ").append(System.currentTimeMillis() - started).append("ms for ")
                .append(threads * operations).append(" requested operations.`");
        log.info(out.toString());
        if (channel != null && invoker != null) {
            Wolfia.handleOutputMessage(channel, TextchatUtils.userAsMention(invoker.getUser().getIdLong()) + "\n" + out);
        }

        return result;
    }

    private boolean doneYet(final Result[] results) {
        for (final Result result : results) {
            if (result == Result.WORKING) {
                return false;
            }
        }
        return true;
    }

    private void prepareStressTest() {
        //drop and recreate the test table
        final EntityManager em = Wolfia.getDbWrapper().unwrap().getEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery(this.DROP_TEST_TABLE).executeUpdate();
            em.createNativeQuery(this.CREATE_TEST_TABLE).executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    private class StressTestThread extends Thread {

        private final int number;
        private final int operations;
        private final Result[] results;
        private final Throwable[] exceptions;


        StressTestThread(final int number, final int operations, final Result[] results, final Throwable[] exceptions) {
            super(StressTestThread.class.getSimpleName() + "-" + number);
            this.number = number;
            this.operations = operations;
            this.results = results;
            this.exceptions = exceptions;
        }

        @Override
        public void run() {
            boolean failed = false;
            EntityManager em = null;
            try {
                for (int i = 0; i < this.operations; i++) {
                    em = Wolfia.getDbWrapper().unwrap().getEntityManager();
                    try {
                        em.getTransaction().begin();
                        em.createNativeQuery(DbTestCommand.this.INSERT_TEST_TABLE)
                                .setParameter("val", ThreadLocalRandom.current().nextInt())
                                .executeUpdate();
                        em.getTransaction().commit();
                    } finally {
                        em.close(); //go crazy and request and close the EM for every single operation, this is a stress test after all
                    }
                }
            } catch (final Exception e) {
                this.results[this.number] = Result.FAILED;
                this.exceptions[this.number] = e;
                failed = true;
                if (em != null)
                    em.close();
            }

            if (!failed)
                this.results[this.number] = Result.SUCCESS;
        }
    }
}
