package controllers

import com.alertavert.sentinel.errors.AuthenticationError
import com.alertavert.sentinel.persistence.DataAccessManager
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models._
import models.resources._


object Application extends Controller {

  // TODO(marco): the DB URI must be read from configuration
  DataAccessManager.init("mongodb://localhost/sentinel-test")

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def users = Action {
    val resJson = Json.toJson(UsersResource.getAllUsers)
    Ok(resJson)
  }

  def userById(id: String) = Action {
    val user = Json.toJson(UsersResource.getUserById(id))
    Ok(user)
  }

  def login = Action(BodyParsers.parse.json) {
    request =>
      val req = request.body
      val username = req \ "username"
      val password = req \ "password"
      try {
        val user = UsersResource.authUser(username.as[String], password.as[String])
        Ok(Json.toJson(user))
      } catch {
        case ex: AuthenticationError => Unauthorized(Json.obj("error" -> ex.getLocalizedMessage))
      }
  }

  def createUser = Action(BodyParsers.parse.json) {
    request =>
      Created(UsersResource.createUser(request.body))
  }

}
