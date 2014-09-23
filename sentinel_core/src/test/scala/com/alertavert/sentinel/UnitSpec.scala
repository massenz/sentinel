package com.alertavert.sentinel

import com.alertavert.sentinel.persistence.mongodb.MongoUserDao
import com.alertavert.sentinel.security.Credentials
import org.scalatest._
import com.alertavert.sentinel.persistence.DataAccessManager

abstract class UnitSpec[T] extends FlatSpec with Matchers with OptionValues with
    Inside with Inspectors with BeforeAndAfter {

  if (! DataAccessManager.isReady) DataAccessManager.init("mongodb://localhost:27017/sentinel-test")

  def getNewCreds: Credentials = {
    val suffix = Math.round(1000 * Math.random()) toString
    val prefix = "test-user"
    val newUsername = List(prefix, suffix) mkString "-"
    val dao = MongoUserDao()
    dao.findByName(newUsername) match {
      case None => Credentials(newUsername, "secret")
      case Some(user) => getNewCreds
    }
  }

}