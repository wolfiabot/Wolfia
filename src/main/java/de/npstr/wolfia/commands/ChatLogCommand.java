package de.npstr.wolfia.commands;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.CommandListener;
import de.npstr.wolfia.Main;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by npstr on 25.09.2016
 */
public class ChatLogCommand extends Command {

    public final static String COMMAND = "chatlog";
    private final String HELP = "```usage: " + getListener().getPrefix()
            + COMMAND + " <days>\nto receive the log of this channel for the last <days> as a txt file\n" +
            "<days> being no bigger than " + Integer.MAX_VALUE + "\n\n" +
            "You need to be authorized to use this command!```";
    private static final Logger LOG = LogManager.getLogger();


    public ChatLogCommand(CommandListener listener) {
        super(listener);
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        if (args.length < 1)
            return false;
        try {
            Integer.valueOf(args[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        if (!Main.hasChatLogAuth(event.getAuthor())) return false;
        int days = Integer.valueOf(args[0]);

        Main.handleOutputMessage(event.getAuthor().getId(), "Received your order: chatlog for channel " + event.getTextChannel().getName() + " over the last " + days + " days.\n" +
                "Please be patient while I collect all the juicy messages for you.");

        Runnable collectAndSendChatlog = () -> {

            LOG.trace("runnable to collect and send chatlog started");

            Calendar cal = new GregorianCalendar();
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.add(Calendar.DAY_OF_MONTH, -days);
            long noOlderThan = cal.getTimeInMillis();

            MessageChannel ch = event.getTextChannel();
            MessageHistory mh = ch.getHistory();

            List<Message> collectingMessages = new ArrayList<>();
            LOG.trace("collecting messages now");

            while (true) {
                List<Message> messages = mh.retrieve();
                if (messages == null) break;

                for (Message m : messages) {
                    if (m.getTime().toEpochSecond() * 1000 > noOlderThan) {
                        collectingMessages.add(0, m);
                    } else break;
                }
            }
            LOG.trace("done collecting messages, writing the log file");

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd yyyy hh:mm:ss");
            try (PrintWriter writer = new PrintWriter("chatlog.txt", "UTF-8")) {
                for (Message m : collectingMessages) {
                    try {
                        String line = "[" + m.getTime().format(dtf) + "] ";
                        line += m.getAuthor().getUsername() + ": ";
                        line += m.getContent(); //TODO emotes, images get ignored currently, change that
                        writer.println(line);
                        //writer.println("[" + m.getTime().format(dtf) + "] " + m.getAuthor().getUsername() + ": " + m.getContent());
                    } catch (NullPointerException e) {
                        //this happens; for example a message that contains nothing but an uploaded picture may trigger this
                        LOG.warn("NullPointerException while writing the log; skipping this message; here, have a stacktrace:");
                        e.printStackTrace();
                    }
                }
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            LOG.trace("done writing the log file, uploading and sending");

            String out = "Your order, Sir. Chatlog for channel " + event.getTextChannel().getName() + " over the last " + days + " days, containing " + collectingMessages.size() + " messages.";
            event.getAuthor().getPrivateChannel().sendFile(new File("chatlog.txt"), new MessageBuilder().appendString(out).build());

            LOG.trace("runnable to collect and send chatlog done successfully, exiting");

        };
        Thread thread = new Thread(collectAndSendChatlog);
        thread.start();

        return true;
    }

    @Override
    public String help() {
        return HELP;
    }
}
