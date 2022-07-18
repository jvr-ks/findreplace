/************************************************
* findreplace_codec.scala
************************************************/
// ü


package de.jvr

import scala.language.postfixOps

object Codec {

// ---------------------------
	def isASCII(ba: Array[Byte]): Boolean = {
		var r = true
		for (i <- ba) {
			if (i < 0 ){
				i match {
					case -4 => r = true
					case -10 => r = true
					case -28 => r = true
					case -33 => r = true
					case -75 => r = true
					case -77 => r = true
					case -78 => r = true
					case -89 => r = true
					case -128 => r = true
					case _ => r = false
				}
			}
		}
		r
	}
// ---------------------------
	def readfile(file: java.io.File): Option[Array[Byte]] = {
		var ba: Option[Array[Byte]] = None
		try {
			val is = new java.io.BufferedInputStream(new java.io.FileInputStream(file))
			ba = Some(Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray)
			is.close()
		} catch {
			case e:Throwable => ;// ignored here
		}
		ba
	}
// ---------------------------
	def guessCodec(s: String):String = {
		var codec = ""
		val f = new java.io.File(s)
		val ba = readfile(f) getOrElse new Array[Byte](0)
		if (isASCII(ba)) codec = "ISO-8859-1"
			else codec = "UTF-8"
		codec
	}
// ---------------------------
	def guessCodec(f: java.io.File):String = {
		var codec = ""
		val ba = readfile(f) getOrElse new Array[Byte](0)
		if (isASCII(ba)) codec = "ISO-8859-1"
			else codec = "UTF-8"
		codec
	}
// ---------------------------
}