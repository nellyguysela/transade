package deburnat.transade.core.conc

import collection.mutable.Map
import xml.Elem
import actors.Actor

import deburnat.transade.core.admins.{CoreAdmin, TransadeScalaAdmin}
import CoreAdmin.{br, tab1, proc, bug}
import deburnat.transade.core.readers.Reader.read

/**
 * An algorithm for data transfer.
 * Project name: deburnat
 * Date: 10/1/13
 * Time: 1:27 AM
 * @author Patrick Meppe (tapmeppe@gmail.com)
 */
protected[conc] final class TransadeScalaActor(
  paths: Map[String, Elem], output: String => Unit
) extends TransadeActor{

  val maxLen = paths.size

  private class ScalaActor(actor: TransadeScalaActor, scalaPath: String, impRoot: Elem) extends Actor{
    override def exceptionHandler = {
      case e: Exception => actor ! (scalaPath, e)
    }
    override def act{
      actor ! TransadeScalaAdmin.run(scalaPath, impRoot, output) //report =: transfer process
    }
  }

  protected def startSubActors = paths.foreach{path =>
    startSubActor(new ScalaActor(this, path._1, path._2))
  }

  protected def getExceptionNode(source: String, e: Exception): String = CoreAdmin.getExceptionNode(
    tab1, proc, e,
    read(source, len)+br+bug.read("class", len)+br+read(e.getMessage, len)
    /* TEMPLATE: move the comment signs to get the xml object
      <process exception={e.getClass.getSimpleName}>
      {read(source, 4)+br+bug.read("class", 4)+br+read(e.getMessage, 4)}
      </process>
    */
  )

}
