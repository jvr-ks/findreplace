/************************************************
* findreplace.scala
************************************************/
// ü

/*
* Findreplace
*
* findreplace.scala
*
* verwendet noch Scala Actoren und Scala-Swing
*
* Lizenz: GPL siehe http://www.gnu.org/copyleft/gpl.html
*
* Copyright by jvr 2013
*
*/

/* List of softmarkers:
*
* #ect1: 243
* #ect10: 480
* #ect11: 539
* #ect12: 555
* #ect13: 571
* #ect14: 606
* #ect15: 821
* #ect16: 868
* #ect17: 887
* #ect18: 906
* #ect19: 936
* #ect2: 291
* #ect20: 954
* #ect21: 972
* #ect22: 1018
* #ect23: 1194
* #ect23: 1194
* #ect23: 1194
* #ect24: 1253
* #ect25: 1350
* #ect26: 1375
* #ect27: 1386
* #ect28: 1413
* #ect29: 1424
* #ect3: 329
* #ect30: 1442
* #ect31: 1488
* #ect32: 1509
* #ect33: 840
* #ect34: 1041
* #ect35: 1229
* #ect35: 1229
* #ect35: 1229
* #ect36: 990
* #ect37: 611
* #ect39: 1268
* #ect4: 357
* #ect40: 1283
* #ect5: 387
* #ect6: 405
* #ect7: 416
* #ect8: 453
* #ect9: 468
* #replaceextractonly: 1238
* #writeextractdateformat: 1274
* #writeextractdateformat: 1274
End of list of softmarkers:*/

package de.jvr

import scala.xml._
import scala.io.Source
import scala.util.matching._
import scala.util.matching.Regex.Match
import scala.util.Try
import scala.util.Success
import scala.util.Failure

import scala.swing._
import scala.swing.event._
import scala.swing.GridBagPanel._

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

import com.sun.jna.platform.win32._

import ActorMessages._

import FindreplaceHelper._
import FindreplaceGui._
import FindreplaceHelpGui._



//#object Findreplace #######################################################
object Findreplace {

	var exitcode = 0
	var args_cmd = new scala.collection.mutable.ArrayBuffer[String]
	val ect = new Ect(List(("","")))

	var control_out = ""
//commands as Array
	var commands: Array[String] = Array("")
//commands as String
	var commandlines = ""

	var fromURL = false

// Display
	val outbuffer = new scala.collection.mutable.ArrayBuffer[String]
	val frbuffer = new scala.collection.mutable.ArrayBuffer[Tuple3[String, String, Int]]

// Target-File
	var lines = ""
// if target already exists, replaceextractonly command
	var targetlines = ""
	var append = false
	var extractmode = false
	var extractToTargetMode = false

	val testresults = new scala.collection.mutable.ArrayBuffer[Tuple3[String, String, Boolean]]

	var extract = ""

	val setArray = new scala.collection.mutable.ArrayBuffer[Tuple3[String, String, String]]
	var hasSetParameters = false
	var countSetParameters = 0

