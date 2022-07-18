/************************************************
* findreplace_gui.scala
************************************************/
// ü

/* List of softmarkers:
*
* #FindreplaceGui END: 720
* #version: 54
End of list of softmarkers:*/

package de.jvr

import scala.xml._
import scala.io.Source
import scala.util.matching._
import scala.util.matching.Regex.Match
import scala.util.Try

import scala.swing._
import scala.swing.event._
import scala.swing.GridBagPanel._
import scala.swing.Dialog._

import scala.actors._
import scala.actors.Actor._

import scala.sys.process._ 
import scala.xml.XML
import scala.language.postfixOps
import scala.language.reflectiveCalls

import java.io.{Console=>_,_}
import java.util.Date
import java.util.Calendar
import java.text.SimpleDateFormat
import javax.swing.UIManager
import java.awt.Insets
import java.awt.Font

import java.awt.Toolkit
import java.awt.datatransfer._

import javax.swing.SwingUtilities

import ActorMessages._

import FindreplaceHelper._


object FindreplaceGui {
	val progname = "findreplace"
	val progurl = "https://github.com/jvr-ks/findreplace"
	val updateUrl = "https://github.com/jvr-ks/findreplace"

//#version
	val version = "0.937"

	val urlVersion = "http://www.jvr.de/tools/versions"

	val urlUpdate = "http://www.jvr.de/content/UPDATE_" + progname + ".exe"
	val pathDefault = "C:\\Program Files (x86)\\" + progname
	
	val urlOnlineHelpDefault = "http://www.jvr.de/tools/findreplace"
	var urlOnlineHelp = urlOnlineHelpDefault


	val newline = sys.props("line.separator")
	val pathSeparator = java.io.File.separator

	val homepath = System.getProperty("user.home") + pathSeparator + progname + pathSeparator

	val configFileName = "findreplaceconfig.xml"

	val guiConfigPath = homepath + configFileName


	var ta0Text = ""
	var ta1Text = ""
	var ta2Text = ""
	var ta3Text = ""
	var sourceFilename = ""
	var sourceUrl = false
	var controlFilename = ""
	var targetFilename = ""

	val urlJvrShow = "www.jvr.de"

	var actorrun = true

	val fontDefaultName = "sans-serif"
	val fontDefaultType = Font.PLAIN
	val fontDefaultSize = 14
	val fontDefault = new Font(fontDefaultName, fontDefaultType, fontDefaultSize)
	var fontSize = fontDefaultSize

	var fontsize3 = 3
	var fontsize2 = fontsize3 - 1

	val menuFontDefaultName = "sans-serif"
	val menuFontDefaultType = Font.PLAIN
	val menuFontDefaultSize = 14
	var menufontsize = menuFontDefaultSize
	val menuFontDefault = new Font(menuFontDefaultName, menuFontDefaultType, menufontsize)

	var commandfile = ""

	var filename = ""
	var fileencoding = "UTF-8"
	var targetname = ""
	var targetencoding = "UTF-8"

	var nobackup = false
	var hastarget = false
	var autoexit = false

	var saved = false
	var clipboard = false
	var autocodec = false
	var sound = true
	var configpath = ""
	var silent = false
// ---------------------------
	val frameMain = new MainFrame(){

		val screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize()

		title = progname + version + " by jvr"
		iconImage = java.awt.Toolkit.getDefaultToolkit.getImage(progname + ".png")
		maximumSize = new java.awt.Dimension(screenSize.width, screenSize.height)
		minimumSize = new java.awt.Dimension(screenSize.width / 4, screenSize.height / 4)
		location = new java.awt.Point(0, 0)

		try {
			for (laf <- UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(laf.getName()) ) {
					UIManager.setLookAndFeel(laf.getClassName())
				}
			}
		} catch {
			case e:Throwable => {
				Dialog.showMessage(null, "LookAndFeel problem: " + e)
			}
		}

		val menufont1 = new Font(menuFontDefaultName, menuFontDefaultType, menufontsize)
		val menufont2 = new Font(menuFontDefaultName, menuFontDefaultType, menufontsize)
		UIManager.put("Menu.font", menufont1)
		UIManager.put("MenuItem.font", menufont2)

		val OS = System.getProperty("os.name").toLowerCase()
		val isWindows = (OS.indexOf("win") >= 0)

		val menuitemsize = new java.awt.Dimension(80, 40)

		var cmd: Seq[String] = null

