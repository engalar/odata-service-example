package odata.dataproviders

import org.apache.olingo.commons.api.data.EntityCollection
import org.apache.olingo.commons.api.edm.EdmEntitySet

trait ODataDataProvider {

  def getData(edmEntitySet: EdmEntitySet, selectList: Option[String], filter: Option[String]): EntityCollection

}