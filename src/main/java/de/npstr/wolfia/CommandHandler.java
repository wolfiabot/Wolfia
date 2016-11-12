package de.npstr.wolfia;

import de.npstr.wolfia.utils.CommandParser;

/**
 * Created by npstr on 05.11.2016
 */
public interface CommandHandler {

    public void handleCommand(CommandParser.CommandContainer cmd);

}