		menuBar = new MenuBar{
			contents += new Menu("File"){
				maximumSize = menuitemsize
				contents += new MenuItem(Action("Open Source-File in external Editor (editor.bat / editor.sh)"){
						if (isWindows && !sourceUrl) cmd = Seq("C:\\Program Files (x86)\\" + progname + "\\editor.bat", sourceFilename)
						else cmd = Seq("editor.sh", sourceFilename)
						val pb = Process(cmd)
						if (sourceFilename != "") {
							try {
								val p = pb.run
							} catch {
								case e:Throwable => println("Cannot start external Editor: " + e)
							}
						}
				})

				contents += new MenuItem(Action("Open Control-File in external Editor (editor.bat / editor.sh)"){
						if (isWindows) cmd = Seq("C:\\Program Files (x86)\\" + progname + "\\editor.bat", controlFilename)
						else cmd = Seq("editor.sh", controlFilename)
						val pb = Process(cmd)
						if (controlFilename != ""){
							try {
								val p = pb.run
							} catch {
								case e:Throwable => println("Cannot start external Editor: " + e)
							}
						}
				})

				contents += new MenuItem(Action("Open Target-File in external Editor (editor.bat / editor.sh)"){
						if (isWindows) cmd = Seq("C:\\Program Files (x86)\\" + progname + "\\editor.bat", targetFilename)
						else cmd = Seq("editor.sh", targetFilename)
						val pb = Process(cmd)
						if (targetFilename != ""){
							try {
								val p = pb.run
							} catch {
								case e:Throwable => println("Cannot start external Editor: " + e)
							}
						}
				})

				contents += new MenuItem(Action("Open Config-File (global version only) in Editor (editor.bat /~.sh)"){

						if (isWindows) {
							cmd = Seq("C:\\Program Files (x86)\\" + progname + "\\editor.bat", guiConfigPath)
						} else {
							cmd = Seq("editor.sh", guiConfigPath)
						}
						val pb = Process(cmd)

						try {
							val p = pb.run
						} catch {
							case e:Throwable => println("Cannot start external Editor: " + e)
						}
				})
			}

			contents += new Menu("Help"){

				contents += new MenuItem(Action("Help") {
						FindreplaceHelpGui.frameHelp.open()
				})

				contents += new MenuItem(Action("Info") {
						Dialog.showMessage(null, Info.info)
				})

			}

			contents += new Menu("Quit"){
				contents += new MenuItem("Quit"){
					mnemonic = Key.Q
					action = new Action("quit"){
						def apply(){
							stop_all()
						}
					}
				}
			}
		}

		listenTo(this)
		reactions += {
			case e:WindowClosing => {
				stop_all()
			}
			case x => ;//println(x)
		}

	} // END frame
