/************************************************
* sound.scala
************************************************/
// ü

package de.jvr

import scala.actors._
import scala.actors.Actor._
import scala.language.postfixOps

import ActorMessages._

import FindreplaceGui._

object Sound {
	var run = true
	
	var player: org.jfugue.Player = null
		
	try {
		player = new org.jfugue.Player()
	} catch {
		case e:Throwable => sound = false
	}

	val play = new Actor {
		def act = {
			loopWhile(run) {
				react {
					case PLAY(tone) => {
						if (sound) {
							player.play(tone)
						} else {
							if (sound) java.awt.Toolkit.getDefaultToolkit().beep()
						}
					}
					
					case STOP => run = false
					
					case e => println("Error: " + e);
				}
			}
		}
	}.start()	

}

