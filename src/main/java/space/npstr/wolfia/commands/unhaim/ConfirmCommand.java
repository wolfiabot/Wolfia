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
//package space.npstr.wolfia.commands.unhaim;
//
//import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
//import space.npstr.wolfia.Config;
//import space.npstr.wolfia.commands.meta.ICommand;
//import space.npstr.wolfia.game.Pregame;
//
///**
// * Created by npstr on 22.10.2016
// */
//public class ConfirmCommand implements ICommand {
//
//    public final static String COMMAND = "confirm";
//
//    private final Pregame pg;
//
//    public ConfirmCommand(final Pregame pg) {
//        this.pg = pg;
//    }
//
//    @Override
//    public boolean argumentsValid(final String[] args, final MessageReceivedEvent event) {
//        return true;
//    }
//
//    @Override
//    public boolean execute(final String[] args, final MessageReceivedEvent event) {
//        this.pg.confirm(event.getAuthor().getId());
//        return false;
//    }
//
//    @Override
//    public String help() {
//        return "```usage: " + Config.PREFIX + COMMAND + " \nto start the game. Game will only start if enough players "
//                + "have signed up\n";
//    }
//}