// ---------------------------
// ---------------------------
	var lastkey = Key.Up

	val gbp0 = new GridBagPanel() {

		val ta0 = new TextArea(""){
			font = new Font(fontDefaultName, fontDefaultType, fontDefaultSize)
		}
		ta0.editable = false

		val caret = ta0.peer.getCaret().asInstanceOf[javax.swing.text.DefaultCaret]
		caret.setUpdatePolicy(javax.swing.text.DefaultCaret.ALWAYS_UPDATE)


		val c_ta0 = new Constraints
		c_ta0.gridx = 0
		c_ta0.gridy = 0
		c_ta0.weightx = 1;
		c_ta0.weighty = 1;
		c_ta0.insets = new Insets(5,5,5,5)
		c_ta0.fill = GridBagPanel.Fill.Both

		add(ta0, c_ta0)
	}

	val gbp1 = new GridBagPanel() {
		focusable = true

// Control-File: editable
		val ta1 = new TextArea(""){
			font = new Font(fontDefaultName, fontDefaultType, fontDefaultSize)

			listenTo(this.mouse.wheel, this.keys)
			reactions += {

				case KeyTyped(_,_,_,_) => {
					if(text != "") {
						val file = new File(controlFilename)
						val pw = new java.io.PrintWriter(file)
						pw.print(text)
						pw.close()
					}
				}

				case KeyPressed(_, v, m,_) => {
					lastkey = v
				}

				case KeyReleased(_, v, m,_) => lastkey = Key.Up

				case e:MouseWheelMoved => {
					val m = e.rotation
					if (m == -1 && isControl() && !isAlt()) {
						fontSize += 1
						font = new Font(fontDefaultName, fontDefaultType, fontSize)
					}

					if (m == 1 && isControl() && !isAlt()) {
						fontSize -= 1
						font = new Font(fontDefaultName, fontDefaultType, fontSize)
					}
				}

				case x => ;//println(x)
			}

		}
		ta1.editable = true

		val caret = ta1.peer.getCaret().asInstanceOf[javax.swing.text.DefaultCaret]
		caret.setUpdatePolicy(javax.swing.text.DefaultCaret.ALWAYS_UPDATE)

		val c_ta1 = new Constraints
		c_ta1.gridx = 0
		c_ta1.gridy = 0
		c_ta1.weightx = 1;
		c_ta1.weighty = 1;
		c_ta1.insets = new Insets(5,5,5,5)
		c_ta1.fill = GridBagPanel.Fill.Both
		add(ta1, c_ta1)

	}
	val gbp2 = new GridBagPanel() {

		val ta2 = new TextArea(""){
			font = new Font(fontDefaultName, fontDefaultType, fontDefaultSize)
		}
		ta2.editable = false

		val caret = ta2.peer.getCaret().asInstanceOf[javax.swing.text.DefaultCaret]
		caret.setUpdatePolicy(javax.swing.text.DefaultCaret.ALWAYS_UPDATE)


		val c_ta2 = new Constraints
		c_ta2.gridx = 0
		c_ta2.gridy = 0
		c_ta2.weightx = 1;
		c_ta2.weighty = 1;
		c_ta2.insets = new Insets(5,5,5,5)
		c_ta2.fill = GridBagPanel.Fill.Both
		add(ta2, c_ta2)

	}

	val gbp3 = new GridBagPanel() {

		val ta3 = new TextArea(""){
			font = new Font(fontDefaultName, fontDefaultType, fontDefaultSize)
		}
		ta3.editable = false

		val caret = ta3.peer.getCaret().asInstanceOf[javax.swing.text.DefaultCaret]
		caret.setUpdatePolicy(javax.swing.text.DefaultCaret.ALWAYS_UPDATE)

		val c_ta3 = new Constraints
		c_ta3.gridx = 0
		c_ta3.gridy = 0
		c_ta3.weightx = 1;
		c_ta3.weighty = 1;
		c_ta3.insets = new Insets(5,5,5,5)
		c_ta3.fill = GridBagPanel.Fill.Both
		add(ta3, c_ta3)
	}

	val sp0 = new ScrollPane() {
		contents = gbp0
	}
	val sp1 = new ScrollPane() {
		contents = gbp1
	}
	val sp2 = new ScrollPane() {
		contents = gbp2
	}

	val sp3 = new ScrollPane() {
		contents = gbp3
	}
	val tbp = new TabbedPane {
		pages += new TabbedPane.Page("Source-File", sp0)
		pages += new TabbedPane.Page("Control-File", sp1)
		pages += new TabbedPane.Page("Control-Log", sp2)
		pages += new TabbedPane.Page("Target-File", sp3)
	}


	val gbp = new GridBagPanel() {

		val c_label_copy = new Constraints
		c_label_copy.gridx = 0
		c_label_copy.gridy = 0
		c_label_copy.fill = GridBagPanel.Fill.Horizontal

		val label_copy = new Label("<html>&copy; by jvr 2013/2015 <a href='" + progurl +"'>" + urlJvrShow + "</a></html>")
		add(label_copy, c_label_copy)

		val c_tbp = new Constraints
		c_tbp.gridx = 0
		c_tbp.gridy = 1
		c_tbp.weightx = 1.0
		c_tbp.weighty = 1.0
		c_tbp.fill = GridBagPanel.Fill.Both


		add(tbp, c_tbp)

		val c_button_close = new Constraints
		c_button_close.gridx = 0
		c_button_close.gridy = 2
		c_button_close.gridwidth = 1
		c_button_close.fill = GridBagPanel.Fill.Horizontal

		add(
			Button("<html><font size=\"5\" color=\"red\">Quit</font></html>") {
				stop_all()
			},
			c_button_close
		)


		def isLabelClicked(label: scala.swing.Label, e: java.awt.Point,tolerance: Int): Boolean = {
			val width:Int = label.bounds.x + label.bounds.width
			val height:Int = label.bounds.y + label.bounds.height
			if (label.bounds.x - tolerance < e.x && label.bounds.y - tolerance < e.y && width + tolerance > e.x && height + tolerance > e.y) true else false
		}

		listenTo(this, label_copy, mouse.clicks, mouse.wheel, keys)

		reactions += {
			case e:MouseClicked => if(isLabelClicked(label_copy, e.point, 20)) openURL(progurl)

			case x => ;//println(x)
		}

	}

	frameMain.contents = gbp
	gbp1.ta1.requestFocus()
	tbp.selection.index = 3

	val guiUpdateActor = new Actor {
		override val scheduler = new SchedulerAdapter {
			def execute(fun: => Unit) { Swing.onEDT(fun) }
		}
		start()

		def act = {
			loopWhile(actorrun) {
				react {
					case Ta0_add(t: String) => {
						ta0Text += t
						gbp0.ta0.text = ta0Text
					}

					case Ta0_replace(t: String) => {
						gbp0.ta0.text = t
						gbp0.ta0.caret.dot = 0
					}

					case Ta1_add(t: String) => {
						ta1Text += t
						gbp1.ta1.text = ta1Text
					}

					case Ta1_replace(t: String) => {
						gbp1.ta1.text = t
						gbp1.ta1.caret.dot = 0
					}

					case Ta2_add(t: String) => {
						ta2Text += t
						gbp2.ta2.text = ta2Text
					}

					case Ta2_replace(t: String) => {
						gbp2.ta2.text = t
						gbp2.ta2.caret.dot = 0
					}

					case Ta3_add(t: String) => {
						ta3Text += t
						gbp3.ta3.text = ta3Text
					}

					case Ta3_replace(t: String) => {
						gbp3.ta3.text = t
						gbp3.ta3.caret.dot = 0
					}

					case Show(t: String) => {
						Dialog.showMessage(null, t)
					}

					case SourceFilename(fn: String) => sourceFilename = fn

					case SourceUrl => sourceUrl = true

					case ControlFilename(fn: String) => controlFilename = fn

					case TargetFilename(fn: String) => targetFilename = fn

					case CENTER => frameMain.centerOnScreen

					case Location(x: Int, y: Int ) => frameMain.location = new java.awt.Point(x, y)

					case Resize(x: Int, y: Int ) => frameMain.size = new java.awt.Dimension(x, y)

					case STOP => actorrun = false

					case CHECKVERSION => {
						try {
// check major
							val html = Source.fromURL(urlVersion).mkString
							val vmatcherREX = """<li>(\w+).*Version\s(\d*.\d*)\s\(major\)</li>""".r
							val a = vmatcherREX findAllIn html

							for (s <- a) {
								s match {
									case vmatcherREX(name, wwwversion) => {

										if (name == progname && wwwversion.toDouble > version.toDouble){

											val options = List("Findreplace 32Bit", "Findreplace Jar", "Cancel")

											val res = Dialog.showOptions(null, "<html><font size=\"" + fontsize3 + "\" color=\"red\">A new major release of " + progname + " is available!</font><br>Update-Download should be startet by your browser.<br>Please reinstall " + progname + "!</html>", "Findreplace Download", optionType = Dialog.Options.YesNoCancel, entries = options, initial = 0)

											res match {
												case Result.Yes => openURL(updateUrl)
												case Result.No => 
												case _ => ;
											}
										} else {
											;
										}
									}

									case _=> ;
								}
							}
						} catch {
							case e:Throwable => Dialog.showMessage(null, "Get Version not possible: " + e); // do nothing
						}

// check minor
						try {
							val html = Source.fromURL(urlVersion).mkString
							val vmatcherREX = """<li>(\w+).*Version\s(\d*.\d*)\s\(minor\)</li>""".r
							val a = vmatcherREX findAllIn html

							for (s <- a) {
								s match {
									case vmatcherREX(name, wwwversion) => {

										if (name == progname && wwwversion.toDouble > version.toDouble){

											val options = List("Findreplace 32Bit", "Findreplace Jar", "Cancel")

											val res = Dialog.showOptions(null, "<html><font size=\"" + fontsize3 + "\" color=\"red\">A new minor release of " + progname + " is available, start installer (Findreplace will be closed!):</font></html>", "Findreplace Update", optionType = Dialog.Options.YesNoCancel, entries = options, initial = 0)

											res match {
												case Result.Yes => findreplace_update("UPDATE_findreplace.exe", "C:\\Program Files (x86)\\" + progname + "\\findreplace.exe")
												case Result.No => findreplace_update("INSTALL_findreplace.sh", "fr.sh")
												case _ => ;
											}
										} else {
											;
										}
									}

									case _=> 
								}
							}
						} catch {
							case e:Throwable => Dialog.showMessage(null, "Get Version not possible: " + e); // do nothing
						}
					}

					case SELECTINDEX(i: Int) => Swing.onEDT(tbp.selection.index = i)

					case SCROLLDOWN(i: Int) => {
						if(i == 0) Swing.onEDT(gbp0.ta0.caret.dot = (gbp0.ta0.peer.getDocument().getLength()))
						if(i == 1) Swing.onEDT(gbp1.ta1.caret.dot = (gbp1.ta1.peer.getDocument().getLength()))
						if(i == 2) Swing.onEDT(gbp2.ta2.caret.dot = (gbp2.ta2.peer.getDocument().getLength()))
						if(i == 3) Swing.onEDT(gbp3.ta3.caret.dot = (gbp3.ta3.peer.getDocument().getLength()))
					}

					case e => println("Got message: " + e)

				}
			}
		}
	}
