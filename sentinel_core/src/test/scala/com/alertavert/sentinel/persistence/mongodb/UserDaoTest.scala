// Copyright AlertAvert.com (c) 2014. All rights reserved.
// Commercial use or modification of this software without a valid license is expressly forbidden

package com.alertavert.sentinel.persistence.mongodb

import org.scalatest._
import com.alertavert.sentinel.model.{Resource, User}
import org.bson.types.ObjectId
import com.alertavert.sentinel.errors.NotAllowedException
import com.alertavert.sentinel.persistence.DAO
import com.alertavert.sentinel.UnitSpec
import com.alertavert.sentinel.security._


class UserDaoTest extends UnitSpec with BeforeAndAfter {

  var dao: DAO[User] = _

  before {
    dao = MongoUserDao()
    val coll = dao.asInstanceOf[MongoUserDao].collection
    dao.clear()
    assume(coll.count() == 0, "Collection should be empty prior to running tests")
  }

  // Use random new credentials for each new users (usernames MUST be unique); we don't care what anyway
  def creds = getNewCreds

  trait CreatedByAdminUser {
    val adminUser = User.builder("admin") hasCreds creds build()
    val adminId = dao << adminUser
  }

  trait CreatedByOrdinaryUser extends CreatedByAdminUser {
    val ordinaryUser = User.builder("Creator", "User") hasCreds creds withId new ObjectId createdBy adminUser build()
    val creatorId = dao << ordinaryUser
  }

  "when saving a valid user, we" should "get a valid OID" in new CreatedByOrdinaryUser {
    val user = User.builder("bob", "foo") createdBy ordinaryUser hasCreds creds build()
    val uid = dao << user
    assert (uid != null)
  }

  it should "preserve the data" in new CreatedByOrdinaryUser {
    val user = User.builder("Dan", "Dude") createdBy ordinaryUser hasCreds("dandude", "abcfedead",
      1234) build()
    val uid = dao << user
    val retrievedUser = dao.find(uid).getOrElse(fail("No user found for the given OID"))
    assert(user.firstName === retrievedUser.firstName)
    assert(user.lastName === retrievedUser.lastName)
    assert(user.getCredentials === retrievedUser.getCredentials)
  }

  it should "get the same ID, if previously set" in new CreatedByOrdinaryUser {
    val user = User.builder("Joe", "blast") createdBy ordinaryUser hasCreds creds build()
    val uid = new ObjectId
    user.setId(uid)
    val newUid = dao << user
    assert (uid === newUid)
    user.resetPassword("foobar")
    val anUid = dao << user
    assert(uid === anUid)
  }

  it should "have the creators' chain preserved" in new CreatedByOrdinaryUser {
    val bob = User.builder("bob") createdBy ordinaryUser hasCreds creds build()
    val bobId = dao << bob

    val bobAgain = dao.find(bobId).getOrElse(fail("Could not retrieve a valid user (Bob)"))
    assert(bob === bobAgain)
    assert(ordinaryUser === bobAgain.createdBy.getOrElse(fail("No creator for bobAgain")))
    val admin = bob.createdBy.get.createdBy.getOrElse(fail("No Admin creator"))
    assert(adminUser === admin)
  }

  it can "be saved with permissions set" in new CreatedByOrdinaryUser {
    val resource = new Resource("buzz", ordinaryUser)
    resource.allowedActions ++: List(Create(), Edit())
    MongoResourceDao() << resource
    // TODO: implement this test
    dao << ordinaryUser
  }

  "when saving many users, they" should "be found again" in new CreatedByOrdinaryUser {
    val users = List(User.builder("alice") createdBy ordinaryUser hasCreds creds build(),
      User.builder("bob") createdBy ordinaryUser hasCreds creds build(),
      User.builder("charlie") createdBy ordinaryUser hasCreds creds build())

    users.foreach(dao << _)
    dao.findAll() map(_.firstName) should contain allOf ("alice", "bob", "charlie")
    dao.findAll() should have size 5
  }

  "saving multiple users with the same username" should "cause an error" in
    new CreatedByOrdinaryUser {
      intercept[NotAllowedException] {
        val usr1 = User.builder("bob") hasCreds Credentials("bob", "foo") build()
        val usr2 = User.builder("BigBob") hasCreds Credentials("bob", "baz") build()

        val id1 = dao << usr1
        val id2 = dao << usr2

        id1 should equal (id2)
      }

  }
}