	val nl = "\n"

//#main(args: Array[String]) ################################################
	def main(args: Array[String]):Unit = {

		val OS = System.getProperty("os.name").toLowerCase()
		val isWindows = (OS.indexOf("win") >= 0)
		val isWindows7 = (OS.indexOf("windows 7") >= 0)
		val isWindowsXP = (OS.indexOf("windows xp") >= 0)
		val isLinux = (OS.indexOf("linux") >= 0)

		val homepath = System.getProperty("user.home") + pathSeparator + progname + pathSeparator

		createPath(homepath)
		println("Data-Directory is: " + homepath)

// args -> args_cmd

		if (args.length > 0 ) {
			for (i <- 0 until args.length) args_cmd += args(i)
		}

		var configFile = homepath + configFileName

		if (new File(configFileName).exists){ // local config overwrites
			configFile = configFileName
		}

		if (! new File(configFile).exists){ // create default config if not existent
			val configTextDefault = """<root>
			<screen_position_x>0</screen_position_x>
			<screen_position_y>0</screen_position_y>
			<center_on_screen>false</center_on_screen>
			<height_pixel>900</height_pixel>
			<width_pixel>1024</width_pixel>
			<sound>true</sound>
			<checkversion>false</checkversion>
			<silent>false</silent>
			</root>
			"""
			printUTF8(configFile, configTextDefault)

		}

		try {
			val configXML = XML.loadFile(configFile)

			val check_version = Try((configXML \\ "checkversion").text.toBoolean) getOrElse true
			if (check_version) guiUpdateActor ! CHECKVERSION

			val screen_position_x = Try((configXML \\ "screen_position_x").text.toInt) getOrElse 0
			val screen_position_y = Try((configXML \\ "screen_position_y").text.toInt) getOrElse 0
			val center_on_screen = Try((configXML \\ "center_on_screen").text.toBoolean) getOrElse false

			val width_pixel = Try((configXML \\ "width_pixel").text.toInt) getOrElse 0
			val height_pixel = Try((configXML \\ "height_pixel").text.toInt) getOrElse 0

			if (width_pixel != 0 && height_pixel != 0) guiUpdateActor ! Resize(width_pixel, height_pixel)
			if (width_pixel != 0 && height_pixel != 0) guiActorHelp ! Resize(width_pixel, height_pixel)

			if (center_on_screen) {
				guiUpdateActor ! CENTER
				guiActorHelp ! CENTER

			} else {
				guiUpdateActor ! new Location(screen_position_x, screen_position_y)
				guiActorHelp ! new Location(screen_position_x, screen_position_y)
			}

			sound = Try((configXML \\ "sound").text.toBoolean) getOrElse true

			silent = Try((configXML \\ "silent").text.toBoolean) getOrElse false

		} catch {
			case e:Throwable => {
				guiUpdateActor ! Resize(FindreplaceGui.frameMain.screenSize.width / 4, FindreplaceGui.frameMain.screenSize.height / 4)
				guiActorHelp ! Resize(FindreplaceGui.frameMain.screenSize.width / 4, FindreplaceGui.frameMain.screenSize.height / 4)
				ect.messages = ("***** Warning: Error in File " + guiConfigPath + " " + e, "#ect1: 243") :: ect.messages //#ect1
			}
		}

// ---------------------------
// inline-functions
// ---------------------------
		def readparam(r: Boolean): Boolean = {
// args_cmd -> frbuffer, control, outbuffer

			var ret = true

			r match {
				case false => ret = false
				case true => {
					val findEX = "find=(.*)".r
					val replaceEX = "replace=(.*)".r
					val setEX = """set=(\S+?)~(\S+?)~(\S+?)""".r

					if (args_cmd.length > 0 ){

						for (i <- 0 until args_cmd.length) {
							val a = args_cmd(i)
							a match {
								case findEX(fd) => {
									frbuffer += Tuple3("find",fd,0)
									outbuffer += ("(Startparameter) find: " + fd)
								}
								case replaceEX(rp) => {
									frbuffer += Tuple3("replace",rp,0)
									outbuffer += ("(Startparameter) replace: " + rp)
								}
								case setEX(variable, value, filter) => {
									hasSetParameters = true
									setArray += Tuple3(variable, value, filter)
									countSetParameters += 1
								}

								case findEX(fd) => {
									frbuffer += Tuple3("find",fd,0)
									outbuffer += ("(Startparameter) find: " + fd)
								}

								case b => {
// ccommandfilename only allowed as first parameter
									if (i == 0){
										val file = new File(b)
										if(!file.exists()){
											ect.messages = ("***** Error: File " + b + " not found!", "#ect2: 291") :: ect.messages //#ect2
											ret = false
										} else {
											guiUpdateActor ! ControlFilename(b)
											commandfile = b
										}
									}
								}
							}
							guiUpdateActor ! new Ta2_replace(outbuffer.mkString("\n"))
						}
					}
				}
			}
			ret
		}
// ---------------------------
		def save(r: Boolean): Boolean = {
// control -> File-write

			var ret = true

			r match {
				case false => ret = false
				case true => {
					var name = ""
					val date = new SimpleDateFormat("yyy_MM_dd_HH_mm") format Calendar.getInstance.getTime

					var writer:PrintWriter = null
					try {
						name = filename + "." + date + ".tmp"
						writer = new PrintWriter(new File(name), fileencoding)
						writer.write(lines.replace("\n", sys.props("line.separator")))
						writer.close()
					} catch {
						case _:Throwable => {
							if (writer != null) Try{writer.close()}
							saved = false
							ect.messages = ("***** Error: Backup-File " + name + " not writable!", "#ect3: 329") :: ect.messages //#ect3
							ret = false
						}
					}
				}
			}
			ret
		}
// ---------------------------
		def readcommandfile(r: Boolean): Boolean = {
// File -> commands

			var ret = true

			r match {
				case false => ret = false
				case true => {
					val codec = Codec.guessCodec(commandfile)
					var c: Source = null

					try {
						c = Source.fromFile(commandfile, codec)
						commandlines = c.getLines().mkString("\n")
						commands = commandlines.split("\n")
						c.close()
					} catch {
						case _:Throwable => {
							if (c != null) Try{c.close()}
							ect.messages = ("***** Error: Control-File not readable!", "#ect4: 357") :: ect.messages //#ect4
							ret = false
						}
					}
				}
			}
			ret
		}
// ---------------------------
		def parseSetControlCommands(r: Boolean): Boolean = {
// Control-File -> set Command must be parsed before all others

			var ret = true

			r match {
				case false => false
				case true => {
					val setEX = """set=(\S+?)~(\S+?)~(\S+?)""".r

					for (ctrl <- commands) {
						ctrl match {
							case setEX(variable, value, filter) => {
								if (!hasSetParameters) setArray += Tuple3(variable, value, filter)
							}

							case _ => ;
						}
					}

					if (hasSetParameters && countSetParameters != setArray.length) {
						ect.messages = ("***** Error in: set= command: Incorrect number of command-line set-parameters!", "#ect5: 387") :: ect.messages //#ect5
						ret = false
					}

// replace "set=" Expressions
					for (i <- setArray){
// negativ lock before
						val replaceREX = new Regex("(?<!set=)" + i._1)

						val filterREX = new Regex(i._3)

						try {
							var i2 = ""
							i2 = filterREX.findFirstIn(i._2) getOrElse ""

							commandlines = replaceREX.replaceAllIn(commandlines, i2)
						} catch {
							case e:Throwable => {
								ect.messages = ("***** Error in: set= Command: " + e, "#ect6: 405") :: ect.messages //#ect6
								ret = false
							}
						}
					}

					commands = commandlines.split("\n")

					if ( commands.length > 0) {
						guiUpdateActor ! new Ta1_replace(commands.mkString("\n"))
					} else {
						ect.messages = ("***** Error: Control-File is empty!", "#ect7: 416") :: ect.messages //#ect7
						commands = Array("")
						ret = false
					}
				}
			}
			ret
		}
// ---------------------------
		def parsecontrolcommands(r: Boolean): Boolean = {
// Control-File -> control

			var ret = true

			r match {
				case false => false
				case true => {
					val commandEX = "command=(.*)".r
					val commentEX = "#(.*)".r
					val filenameEX = "source=(.*)".r
					val filenameUrlEX = "sourceURL=(.*)".r
					val filename1EX = "file=(.*)".r // deprecated
					val targetnameEX= "target=(.*)".r
					val fileencodingEX = "fileencoding=(.*)".r

					for (i <- 0 until commands.length) {

						commands(i) match {
							case commandEX(cmd) => {
								cmd match {
									case "nobackup" => nobackup = true
									case "autoexit" => autoexit = true
									case "clipboard" => clipboard = true
									case "sound" => sound = true
									case "nosound" => sound = false

									case a => {
										ect.messages = (" *** Warning, command: " + a + " not found!", "#ect8: 453") :: ect.messages //#ect8
									}
								}
								outbuffer += ("(" + i + ") Command: " + cmd)
							}

							case commentEX(cmt) => outbuffer += ("(" + i + ") # " + cmt)

							case filenameEX(fn) => {
								filename = fn
								if (!hastarget) targetname = fn
								outbuffer += ("(" + i + ") file: " + filename )
								guiUpdateActor ! SourceFilename(filename )
								val file = new File(filename)
								if(!file.exists()){
									ect.messages = ("***** Error: File " + filename + " not found!", "#ect9: 468") :: ect.messages //#ect9
									ret = false
								}
							}

							case filename1EX(fn) => {
								filename = fn
								if (!hastarget) targetname = fn
								outbuffer += ("(" + i + ") file: " + filename)
								guiUpdateActor ! SourceFilename(filename)
								val file = new File(filename)
								if(!file.exists()){
									ect.messages = ("***** Error: File " + filename  + " not found!", "#ect10: 480") :: ect.messages //#ect10
									ret = false
								}
							}

							case filenameUrlEX(fn) => {
								fromURL = true
								filename = fn
								if (!hastarget) targetname = "fromURL.txt"
								outbuffer += ("(" + i + ") file: " + filename)
								guiUpdateActor ! SourceUrl
							}

							case targetnameEX(tn) => {
								targetname = tn
								hastarget = true
								guiUpdateActor ! TargetFilename(targetname)
								outbuffer += ("(" + i + ") target: " + targetname)
							}

							case fileencodingEX(fe) => {
								fileencoding = fe.toUpperCase()

								if (fileencoding == "AUTO") {
									autocodec = true
								}

								outbuffer += ("(" + i + ") fileencoding: " + fileencoding)
							}

// ignore other
							case _ => ;
						}
					}
				}
			}
			ret
		}
// ---------------------------
		def readtextfile(r: Boolean): Boolean = {
// Source-> lines

			var ret = true

			r match {
				case false => ret = false
				case true => {
					if (!clipboard) {

						if (fromURL){
							try {
								fileencoding = "UTF-8"
								val s = Source.fromURL( filename, fileencoding )
								lines = s.mkString
								s.close()
								guiUpdateActor ! new Ta0_replace(lines)
							} catch {
								case _:Throwable => {
									ect.messages = ("***** Error: Cannot read sourceURL: " + filename, "#ect11: 539") :: ect.messages //#ect11
									lines = ""
									ret = false
								}
							} finally {}
						} else {
							var s: Source = null
							try {
								if (autocodec) fileencoding = Codec.guessCodec(filename)
								s = Source.fromFile(filename, fileencoding)
								lines = s.getLines().mkString("\n")
								s.close()
								guiUpdateActor ! new Ta0_replace(lines)
							} catch {
								case _:Throwable => {
									if (s != null) Try{s.close()}
									ect.messages = ("***** Error: Cannot read source-file (codec ok?)!" , "#ect12: 555") :: ect.messages //#ect12
									lines = ""
									ret = false
								}
							} finally {}
						}
					} else {
						val systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
						val transferData = systemClipboard.getContents( null )

						var s:Any = ""

						try {
							s = transferData.getTransferData( DataFlavor.stringFlavor )
						} catch {
							case e:Throwable => {
								ect.messages = ("***** Error: Clipboard-Data is not text!" , "#ect13: 571") :: ect.messages //#ect13
								lines = ""
								ret = false
							}
						}
						lines = "" + s
						guiUpdateActor ! new Ta0_replace(lines)
					}
				}
			}
			ret
		}
// ---------------------------
		def readtargetfile(r: Boolean): Boolean = {
// Target-File -> targetlines

			var ret = true

			if (!clipboard){
				if (hastarget){
					if (targetname != ""){
						var s: Source = null
						try {
							if (!new File( targetname ).exists){
								val out = new PrintWriter(targetname, "UTF-8")
								out.print( "" )
								out.close()
							}
							if (autocodec) targetencoding = Codec.guessCodec(targetname)
							s = Source.fromFile(targetname, targetencoding)
							targetlines = s.getLines().mkString("\n")
							s.close()
						} catch {
							case _:Throwable => {
								if (s != null) Try{s.close()}
								ect.messages = ("***** Error: Cannot read Target-File!", "#ect14: 606") :: ect.messages //#ect14
								ret = false
							}
						}
					} else {
						ect.messages = ("***** Error: Target-File not defined!", "#ect37: 611") :: ect.messages //#ect37
						ret = false
					}
				}
			}
			ret
		}
// ---------------------------
		def parsecommands(r: Boolean): Boolean = {
// commands -> frbuffer, outbuffer

			var ret = true

			r match {
				case false => ret = false
				case true => {

					val findEX = "find=(.*)".r
					val findg1EX = "findg1=(.*)".r
					val findg2EX = "findg2=(.*)".r
					val findg2rEX = "findg2r=(.*)".r
					val findg3EX = "findg3=(.*)".r
					val findfEX = "findf=(.*)".r
					val findcEX = "findc=(.*)".r
					val findtlEX = "findtl=(.*)".r
					val findtuEX = "findtu=(.*)".r
					val findccEX = "findcc=(.*)".r
					val findextractEX = "findextract=(.*)".r

					val replaceEX = "replace=(.*)".r
					val replaceg1EX = "replaceg1=(.*)".r
					val replaceg2EX = "replaceg2=(.*)".r
					val replaceg2rEX = "replaceg2r=(.*)".r
					val replaceg3EX = "replaceg3=(.*)".r
					val replacefEX = "replacef=(.*)".r
					val replacecEX = "replacec=(.*)".r
					val replacetlEX = "replacetl=(.*)".r
					val replacetuEX = "replacetu=(.*)".r
					val replaceccEX = "replacecc=(.*)".r
					val writeextractEX = "writeextract=(.*)".r
					val writeextractcEX = "writeextractc=(.*)".r
					val writeextractaddEX = "writeextractadd=(.*)".r
					val writeextractaddcEX = "writeextractaddc=(.*)".r

					val writeextractgEX = "writeextractg=(.*)".r
					val writeextractgcEX = "writeextractgc=(.*)".r
					val writeextractgaddEX = "writeextractgadd=(.*)".r
					val writeextractgaddcEX = "writeextractgaddc=(.*)".r


					val replaceextractonlyEX = "replaceextractonly=(.*)".r
					val writeextractDateFormatEX = "writeextractdateformat=(.*)".r
					val writeextractDateaddEX = "writeextractdateadd=(.*)".r

					val counterreplaceEX = "counterreplace=(.*)".r
					val counterreplaceevenEX = "counterreplaceeven=(.*)".r
					val counterreplaceoddEX = "counterreplaceodd=(.*)".r

					val g3formatEX = "g3format=(.*)".r

					val beforeEX = "before=(.*)".r
					val behindEX = "behind=(.*)".r

					val beforennlEX = "beforennl=(.*)".r
					val behindnnlEX = "behindnnl=(.*)".r

					val beforeextractEX = "beforeextract=(.*)".r
					val behindextractEX = "behindextract=(.*)".r

					val testnameEX = "testname=(.*)".r
					val testequalsEX = "testequals=(.*)".r


					for (i <- 0 until commands.length) {

						commands(i) match {
							case findEX(fd) => {frbuffer += Tuple3("find",fd,i);outbuffer += ("(" + i + ") find: " + fd)}
							case replaceEX(rp) => {frbuffer += Tuple3("replace",rp,i);outbuffer += ("(" + i + ") replace: " + rp)}
							case findfEX(fd) => {frbuffer += Tuple3("findf",fd,i);outbuffer += ("(" + i + ") findf: " + fd)}
							case replacefEX(rp) => {frbuffer += Tuple3("replacef",rp,i);outbuffer += ("(" + i + ") replacef: " + rp)}
							case findcEX(fd) => {frbuffer += Tuple3("findc",fd,i);outbuffer += ("(" + i + ") findc: " + fd)}
							case replacecEX(rp) => {frbuffer += Tuple3("replacec",rp,i);outbuffer += ("(" + i + ") replacec: " + rp)}
							case findg1EX(fd) => {frbuffer += Tuple3("findg1",fd,i);outbuffer += ("(" + i + ") findg1: " + fd)}
							case replaceg1EX(rp) => {frbuffer += Tuple3("replaceg1",rp,i);outbuffer += ("(" + i + ") replaceg1: " + rp)}
							case findg2EX(fd) => {frbuffer += Tuple3("findg2",fd,i);outbuffer += ("(" + i + ") findg2: " + fd)}
							case replaceg2EX(rp) => {frbuffer += Tuple3("replaceg2",rp,i);outbuffer += ("(" + i + ") replaceg2: " + rp)}
							case findg2rEX(fd) => {frbuffer += Tuple3("findg2r",fd,i);outbuffer += ("(" + i + ") findg2r: " + fd)}
							case replaceg2rEX(rp) => {frbuffer += Tuple3("replaceg2r",rp,i);outbuffer += ("(" + i + ") replaceg2r: " + rp)}

							case findg3EX(fd) => {frbuffer += Tuple3("findg3",fd,i);outbuffer += ("(" + i + ") findg3: " + fd)}
							case replaceg3EX(rp) => {frbuffer += Tuple3("replaceg3",rp,i);outbuffer += ("(" + i + ") replaceg3: " + rp)}
							case g3formatEX(rp) => {frbuffer += Tuple3("g3format",rp,i);outbuffer += ("(" + i + ") g3format: " + rp)}

							case findtlEX(fd) => {frbuffer += Tuple3("findtl",fd,i);outbuffer += ("(" + i + ") findtl: " + fd)}
							case replacetlEX(rp) => {frbuffer += Tuple3("replacetl",rp,i);outbuffer += ("(" + i + ") replacetl: " + rp)}

							case findtuEX(fd) => {frbuffer += Tuple3("findtu",fd,i);outbuffer += ("(" + i + ") findtu: " + fd)}
							case findccEX(fd) => {frbuffer += Tuple3("findcc",fd,i);outbuffer += ("(" + i + ") findcc: " + fd)}
							case replacetuEX(rp) => {frbuffer += Tuple3("replacetu",rp,i);outbuffer += ("(" + i + ") replacetu: " + rp)}
							case replaceccEX(rp) => {frbuffer += Tuple3("replacecc",rp,i);outbuffer += ("(" + i + ") replacecc: " + rp)}
							case findextractEX(fd) => {frbuffer += Tuple3("findextract",fd,i);outbuffer += ("(" + i + ") findextract: " + fd)}
							case writeextractEX(rp) => {frbuffer += Tuple3("writeextract",rp,i);outbuffer += ("(" + i + ") writeextract: " + rp)}
							case writeextractcEX(rp) => {frbuffer += Tuple3("writeextractc",rp,i);outbuffer += ("(" + i + ") writeextractc: " + rp)}
							case writeextractaddEX(rp) => {frbuffer += Tuple3("writeextractadd",rp,i);outbuffer += ("(" + i + ") writeextractadd: " + rp)}
							case writeextractaddcEX(rp) => {frbuffer += Tuple3("writeextractaddc",rp,i);outbuffer += ("(" + i + ") writeextractaddc: " + rp)}

							case writeextractgEX(rp) => {frbuffer += Tuple3("writeextractg",rp,i);outbuffer += ("(" + i + ") writeextractg: " + rp)}
							case writeextractgcEX(rp) => {frbuffer += Tuple3("writeextractgc",rp,i);outbuffer += ("(" + i + ") writeextractgc: " + rp)}
							case writeextractgaddEX(rp) => {frbuffer += Tuple3("writeextractgadd",rp,i);outbuffer += ("(" + i + ") writeextractgadd: " + rp)}
							case writeextractgaddcEX(rp) => {frbuffer += Tuple3("writeextractgaddc",rp,i);outbuffer += ("(" + i + ") writeextractgaddc: " + rp)}


							case replaceextractonlyEX(rp) => {frbuffer += Tuple3("replaceextractonly",rp,i);outbuffer += ("(" + i + ") replaceextractonly: " + rp)}

							case writeextractDateFormatEX(rp) => {frbuffer += Tuple3("writeextractdateformat",rp,i);outbuffer += ("(" + i + ") writeextractdateformat: " + rp)}
							case writeextractDateaddEX(rp) => {frbuffer += Tuple3("writeextractdateadd",rp,i);outbuffer += ("(" + i + ") writeextractdateadd: " + rp)}

							case beforeEX(bf) => {frbuffer += Tuple3("before",bf,i);outbuffer += ("(" + i + ") before: " + bf)}
							case behindEX(bh) => {frbuffer += Tuple3("behind",bh,i);outbuffer += ("(" + i + ") behind: " + bh)}

							case beforennlEX(bf) => {frbuffer += Tuple3("beforennl",bf,i);outbuffer += ("(" + i + ") beforennl: " + bf)}
							case behindnnlEX(bh) => {frbuffer += Tuple3("behindnnl",bh,i);outbuffer += ("(" + i + ") behindnnl: " + bh)}

							case beforeextractEX(bf) => {frbuffer += Tuple3("beforeextract",bf,i);outbuffer += ("(" + i + ") beforeextract: " + bf)}
							case behindextractEX(bh) => {frbuffer += Tuple3("behindextract",bh,i);outbuffer += ("(" + i + ") behindextract: " + bh)}

							case testnameEX(tf) => {frbuffer += Tuple3("testname",tf,i);outbuffer += ("(" + i + ") testname: " + tf)}
							case testequalsEX(tf) => {frbuffer += Tuple3("testequals",tf,i);outbuffer += ("(" + i + ") testequals: " + tf)}

							case counterreplaceEX(rp) => {frbuffer += Tuple3("counterreplace",rp,i);outbuffer += ("(" + i + ") counterreplace: " + rp)}
							case counterreplaceevenEX(rp) => {frbuffer += Tuple3("counterreplaceeven",rp,i);outbuffer += ("(" + i + ") counterreplaceeven: " + rp)}
							case counterreplaceoddEX(rp) => {frbuffer += Tuple3("counterreplaceodd",rp,i);outbuffer += ("(" + i + ") counterreplaceodd: " + rp)}

// ignore other
							case b => ;
						}
					}
				}
			}
			ret
		}
// ---------------------------
		def repl_special(rp: String): String = {
			val rep: String = rp.replace("\\", "\\\\").replace("$", "\\$")
			rep
		}
// ---------------------------
		def dofr(r: Boolean): Boolean = {
// frbuffer -> lines

			var ret = true

			r match {
				case false => ret = false
				case true => {
					var find = ""
					var replace = ""
					var findf = ""
					var replacef = ""
					var findc = ""
					var replacec = ""
					var findg1 = ""
					var replaceg1 = ""
					var findg2 = ""
					var replaceg2 = ""
					var findg2r = ""
					var replaceg2r = ""
					var findg3 = ""
					var replaceg3 = ""
					var g3format = "g3format123"
					var findtl = ""
					var replacetl = ""
					var findtu = ""
					var findcc = ""
					var replacetu = ""
					var replacecc = ""
					var findextract = ""

					var writeextract = ""
					var writeextractc = ""
					var writeextractadd = ""
					var writeextractaddc = ""

					var writeextractg = ""
					var writeextractgc = ""
					var writeextractgadd = ""
					var writeextractgaddc = ""

					var replaceextractonly = ""
					var writeextractdateformat = ""
					var writeextractdateadd = ""

					var testname = ""
					var testequals = ""
					var findcounter = ""
					var replacecounter = ""

					for (fi <- frbuffer; if ret) {
						Try {
							fi match {
								case ("find",fd,_) => {find = fd}

								case ("replace",rp,_) => {
									if (find != ""){
										replace = repl_special(rp)
										val replaceREX = new Regex(find)
										try {
											lines = replaceREX.replaceAllIn(lines, replace)
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: replace= Command: " + e, "#ect15: 821") :: ect.messages //#ect15
												ret = false
											}
										}
										find = ""
									}
								}

								case ("findf",fd,_) => {findf = fd}

								case ("replacef",rp,_) => {
									if (findf != ""){
										replace = repl_special(rp)
										val replaceREX = new Regex(findf)

										try {
											lines = replaceREX.replaceFirstIn(lines, replace)
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: replacef= Command: " + e, "#ect33: 840") :: ect.messages //#ect33
												ret = false
											}
										}
										findf = ""
									}
								}

								case ("findc",fd,_) => {findc = fd}

								case ("replacec",rp,_) => {
									if (findc != ""){
										lines = lines.replace(findc, rp + "\n")
										findc = ""
									}
								}

								case ("findg1",fd,_) => {findg1 = fd}

								case ("replaceg1",rp,_) => {
									if (findg1 != ""){
										replaceg1 = repl_special(rp)
										val replaceREX = new Regex(findg1,"G1")

										try {
											lines = replaceREX.replaceAllIn(lines, (m: Match) => replaceg1 format (m group "G1"))
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: replaceg1= Command: " + e, "#ect16: 868") :: ect.messages //#ect16
												ret = false
											}
										}
										findg1 = ""
									}
								}

								case ("findg2",fd,_) => {findg2 = fd}

								case ("replaceg2",rp,_) => {
									if (findg2 != ""){
										replaceg2 = repl_special(rp)
										val replaceREX = new Regex(findg2,"G1","G2")

										try {
											lines = replaceREX.replaceAllIn(lines, (m: Match) => replaceg2 format (m group "G1", m group "G2"))
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: replaceg2= Command: " + e, "#ect17: 887") :: ect.messages //#ect17
												ret = false
											}
										}
										findg2 = ""
									}
								}

								case ("findg2r",fd,_) => {findg2r = fd}

								case ("replaceg2r",rp,_) => {
									if (findg2r != ""){
										replaceg2r = repl_special(rp)
										val replaceREX = new Regex(findg2r,"G1","G2")

										try {
											lines = replaceREX.replaceAllIn(lines, (m: Match) => replaceg2r format (m group "G2", m group "G1"))
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: replaceg2r= Command: " + e, "#ect18: 906") :: ect.messages //#ect18
												ret = false
											}
										}
										findg2r = ""
									}
								}

								case ("findg3",fd,_) => {findg3 = fd}
								case ("g3format",fd,_) => {g3format = fd}

								case ("replaceg3",rp,_) => {
									if (findg3 != ""){
										replaceg3 = repl_special(rp)
										val replaceREX = new Regex(findg3,"G1","G2","G3")

										try {
											if(g3format == "g3format123") lines = replaceREX.replaceAllIn(lines, (m: Match) => replaceg3 format (m group "G1", m group "G2", m group "G3"))
											if(g3format == "g3format132") lines = replaceREX.replaceAllIn(lines, (m: Match) => replaceg3 format (m group "G1", m group "G3", m group "G2"))
											if(g3format == "g3format231") lines = replaceREX.replaceAllIn(lines, (m: Match) => replaceg3 format (m group "G2", m group "G3", m group "G1"))
											if(g3format == "g3format213") lines = replaceREX.replaceAllIn(lines, (m: Match) => replaceg3 format (m group "G2", m group "G1", m group "G3"))
											if(g3format == "g3format321") lines = replaceREX.replaceAllIn(lines, (m: Match) => replaceg3 format (m group "G3", m group "G2", m group "G1"))
											if(g3format == "g3format312") lines = replaceREX.replaceAllIn(lines, (m: Match) => replaceg3 format (m group "G3", m group "G1", m group "G2"))
											if(g3format == "g3format111") lines = replaceREX.replaceAllIn(lines, (m: Match) => replaceg3 format (m group "G1", m group "G1", m group "G1"))
											if(g3format == "g3format212") lines = replaceREX.replaceAllIn(lines, (m: Match) => replaceg3 format (m group "G2", m group "G1", m group "G2"))
											if(g3format == "g3format121") lines = replaceREX.replaceAllIn(lines, (m: Match) => replaceg3 format (m group "G1", m group "G2", m group "G1"))
											if(g3format == "g3format122") lines = replaceREX.replaceAllIn(lines, (m: Match) => replaceg3 format (m group "G1", m group "G2", m group "G2"))

										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: replaceg2r= Command: " + e, "#ect19: 936") :: ect.messages //#ect19
												ret = false
											}
										}
										findg2r = ""
									}
								}

								case ("findtl",fd,_) => {findtl = fd}

								case ("replacetl",rp,_) => {
									if (findtl != ""){
										replacetl = repl_special(rp)
										val replaceREX = new Regex(findtl,"Tlabel_copy")
										try {
											lines = replaceREX.replaceAllIn(lines, (m: Match) => replacetl format (m group "Tlabel_copy").toLowerCase())
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: replace= Command: " + e, "#ect20: 954") :: ect.messages //#ect20
												ret = false
											}
										}
										findtl = ""
									}
								}

