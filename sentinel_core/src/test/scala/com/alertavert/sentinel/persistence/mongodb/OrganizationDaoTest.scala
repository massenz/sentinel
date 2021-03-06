// Copyright AlertAvert.com (c) 2014. All rights reserved.
// Commercial use or modification of this software without a valid license is expressly forbidden

package com.alertavert.sentinel.persistence.mongodb

import com.alertavert.sentinel.security.Credentials
import org.scalatest._
import com.alertavert.sentinel.model.{User, Organization}
import org.bson.types.ObjectId
import com.alertavert.sentinel.persistence.{DataAccessManager, DAO}
import com.alertavert.sentinel.UnitSpec


class OrganizationDaoTest extends UnitSpec with BeforeAndAfter {

  var dao: DAO[Organization] = _

  trait OrgCreator {
    val admin = User.builder("admin") hasCreds getNewCreds build()
    val userDao = MongoUserDao()
    admin.setId(userDao << admin)
  }

  before {
    dao = MongoOrganizationDao()
    dao.asInstanceOf[MongoOrganizationDao].collection.drop()
    assume(dao.asInstanceOf[MongoOrganizationDao].collection.count() == 0,
      "Collection should be empty prior to running tests")
  }

  "Organizations" can "be saved in Mongo" in new OrgCreator {
    val acme = (Organization.builder("Acme Inc.")
      createdBy admin
      build)
    val id = dao.upsert(acme)
    assert(id != null)
    assert(! id.toString.isEmpty)
  }

  they can "be retrieved" in new OrgCreator {
    val acme = (Organization.builder("New Acme Inc.")
      createdBy admin
      build)
    val id = dao << acme
    assert(id != null)
    val acme_reborn = dao.find(id)
    assert(acme_reborn != None)
    // This is necessary to ensure equality, all else being equal too:
    acme.setId(id)
    assert(acme_reborn.get === acme)
  }

  they can "be found by name" in new OrgCreator {
    val testOrg = Organization.builder("test-org") createdBy admin build
    val id = dao << testOrg
    val foundIt = dao.findByName("test-org").getOrElse(fail("Could not retrieve org by name"))
    assert(testOrg === foundIt)
  }
}
