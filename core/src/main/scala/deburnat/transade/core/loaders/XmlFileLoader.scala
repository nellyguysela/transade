package deburnat.transade.core.loaders

import xml.{Elem, Node}

import java.io.File

import deburnat.transade.core.admins.{CoreAdmin, PdfCreator}
import CoreAdmin._
import deburnat.transade.core.conc.{TransadeNodes, Concurrency}
import deburnat.transade.Mode._

protected[transade] final class XmlFileLoader(admin: CoreAdmin) {

  private val (output, xmlFilePath, _ref) = (
    admin.output, new StringBuilder, <references/>.asInstanceOf[Node]
  )
  /* goodToCompute determines whether or not the getTransfers method has been invoked
   * prior to the compute method.
   * ref represents the current [references] Node.
   */
  private var (ref, goodToCompute) = (_ref, false)


  /**
   * This method is used to get all the transfer nodes and set the references node
   * in the given .xml file.
   * @param xmlFilePath The path of the (deburnat).xml file
   * @return A NodeSeq object representing the list of all transfer found in a the deburnat node
   *         or null otherwise.
   */
  def getTransfers(xmlFilePath: String): Seq[Node] = try{
    this.xmlFilePath.clear
    val _xmlFilePath = getCanPath(xmlFilePath)
    val root = scala.xml.XML.loadFile(_xmlFilePath)
    /* The root is initialized at this level because outside of this method
     * there is no usage of the root.
     */

    if(root.label.equals("transade")){ //first check whether or not the file root's label is "transade"
      this.xmlFilePath.append(_xmlFilePath)
      val temp = root \\ "references"
      ref = if(temp.nonEmpty) temp(0) else _ref
      /* This is in reality one node ("references").
       * The [ref] nodes can't be provided yet because it first has  to be determined whether or not
       * the [references] node is included.
       * This process takes place in the deburnat.transade.core.admins.TransadeXmlAdmin.setRefs method
       */

      goodToCompute = true //now the compute method can be invoked
      root \\ transfer
    }else null
  }catch{case e: Exception => null} //org.xml.sax.SAXParseException, FileNotFoundException


  /**
   * It's invoked to parse and possibly compile the given {transfer} nodes.
   * @param transfers The {transfer} nodes to parse and possibly compile.
   * @param mode It determines whether or not the files created next to the report
   *             should be compiled.
   *             If compiled, this attribute also determines whether or not the files
   *             should be deleted.
   * @return A Tuppel object: the report file, the transfer folder.
   */
  def compute(transfers: Seq[Node], mode: Mode): (File, File) =
    if(admin.goodToGo && goodToCompute && transfers != null && transfers.nonEmpty) try{
      val preview = mode match {
        //this object determines whether or not the parsed classes are to be compiled & run
        case PREVIEW => true
        case BETA_COMPILE | USER_COMPILE => false
      }

      val reportFile = PdfCreator.saveReport(
        "<%s>%s</%s>".format(root, //root =: report
          br+Concurrency.compute(TransadeNodes(ref, transfers), preview, output)+br,
        root),
        xmlFilePath.mkString
      )

      output(view.read("outputreport") + ": " + reportFile.getCanonicalPath)
      //Empty the bin directory. Empty the "doc" directory only if required.
      emptyBinDir
      if(mode == USER_COMPILE) emptyDocDir else output(view.read("outputothers") + ": " + getDocPath)

      goodToCompute = false //reset
      (reportFile, new File(getDocPath)) //return: transfers directory
    }catch {case e: Exception => null} else null


  /**
   * @see compute(dirPath: String, transfers: Seq[Node], mode: Mode, output: String => Unit)
   *      for more information.
   */
  def compute(transfers: Seq[Node]): (File, File) = compute(transfers, USER_COMPILE)

  /**
   * @see compute(dirPath: String, transfers: Seq[Node], mode: Mode, output: String => Unit)
   *      for more information.
   */
  def compute(xmlFilePath: String): (File, File) = compute(getTransfers(xmlFilePath), USER_COMPILE)

}