								case ("findtu",fd,_) => {findtu = fd}

								case ("replacetu",rp,_) => {
									if (findtu != ""){
										replacetu = repl_special(rp)
										val replaceREX = new Regex(findtu,"TU1")
										try {
											lines = replaceREX.replaceAllIn(lines, (m: Match) => replacetu format (m group "TU1").toUpperCase())
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: replacetu= Command: " + e, "#ect21: 972") :: ect.messages //#ect21
												ret = false
											}
										}
										findtu = ""
									}
								}

								case ("findcc",fd,_) => {findcc = fd}

								case ("replacecc",rp,_) => {
									if (findcc != ""){
										replacecc = repl_special(rp)
										val replaceREX = new Regex(findcc)
										try {
											lines = replaceREX.replaceAllIn(lines, replacecc.head.toUpper + replacecc.tail)
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: replacecc= Command: " + e, "#ect36: 990") :: ect.messages //#ect36
												ret = false
											}
										}
										findcc = ""
									}
								}


								case ("findextract",fd,_) => {
									findextract = fd
								}

								case ("writeextract",rp,_) => {
									if (findextract != ""){
										val extractREX = new Regex(findextract)
										append = false
										extractmode = true

										try {
											for(m <- extractREX.findAllIn(lines).matchData){
												for (s <- m.subgroups) {
													extract += rp.replace("%s", s.mkString)
												}
											}
// keep result in extract, not in lines
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: findextract= Command: " + e, "#ect22: 1018") :: ect.messages //#ect22
												ret = false
											}
										}
										findextract = ""
									}
								}

								case ("writeextractc",rp,_) => {
									if (findextract != ""){
										val extractREX = new Regex(findextract)
										append = false
										extractmode = true

										try {
											for(m <- extractREX.findAllIn(lines).matchData){
												for (s <- m.subgroups) {
													extract += rp.replace("%s", s.mkString) + "\n"
												}
											}
// keep result in extract, not in lines
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: findextract= Command: " + e, "#ect34: 1041") :: ect.messages //#ect34
												ret = false
											}
										}
										findextract = ""
									}
								}

								case ("writeextractadd",rp,_) => {
// add
									if (findextract != ""){
										val extractREX = new Regex(findextract)
										append = true
										extractmode = true

										try {
											for(m <- extractREX.findAllIn(lines).matchData){
												for (s <- m.subgroups) {
													extract += rp.replace("%s", s.mkString)
												}
											}
// keep result in extract, not in lines
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: findextractadd= Command: " + e, "#ect23: 1194") :: ect.messages //#ect23
												ret = false
											}
										}
										findextract = ""
									}
								}

								case ("writeextractaddc",rp,_) => {
// add with return
									if (findextract != ""){
										val extractREX = new Regex(findextract)
										append = true
										extractmode = true

										try {
											for(m <- extractREX.findAllIn(lines).matchData){
												for (s <- m.subgroups) {
													extract += rp.replace("%s", s.mkString) + "\n"
												}
											}
// keep result in extract, not in lines
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: findextractadd= Command: " + e, "#ect35: 1229") :: ect.messages //#ect35
												ret = false
											}
										}
										findextract = ""
									}
								}

								case ("writeextractg",rp,_) => {
// group
									if (findextract != ""){
										val extractREX = new Regex(findextract)
										append = false
										extractmode = true

										try {
											for(m <- extractREX.findAllIn(lines).matchData){
												var counter = 1
												var replaceMe = ""
												var newContent = rp
												for (s <- m.subgroups) {
													replaceMe = "%s" + counter.toString
													newContent = newContent.replace(replaceMe, s.mkString)
													counter += 1
												}
												for (i <- 1 to 9){ // remove unused %sN
													replaceMe = "%s" + i
													newContent = newContent.replace(replaceMe, "")
												}

												extract += newContent
											}
// keep result in extract, not in lines
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: findextractadd= Command: " + e, "#ect23: 1194") :: ect.messages //#ect23
												ret = false
											}
										}
										findextract = ""
									}
								}

								case ("writeextractgc",rp,_) => {
// group add with return
									if (findextract != ""){
										val extractREX = new Regex(findextract)
										append = false
										extractmode = true

										try {
											for(m <- extractREX.findAllIn(lines).matchData){
												var counter = 1
												var replaceMe = ""
												var newContent = rp
												for (s <- m.subgroups) {
													replaceMe = "%s" + counter.toString
													newContent = newContent.replace(replaceMe, s.mkString)
													counter += 1
												}

												for (i <- 1 to 9){ // remove unused %sN
													replaceMe = "%s" + i
													newContent = newContent.replace(replaceMe, "")
												}
												extract += newContent + "\n"
											}
// keep result in extract, not in lines
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: findextractadd= Command: " + e, "#ect35: 1229") :: ect.messages //#ect35
												ret = false
											}
										}
										findextract = ""
									}
								}

								case ("writeextractgadd",rp,_) => {
// group add
									if (findextract != ""){
										val extractREX = new Regex(findextract)
										append = true
										extractmode = true

										try {
											for(m <- extractREX.findAllIn(lines).matchData){
												var counter = 1
												var replaceMe = ""
												var newContent = rp
												for (s <- m.subgroups) {
													replaceMe = "%s" + counter.toString
													newContent = newContent.replace(replaceMe, s.mkString)
													counter += 1
												}
												for (i <- 1 to 9){ // remove unused %sN
													replaceMe = "%s" + i
													newContent = newContent.replace(replaceMe, "")
												}

												extract += newContent
											}
// keep result in extract, not in lines
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: findextractadd= Command: " + e, "#ect23: 1194") :: ect.messages //#ect23
												ret = false
											}
										}
										findextract = ""
									}
								}

								case ("writeextractgaddc",rp,_) => {
// group add with return
									if (findextract != ""){
										val extractREX = new Regex(findextract)
										append = true
										extractmode = true

										try {
											for(m <- extractREX.findAllIn(lines).matchData){
												var counter = 1
												var replaceMe = ""
												var newContent = rp
												for (s <- m.subgroups) {
													replaceMe = "%s" + counter.toString
													newContent = newContent.replace(replaceMe, s.mkString)
													counter += 1
												}
												for (i <- 1 to 9){ // remove unused %sN
													replaceMe = "%s" + i
													newContent = newContent.replace(replaceMe, "")
												}

												extract += newContent + "\n"
											}
// keep result in extract, not in lines
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: findextractadd= Command: " + e, "#ect35: 1229") :: ect.messages //#ect35
												ret = false
											}
										}
										findextract = ""
									}
								}

								case ( "replaceextractonly",rp,_ ) => {
//#replaceextractonly
									if (findextract != ""){
										val extractREX = new Regex( findextract )
										extractToTargetMode = true


										try {
											for (m <- extractREX.findAllIn( lines ).matchData; s <- m.subgroups){
												val sInsert = s.mkString
// result in targetlines
												val replaceREX = new Regex( rp )
												targetlines = replaceREX.replaceAllIn(targetlines, "$1" + scala.util.matching.Regex.quoteReplacement( sInsert ) + "$3")
											}
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: replaceextractonly= Command: " + e, "#ect24: 1253") :: ect.messages //#ect24
												ret = false
											}
										}
										findextract = ""
									}
								}

								case ( "writeextractdateformat",rp,_ ) => {//#writeextractdateformat
									try {
										if(rp != ""){
											writeextractdateformat = new SimpleDateFormat(rp).format(System.currentTimeMillis())
										}
									} catch {
										case e:Throwable => {
											ect.messages = ("***** Error in: writeextractdateformat= Command: " + e, "#ect39: 1268") :: ect.messages //#ect39
											ret = false
										}
									}
								}

								case ( "writeextractdateadd",rp,_ ) => {//#writeextractdateformat
									try {
										if(rp != ""){
											append = true
											extractmode = true
											extract += rp.replace("%s", writeextractdateformat)
										}
									} catch {
										case e:Throwable => {
											ect.messages = ("***** Error in: writeextractdateadd= Command: " + e, "#ect40: 1283") :: ect.messages //#ect40
											ret = false
										}
									}

								}
								case ("before",bf,_) => {
									lines = bf + nl + lines
								}

								case ("behind",bh,_) => {
									lines = lines + nl + bh
								}

								case ("beforennl",bf,_) => {
									lines = bf + lines
								}

								case ("behindnnl",bh,_) => {
									lines = lines + bh
								}

								case ("beforeextract",bf,_) => {
									extract = bf + nl + extract
								}

								case ("behindextract",bh,_) => {
									extract = extract + nl + bh
								}

								case ("testname",tn,_) => {testname = tn}

								case ("testequals",te,_) => {
									testequals = te
									val testnameREX = new Regex(testequals)
									if(extractToTargetMode){
										testnameREX findFirstIn(targetlines) match {
											case Some(m) => testresults += Tuple3(testname, "OK! ", false)
											case _ => testresults += Tuple3(testname, "FAILED!", true)
										}
									} else{
										if(extractmode){
											testnameREX findFirstIn(extract) match {
												case Some(m) => testresults += Tuple3(testname, "OK! ", false)
												case _ => testresults += Tuple3(testname, "FAILED!", true)
											}
										} else {
											testnameREX findFirstIn(lines) match {
												case Some(m) => testresults += Tuple3(testname, "OK! ", false)
												case _ => testresults += Tuple3(testname, "FAILED!", true)
											}
										}
									}
								}

								case ("counterreplace",rp,_) => {
									var counter = rp.toInt
									var outline = ""
									var outlines = ""
									val replaceREX = new Regex("%n%")
									val text = lines.split("\n")

									for (line <- text) {
										try {
											outline = replaceREX.replaceAllIn(line, counter.toString)
										} catch {
											case e:Throwable => {
												ect.messages = ("***** Error in: counterreplace= Command: " + e, "#ect25: 1350") :: ect.messages //#ect25
												ret = false
											}
										}
										outlines += outline + "\n"
										outline = ""
										counter += 1
									} 
									lines = outlines
								}

								case ("counterreplaceeven",rp,_) => {
									var start = rp.toInt
									var counter = 0
									var outline = ""
									var outlines = ""
									val replaceREX = new Regex("%even%")
									val text = lines.split("\n")

									for (line <- text) {
										if(counter % 2 == 0 && counter >= start){
											try {
												outline = replaceREX.replaceAllIn(line, "even")
											} catch {
												case e:Throwable => {
													ect.messages = ("***** Error in: counterreplace= Command: " + e, "#ect26: 1375") :: ect.messages //#ect26
													ret = false
												}
											}
											outlines += outline + "\n"
											outline = ""
										} else {
											try {
												outline = replaceREX.replaceAllIn(line, "noteven")
											} catch {
												case e:Throwable => {
													ect.messages = ("***** Error in: counterreplace= Command: " + e, "#ect27: 1386") :: ect.messages //#ect27
													ret = false
												}
											}
											outlines += outline + "\n"
											outline = ""
										}
										counter += 1
									} 
									lines = outlines
								}


								case ("counterreplaceodd",rp,_) => {
									var start = rp.toInt
									var counter = 0
									var outline = ""
									var outlines = ""
									val replaceREX = new Regex("%odd%")
									val text = lines.split("\n")

									for (line <- text) {
										if(counter % 2 == 1 && counter >= start){
											try {
												outline = replaceREX.replaceAllIn(line, "odd")
											} catch {
												case e:Throwable => {
													ect.messages = ("***** Error in: counterreplaceodd= Command: " + e, "#ect28: 1413") :: ect.messages //#ect28
													ret = false
												}
											}
											outlines += outline + "\n"
											outline = ""
										} else {
											try {
												outline = replaceREX.replaceAllIn(line, "notodd")
											} catch {
												case e:Throwable => {
													ect.messages = ("***** Error in: counterreplaceodd= Command: " + e, "#ect29: 1424") :: ect.messages //#ect29
													ret = false
												}
											}
											outlines += outline + "\n"
											outline = ""
										}
										counter += 1
									}
									lines = outlines
								}

