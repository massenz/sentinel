package com.alertavert.sentinel.model

import com.mongodb.casbah.Imports.ObjectId

import com.alertavert.sentinel.security.Credentials
import com.alertavert.sentinel.UnitSpec

/**
 * Unit tests for the User class
 */
class UserTest extends UnitSpec {

  trait UserBuilder {
    val builder = User.builder("Bob")
  }

  trait UserCredentials {
    val creds = Credentials.createCredentials("anUser", "andHisPwd")
  }

  "A user" can "be created" in new UserBuilder {
    val user = builder.build()
    assert(user != null)
    assertResult("Bob") { user.firstName }
  }

  it can "have credentials" in new UserBuilder with UserCredentials {
    val aUser = builder.hasCreds(creds.username, creds.hashedPassword, creds.salt).isActive.build
    assert(aUser != null)
    assert(aUser.checkCredentials(creds))
    assert(aUser.isActive)
  }

  it should "have a string representation" in new UserBuilder {
    val userId = new ObjectId()
    val firstName = "Bob"

    val bob = builder.withId(userId).isActive.build()
    assertResult(s"[$userId] $firstName  (Active)") {
      bob.toString
    }
  }

  "Invalid credentials" must "fail checks" in new UserBuilder with UserCredentials {
    // the `salt` is not a secret, but can be used as a `challenge` and thus may be known to a hacker
    val salt = creds.salt
    val guessedHashPwd = Credentials.hash("wildGuess", salt)
    val hackerCreds = new Credentials("bob", guessedHashPwd, salt)

    // We would normally retrieve the user from the DB - we'll just create it anew here
    val aUser = builder.hasCreds(creds.username, creds.hashedPassword, creds.salt).
      isActive.build()
    assert(! (aUser checkCredentials hackerCreds))
    assert(aUser checkCredentials creds)
  }

  "users with same username and ID" should "be equal" in {
    val userId = new ObjectId
    val userA = User.builder("joe").withId(userId).hasCreds("joe", "doesntmatter", 999).build()
    val userB = User.builder("bob").withId(userId).hasCreds("joe", "reallydoesnt", 666).build()
    assert(userA === userB)

    // this should work also if no _id has been assigned
    val userC = User.builder("jill").hasCreds("lady", "doesntmatter", 999).build()
    val userD = User.builder("jane").hasCreds("lady", "reallydoesnt", 666).build()
    assert(userC === userD)
  }
}