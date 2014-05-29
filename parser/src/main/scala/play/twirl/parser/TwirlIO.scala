package play.twirl.parser

import java.io._
import scala.io.Codec
import java.net.URL

/**
 * IO utilites for Twirl.
 *
 * This is intentionally not public API.
 */
private[twirl] object TwirlIO {

  val defaultEncoding = scala.util.Properties.sourceEncoding

  val defaultCodec = Codec(defaultEncoding)

  /**
   * Read the given stream into a byte array.
   *
   * Does not close the stream.
   */
  def readStream(stream: InputStream): Array[Byte] = {
    val buffer = new Array[Byte](8192)
    var len = stream.read(buffer)
    val out = new ByteArrayOutputStream()
    while (len != -1) {
      out.write(buffer, 0, len)
      len = stream.read(buffer)
    }
    out.toByteArray
  }

  /**
   * Read the file as a String.
   */
  def readFile(file: File): Array[Byte] = {
    val is = new FileInputStream(file)
    try {
      readStream(is)
    } finally {
      closeQuietly(is)
    }
  }

  /**
   * Read the given stream into a String.
   *
   * Does not close the stream.
   */
  def readStreamAsString(stream: InputStream, codec: Codec = defaultCodec): String = {
    new String(readStream(stream), codec.name)
  }

  /**
   * Read the URL as a String.
   */
  def readUrlAsString(url: URL, codec: Codec = defaultCodec): String = {
    val is = url.openStream()
    try {
      readStreamAsString(is, codec)
    } finally {
      closeQuietly(is)
    }
  }

  /**
   * Read the file as a String.
   */
  def readFileAsString(file: File, codec: Codec = defaultCodec): String = {
    val is = new FileInputStream(file)
    try {
      readStreamAsString(is, codec)
    } finally {
      closeQuietly(is)
    }
  }

  /**
   * Write the given String to a file
   */
  def writeStringToFile(file: File, contents: String, codec: Codec = defaultCodec) = {
    if (!file.getParentFile.exists) {
      file.getParentFile.mkdirs()
    }
    val writer = new OutputStreamWriter(new FileOutputStream(file), codec.name)
    try {
      writer.write(contents)
    } finally {
      closeQuietly(writer)
    }
  }

  /**
   * Close the given closeable quietly.
   *
   * Ignores any IOExceptions encountered.
   */
  def closeQuietly(closeable: Closeable) = {
    try {
      if (closeable != null) {
        closeable.close()
      }
    } catch {
      case e: IOException => // Ignore
    }
  }

  /**
   * Delete the given directory recursively.
   */
  def deleteRecursively(dir: File) {
    if (dir.isDirectory) {
      dir.listFiles().foreach(deleteRecursively)
    }
    dir.delete()
  }
}