// ignore other
								case b => ;
							}
						} match {
							case Success(s) => ;
							case Failure(f) => {
								ect.messages = ("***** Error in Regex-Expression: " + f, "#ect30: 1442") :: ect.messages //#ect30
								ret = false
							}
						}
					}
				}
			}
			ret
		}
// ---------------------------
		def regexEscape(s: String): String = {
			var t = s.replace("\\","\\\\").replace(".","\\.").replace("^","\\^")
			t = t.replace("|","\\|").replace("$","\\$").replace("*","\\*").replace("+","\\+").replace("?","\\?")
			t = t.replace("(","\\(").replace(")","\\)").replace("{","\\{").replace("}","\\}").replace("[","\\[").replace("]","\\]")
			t
		}
// ---------------------------
		def writetarget(r: Boolean): Boolean = {
// mit backup der target-Datei
// verwendet: control + lines, liefert: 2 x Datei-write

			var ret = true

			r match {
				case false => ret = false
				case true => {

					if (!extractToTargetMode){
						if (clipboard){
							val systemClipboard: Clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
							val transferable: Transferable = new StringSelection(lines)
							systemClipboard.setContents(transferable, null)
						} else {
							var writer_out:BufferedWriter = null
							try {
								writer_out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetname, append), fileencoding))
								if (!extractmode) {
									writer_out.write(lines.replace("\n", sys.props("line.separator")))
								} else {
									writer_out.write(extract.replace("\n", sys.props("line.separator")))
								}
								writer_out.flush()
								writer_out.close()
							} catch {
								case e:Throwable => {
									if (writer_out != null) Try{writer_out.close()}
									ect.messages = ("***** Error: Target-File not writable: " + e, "#ect31: 1488") :: ect.messages //#ect31
									ret = false
								}
							}
						}
						if (!extractmode){
							guiUpdateActor ! new Ta3_replace(lines)
						} else {
							guiUpdateActor ! new Ta3_replace(extract)
						}
					} else {
						var writer_out:BufferedWriter = null
						try {
							writer_out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetname, append), targetencoding))
							writer_out.write(targetlines.replace("\n", sys.props("line.separator")))
							writer_out.flush()
							writer_out.close()
							guiUpdateActor ! new Ta3_replace(targetlines)
						} catch {
							case e:Throwable => {
								if (writer_out != null) Try{writer_out.close()}
								ect.messages = ("***** Error: Target-File not writable: " + e, "#ect32: 1509") :: ect.messages //#ect32
								ret = false
							}
						}
					}
				}
			}
			ret
		}