// ---------------------------
	def findreplace_update(updatefilename: String, restartfile: String) = {

		var batchfilename = ""

		val OS = System.getProperty("os.name").toLowerCase()
		val isWindows = (OS.indexOf("win") >= 0)

		if (isWindows){
//Batchdatei erstellen
			batchfilename = "C:\\jvrData\\" + progname + "\\startupdate.bat"
			var script = "@rem Generated by findreplace. Do not edit!\n"
			script += "@call " + updatefilename + " \n"
			script += "@\"" + restartfile + "\"\n"
			script += "@exit\n\n"

			Try{
				val out = new PrintWriter( batchfilename , "ISO-8859-1")
				out.print( script )
				out.close()
			}
		} else {
//Batchdatei erstellen
			batchfilename = "startupdate.sh"
			var script = "@rem Generated by findreplace. Do not edit!\n"
			script += updatefilename + " \n"
			script += "@\"" + restartfile + "\"\n"
			script += "@exit\n\n"

			Try{
				val out = new PrintWriter( batchfilename , "ISO-8859-1")
				out.print( script )
				out.close()
			}
		}

		val u = new java.net.URL(urlUpdate)
		val uc = u.openConnection()
		uc.connect()
		val in = uc.getInputStream()

		val out = new FileOutputStream(new File("C:\\jvrData\\" + progname + "\\" + updatefilename))

		val t = new Thread(new Runnable() {
				def run() {
					try {
						val buffer = new Array[Byte](16384)

						def doStream(total: Int = 0): Int = {
							val n = in.read(buffer)
							if (n == -1){
								total
							} else {
								out.write(buffer, 0, n)
								doStream(total + n)
							}
						}

						doStream()

					} catch {
						case e:Throwable => Dialog.showMessage(null, "Program-update is not possible: " + e)
					}
					out.close()
					in.close()

					val options = List("Ja","Nein")

					val res = Dialog.showOptions(null, "<html><font size=\"" + fontsize3 + "\" color=\"red\">Minor update ready to install, install now (Files will be saved)?</font></html>", progname + " Update", optionType = Dialog.Options.YesNo, entries = options, initial = 0)

					var startupdate = false
					res match {
						case Result.Yes => startupdate = true
						case Result.No => startupdate = false
						case _ => ;
					}

					if (startupdate) {
						val cmd = Seq(batchfilename)
						val pb = Process(cmd)

						try {
							val p = pb.run
							stop_all()
						} catch {
							case e:Throwable => Dialog.showMessage(null, "Cannot start external Process: " + e)
						}
					}
				}
		})
		t.setDaemon(true);
		t.start();
	}
// ---------------------------
	def openURL(url: String) = {
		try {
			val desktop = java.awt.Desktop.getDesktop()
			desktop.browse(new java.net.URI(url))
		} catch {
			case _:Throwable => "Error opening browser"
		}
		""
	}
// ---------------------------
	def stop_all():Unit = {
		Sound.play ! STOP
		guiUpdateActor ! STOP
		FindreplaceHelpGui.stop_all()
		frameMain.dispose
	}
// ---------------------------
	def isControl(): Boolean = {
		if (lastkey == Key.Control) true else false
	}
// ---------------------------
	def isAlt(): Boolean = {
		if (lastkey == Key.Alt) true else false
	}
// ---------------------------
// ---------------------------
// ---------------------------



} //#FindreplaceGui END