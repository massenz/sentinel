package com.alertavert.sentinel.security

import com.alertavert.sentinel.persistence.HasCreator
import com.alertavert.sentinel.model.{Resource, User}
import org.bson.{Transformer, BSON}
import com.mongodb.casbah.commons.conversions.MongoConversionHelper


trait Action extends Serializable {
  def name = {
    val fqn = getClass.getName
    val last_dot = fqn.lastIndexOf('.')
    if (last_dot > 0) fqn.substring(last_dot + 1) else fqn
  }

  override def toString = name.capitalize
}

object Action {
  var actionsMap: Map[String, Action] = Map()

  def register(action: Action) {
    actionsMap += action.name -> action
  }
}

class ManageSystem extends Action

object ManageSystem {
  private val action = new ManageSystem
  Action.register(action)
  def apply() = action
}

class Create extends Action

object Create {
  private val action = new Create
  Action.register(action)
  def apply(): Action = action
}

class Grant extends ManageSystem

object Grant {
  private val action = new Grant
  Action.register(action)

  def apply() = action
}

class Edit extends Action

object Edit {
  private val action = new Edit
  Action.register(action)

  def apply(): Action = action
}

class Delete extends Action

object Delete {
  private val action = new Delete
  Action.register(action)

  def apply() = action
}

class View extends Action

object View extends Action {
  private val action = new View
  Action.register(action)

  def apply(): Action = action
}


/**
 * A ``permission`` defines an action that can be performed (eg, ``edit``) on a Resource.
 * Permissions are immutable.
 *
 * @param action
 * @param resource
 */
class Permission(val action: Action, val resource: Resource) {
  private var user: User = _

  // TODO: this needs some guard: ie, verifying that the 'grantor' has the permission to Grant on
  // this Asset
  def grant(user: User) {
    this.user = user
  }

  def grantedTo: Option[User] = user match {
    case null => None
    case _ => Some(user)
  }
}
