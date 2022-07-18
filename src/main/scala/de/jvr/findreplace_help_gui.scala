/************************************************
* 
************************************************/
// ü

package de.jvr

import scala.xml._
import scala.io.Source
import scala.util.matching._
import scala.util.matching.Regex.Match
import scala.util.Try

import scala.swing._
import scala.swing.event._
import scala.swing.GridBagPanel._

import scala.actors._
import scala.actors.Actor._

import scala.sys.process._ 
import scala.xml.XML

import java.io.{Console=>_,_}
import java.util.Date
import java.util.Calendar
import java.text.SimpleDateFormat
import javax.swing.UIManager
import java.awt.Insets
import java.awt.Font

import java.awt.Toolkit
import java.awt.datatransfer._

import ActorMessages._

// VIEW ------------------------------------------------------------------------------------------------------------------------ 
object FindreplaceHelpGui {
	
	var actorrun = true
	
	// ---------------------------
	def stop_all():Unit = {
		 guiActorHelp ! STOP
		 frameHelp.dispose
	}
	// ---------------------------
	val frameHelp = new Frame() {
		 
		 title = "Findreplace Help"
		 iconImage = java.awt.Toolkit.getDefaultToolkit.getImage("findreplace.png")
		 
		 try {
			 UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel)
		 } catch {
			 case _:Throwable => UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
		 }
		 
		 val screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize()
		 preferredSize = new java.awt.Dimension(screenSize.width / 4, screenSize.height / 4)
		 location = new java.awt.Point(0, 0)
		 
		 
		 val menuitemsize = new java.awt.Dimension(80, 40)
		 
		 var cmd: Seq[String] = null
		 
		 menuBar = new MenuBar{
			 contents += new MenuItem("Quit Help") {
				 mnemonic = Key.Q
				 action = new Action("Quit Help") {
					 def apply() {
						 stop_all()
					 }
				 }
			 }
		 }
		 
		 listenTo( this )
		 reactions += {
			 case e:WindowClosing => {
				 stop_all()
			 }
			 case x => ;//println( x )
		 }
		 
	} // END frame
	// ---------------------------
	def openURL(url: String) = {
		 try {
			 val desktop = java.awt.Desktop.getDesktop()
			 desktop.browse(new java.net.URI( url ))
		 } catch {
			 case _:Throwable => "Error opening browser"
		 }
		 ""
	}
	// ---------------------------
	var lastkey = Key.Up
	
	def isControl(): Boolean = {
		 if (lastkey == Key.Control) true else false
	}
	
	def isAlt(): Boolean = {
		 if (lastkey == Key.Alt) true else false
	}
	// ---------------------------
	
	val gbp0 = new GridBagPanel() {
		 focusable = true
		 
		 val helplabel = new Label(Help.helphtml) {}
		 
		 val c_helplabel = new Constraints
		 c_helplabel.gridx = 0
		 c_helplabel.gridy = 0
		 c_helplabel.weightx = 1;
		 c_helplabel.weighty = 1;
		 c_helplabel.insets = new Insets(5,5,5,5)
		 c_helplabel.fill = GridBagPanel.Fill.Both
		 add(helplabel, c_helplabel)
	}
	
	val sp0 = new ScrollPane() {contents = gbp0}
	frameHelp.contents = sp0
	
	
	val guiActorHelp = new Actor {
		 override val scheduler = new SchedulerAdapter {
			 def execute(fun: => Unit) { Swing.onEDT( fun ) }
		 }
		 start()
		 
		 def act = {
			 
			 loopWhile( actorrun ) {
				 react {
					 case CENTER => frameHelp.centerOnScreen
					 
					 case Location(x: Int, y: Int ) => frameHelp.location = new java.awt.Point(x, y)
					 
					 case Resize(x: Int, y: Int ) => frameHelp.size = new java.awt.Dimension(x, y)
					 
					 case STOP => actorrun = false
					 
					 case e => println("Got message: " + e)
				 }
			 }
		 }
	}
	
} // class FindreplaceHelpGui END