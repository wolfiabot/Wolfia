//package de.npstr.wolfia.commands;
//
//import com.gargoylesoftware.htmlunit.WebClient;
//import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
//import com.gargoylesoftware.htmlunit.html.HtmlDivision;
//import com.gargoylesoftware.htmlunit.html.HtmlPage;
//import Command;
//import CommandListener;
//import net.dv8tion.jda.events.message.MessageReceivedEvent;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import java.io.IOException;
//
///**
// * Created by npstr on 25.09.2016
// */
//public class RoleInfoCommand extends Command {
//
//    public final static String COMMAND = "role";
//    private final String HELP = "```usage: " + getListener().getPrefix()
//            + COMMAND + " <role>\nto have the bot post info from MUs mafia role database about the request <role>```";
//    private static final Logger LOG = LogManager.getLogger();
//
//
//    public RoleInfoCommand(CommandListener listener) {
//        super(listener);
//    }
//
//    @Override
//    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
//        if (args.length < 1) return false;
//        String role = "";
//        for (String s : args) {
//            role += s;
//        }
//        return !role.equals("");
//    }
//
//    @Override
//    public boolean execute(String[] args, MessageReceivedEvent event) {
//        String role = "";
//        for (String s : args) {
//            role += s + "%20";
//        }
//        role = role.substring(0, role.length() - 3);
//        LOG.trace("received role info request for " + role);
//
//        String roleUrl = "http://www.mafiauniverse.com/forums/database/mafia-roles/role/?mafiarole=" + role;
//
//        try (final WebClient webClient = new WebClient()) {
//            final HtmlPage page = webClient.getPage(roleUrl);
//            page.getByXPath("//div[@class=block")
//            final HtmlDivision div = page.getHtmlElementById("some_div_id");
//            final HtmlAnchor anchor = page.getAnchorByName("anchor_name");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//        return false;
//    }
//
//    @Override
//    public String help() {
//        return HELP;
//    }
//}
