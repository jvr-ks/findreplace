/************************************************
* findreplace_actormessages.scala
************************************************/
// ü

package de.jvr

// FindreplaceGui
object ActorMessages {

	case class Ta0_add(t: String)
	case class Ta0_replace(t: String)

	case class Ta1_add(t: String)
	case class Ta1_replace(t: String)

	case class Ta2_add(t: String)
	case class Ta2_replace(t: String)

	case class Ta3_add(t: String)
	case class Ta3_replace(t: String)

	case class Show(t: String)

	case class SourceFilename(fn: String)

	case object SourceUrl

	case class ControlFilename(fn: String)

	case class TargetFilename(fn: String)

	case object CENTER

	case class Location(x: Int, y: Int )
	case class Resize(x: Int, y: Int )

	case class SELECTINDEX(i: Int)

	case class SCROLLDOWN(i: Int)

	case object CHECKVERSION

	case class JUSTSAVED(s: String)
	case object RESETJUSTSAVED

	case object JUSTSAVEDALL
	case object RESETJUSTSAVEDALL

	// Sound
	case class PLAY(t: String)

	// Multi
	case object STOP
}