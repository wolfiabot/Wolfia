package de.npstr.wolfia.commands;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.CommandListener;
import de.npstr.wolfia.Main;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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

        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_MONTH, -days);
        long noOlderThan = cal.getTimeInMillis();

        MessageChannel ch = event.getTextChannel();
        MessageHistory mh = ch.getHistory();

        List<Message> collectingMessages = new ArrayList<>();

        while (true) {
            List<Message> messages = mh.retrieve();
            if (messages == null) break;

            for (Message m : messages) {
                if (m.getTime().toEpochSecond() * 1000 > noOlderThan) {
                    collectingMessages.add(0, m);
                } else break;
            }
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd yyyy hh:mm:ss");
        try (PrintWriter writer = new PrintWriter("log.txt", "UTF-8")) {
            for (Message m : collectingMessages) {
                writer.println("[" + m.getTime().format(dtf) + "] " + m.getAuthor().getUsername() + ": " + m.getContent());
            }
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String out = "Your order, Sir. 1x chatlog for channel " + event.getTextChannel().getName() + " over the last " + days + " days, containing " + collectingMessages.size() + " messages.";
        event.getAuthor().getPrivateChannel().sendFile(new File("log.txt"), new MessageBuilder().appendString(out).build());

        return true;
    }

    @Override
    public String help() {
        return HELP;
    }
}
