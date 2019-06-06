/*
 * Copyright (C) 2017-2019 Dennis Neufeld
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

package space.npstr.wolfia.engine

import akka.actor.{Actor, ActorLogging, Props}
import space.npstr.wolfia.engine.Engine.StartGame

object Engine {

  case class StartGame()

  def props(): Props = Props(new Engine())
}

class Engine extends Actor with ActorLogging {

  log.debug("Engine initializing")

  override def receive: Receive = {
    case StartGame => createGame()
    case _ => throw new IllegalArgumentException("u what m8")
  }

  def createGame(): Unit = {

  }

}
