package de.npstr.wolfia;

/**
 * Created by npstr on 22.08.2016
 */

import de.npstr.wolfia.utils.Sneaky;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.hooks.ListenerAdapter;

public class Wolfia extends ListenerAdapter {

    private static JDA jda;

    public static void main(String[] args) {

        try {

            jda = new JDABuilder().setBotToken(Sneaky.DISCORD_TOKEN).buildBlocking();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}