// ----- FindreplaceGui open
		if (silent) {
			autoexit = true
		} else {
			FindreplaceGui.frameMain.open
			FindreplaceGui.frameMain.visible = true

			FindreplaceHelpGui.frameHelp.open
			FindreplaceHelpGui.frameHelp.visible = false
		}

		guiUpdateActor ! new Ta1_add("Working, please be patient...")

// default, ev. overwritten by readparam
		commandfile = "findreplace.frpl"

		guiUpdateActor ! ControlFilename(commandfile)

		var findreplacerun = readparam(true)
		findreplacerun = readcommandfile(findreplacerun)
		findreplacerun = parseSetControlCommands(findreplacerun)
		findreplacerun = parsecontrolcommands(findreplacerun)
		findreplacerun = readtextfile(findreplacerun)
		findreplacerun = readtargetfile(findreplacerun)
		findreplacerun = parsecommands(findreplacerun)
		findreplacerun = dofr(findreplacerun)
		if (!nobackup && !clipboard) findreplacerun = save(findreplacerun)
		findreplacerun = writetarget(findreplacerun)

		var out = ""
		var errortext = ""

		for(i <- outbuffer) {
			out += i + nl
		}

		if (ect.messages.length > 1){
			FindreplaceGui.frameMain.open
			FindreplaceGui.frameMain.visible = true

			for (i <- ect.messages.reverse) {
				if (i._2 != "")
				errortext += i._1 + " code: " + i._2 + nl
				else
				errortext += i._1 + nl
			}

			control_out += out + nl + nl + errortext

			guiUpdateActor ! new Ta2_replace(control_out)

			Sound.play ! new PLAY("I[Flute]")
			Thread.sleep(100)
			Sound.play ! new PLAY("a a")

			guiUpdateActor ! SELECTINDEX(2)
			guiUpdateActor ! SCROLLDOWN(2)

			guiUpdateActor ! Show("Errors occured!")
		} else {
			control_out += out

			guiUpdateActor ! new Ta2_replace(control_out)

			Sound.play ! new PLAY("I[Piano]]")
			Thread.sleep(100)
			Sound.play ! new PLAY("g e c5h")
		}

		if (testresults.length > 0) {
			var out = ""
			var testfailed = false

			for (i <- testresults) {
				out += i._1 + " " + i._2 + nl
				testfailed = testfailed || i._3
			}
			if (testfailed) {
				out += nl + nl + "Test FAILED!" 
				exitcode = 1
				autoexit = false
			} else {
				out += nl + nl + "Test is ok!" 
				exitcode = 0
			}

			if (!autoexit) guiUpdateActor ! Show(out)
		}

		val autostop = autoexit && (ect.messages.length <= 1) && findreplacerun

		if (autostop) {
			guiUpdateActor ! STOP
			guiActorHelp ! STOP
			FindreplaceHelpGui.frameHelp.dispose
			FindreplaceGui.frameMain.dispose
			Sound.play ! STOP
			Thread.sleep(300)
			sys.exit(exitcode) 
		}

	} // END main
}
