/************************************************
* findreplace_helper.scala
************************************************/
// ü

/* List of softmarkers:
*
* #createPath: 27
* #ect100: 31
* #ect89: 67
* #ect90: 96
* #ect91: 118
* #gracefulStop: 128
* #printANSI(filename: String, t: String, replace: Boolean = true): 80
* #printCODEC(filename: String, t: String, codec: String, replace: Boolean = true, sendjustsaved: Boolean = true): 102
* #printUTF8(filename: String, t: String, lineEndReplace: Boolean = true): Boolean: 50
* #showMemory(): 36
* #stop_all: 126
End of list of softmarkers:*/

package de.jvr

import scala.swing._
import scala.swing.event._
import scala.swing.GridBagPanel._
import scala.swing.Dialog._


import java.io.{Console=>_,_}
import java.nio.file._
import java.nio.file.Path

import ActorMessages._

import FindreplaceGui._


object FindreplaceHelper {

//#createPath #############################################################
	def createPath(directory: String) = {
		// oder mit Java nio: val dir = Files.createDirectories(Paths.get(directory))
		if (!new File(directory).exists() ){
			if (!(new File(directory)).mkdirs()) (s"Cannot create directory: $directory", "#ect100: 31") :: Findreplace.ect.messages //#ect100
		}
	}

/*
//#showMemory() #############################################################
	def showMemory(): Unit = {
		val runtime = Runtime.getRuntime()
		val mb = 1024 * 1024

		val memUsed = (runtime.totalMemory() - runtime.freeMemory()) / mb
		val memFree = runtime.freeMemory() / mb
		val memTotal = runtime.totalMemory() / mb
		val memMax = runtime.maxMemory() / mb
		val showMem = s"Used $memUsed MB, free $memFree MB, max $memMax MB"
					
		gbp.label_memfree.text = "<html><font size=\"" + colorLabelFontsizeSmall + "\">" + showMem + "</font></html>"
	}
*/

//#printUTF8(filename: String, t: String, lineEndReplace: Boolean = true): Boolean ##########
	def printUTF8(filename: String, t: String, lineEndReplace: Boolean = true) = {
		// sends message: JUSTSAVED (if not markers file)
		guiUpdateActor ! JUSTSAVED(new File(filename).getCanonicalPath())
		
		var r = true
		var out: BufferedWriter = null
		try {
			out = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(filename) , "UTF-8"))
			if (lineEndReplace){
				out.write( t.replace("\n", newline) )
			} else {
				out.write( t )
			}
		} catch {
			case e:Throwable => {
				r = false
				Swing.onEDT(Dialog.showMessage(null, "Error (#ect89: 67): " + e)) //#ect89
			}
		} finally {
			try {
				if (out != null) out.close()
			} catch {
				case e:Throwable => ;
			}
		}

		r
	}

//#printANSI(filename: String, t: String, replace: Boolean = true) ##########
	def printANSI(filename: String, t: String, replace: Boolean = true): Boolean = {
		// sends message: JUSTSAVED
		guiUpdateActor ! JUSTSAVED(new File(filename).getCanonicalPath())

		var r = true
		var out: BufferedWriter = null
		try {
			out = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(filename) , "ISO8859-1"))
			if (replace) out.write( t.replace("\n", newline) )
			if (!replace) out.write( t )
			if(out != null) out.close()
		} catch {
			case e:Throwable => {
				if(out != null) out.close()
				r = false
				Swing.onEDT(Dialog.showMessage(null, "Error (#ect90: 96): " + e)) //#ect90
			}
		}
		r
	}

//#printCODEC(filename: String, t: String, codec: String, replace: Boolean = true, sendjustsaved: Boolean = true) 
	def printCODEC(filename: String, t: String, codec: String, replace: Boolean = true, sendjustsaved: Boolean = true): Boolean = {
		// sends message: JUSTSAVED
		if (sendjustsaved) guiUpdateActor ! JUSTSAVED(new File(filename).getCanonicalPath())

		var r = true
		var out: BufferedWriter = null
		try {
		out = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(filename) , codec))
			if (replace) out.write( t.replace("\n", newline) )
			if (!replace) out.write( t )
			if(out != null) out.close()
		} catch {
			case e:Throwable => {
				if(out != null) out.close()
				r = false
				Swing.onEDT(Dialog.showMessage(null, "Error (#ect91: 118): " + e)) //#ect91
			}
		}
		r
	}

/************************************************************************************************************
* 
* 	//#stop_all ###############################################################--
* 	def stop_all(): Unit = {
* 			//#gracefulStop
* 		try {
* 			val stopped: Future[Boolean] = gracefulStop(dirwatcherActor, 3 seconds, SHUTDOWN)
* 			Await.result(stopped, 5 seconds)
* 		} catch {
* 			case e: akka.pattern.AskTimeoutException => println("dirwatcherActor wasn't stopped within 5 seconds")
* 		}
* 
* 		try {
* 			val stopped: Future[Boolean] = gracefulStop(soundActor, 3 seconds, SHUTDOWN)
* 			Await.result(stopped, 5 seconds)
* 		} catch {
* 			case e: akka.pattern.AskTimeoutException => println("soundActor wasn't stopped within 5 seconds")
* 		}
* 
* 		try {
* 			val stopped: Future[Boolean] = gracefulStop(previewActor, 3 seconds, SHUTDOWN)
* 			Await.result(stopped, 5 seconds)
* 		} catch {
* 			case e: akka.pattern.AskTimeoutException => println("previewActor wasn't stopped within 5 seconds")
* 		}
* 
* 		try {
* 			val stopped: Future[Boolean] = gracefulStop(guiUpdateActor, 3 seconds, SHUTDOWN)
* 			Await.result(stopped, 5 seconds)
* 		} catch {
* 			case e: akka.pattern.AskTimeoutException => println("guiUpdateActor wasn't stopped within 5 seconds")
* 		}
* 
* 		system.terminate()
* 		mainframe.dispose
* 		//println("SIMPED PROGRAMM-ENDE")
* 		sys.exit(0)
* 	}
************************************************************************************************************/
	
}