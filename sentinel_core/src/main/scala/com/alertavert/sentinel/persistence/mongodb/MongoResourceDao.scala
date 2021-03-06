// Copyright AlertAvert.com (c) 2014. All rights reserved.
// Commercial use or modification of this software without a valid license is expressly forbidden

package com.alertavert.sentinel.persistence.mongodb

import com.mongodb.casbah.Imports._
import com.alertavert.sentinel.model.Resource
import com.alertavert.sentinel.security.Action
import com.alertavert.sentinel.persistence.DataAccessManager
import com.alertavert.sentinel.errors.{NotFoundException, NotAllowedException, DbException}

class MongoResourceDao(override val collection: MongoCollection) extends
    MongoDao[Resource](collection) with MongoSerializer[Resource] {

  override def serialize(resource: Resource): MongoDBObject = {
    MongoDBObject(
      "owner_id" -> resource.owner.id.getOrElse({
        MongoUserDao() << resource.owner
        resource.owner.id
      }),
      "allowed_actions" -> resource.allowedActions,
      "name" -> resource.name,
      "asset_type" -> resource.assetType,
      // the path is serialized as a read-only value, it won't be deserialized as it can be
      // reconstructed from the asset_type and the ID
      "path" -> resource.path
    )
  }

  override def deserialize(dbObj: MongoDBObject): Resource = {
    val ownerId = dbObj.as[ObjectId]("owner_id")
    val owner = MongoUserDao().find(ownerId).getOrElse(
      throw new NotFoundException(ownerId, "Invalid Owner ID for resource"))
    val name = dbObj.as[String]("name")
    val res = new Resource(name, owner)
    res.allowedActions ++= dbObj.as[List[Action]]("allowed_actions")

    res
  }

  override def findByName(name: String) = collection.findOne(MongoDBObject(
      "name" -> name
    )) match {
      case None => None
      case Some(resource) => Some(deserialize(resource))
    }
}

object MongoResourceDao {
  private val RESOURCE_COLLECTION = "resources"
  private var instance: MongoResourceDao = _

  def apply(): MongoResourceDao = instance match {
    case null =>
      if (DataAccessManager isReady) {
      instance = new MongoResourceDao(DataAccessManager.db(RESOURCE_COLLECTION))
          with IdSerializer[Resource] with CreatorSerializer[Resource]
      } else {
        throw new DbException("DataAccessManager not initialized; use DataAccessManager.init()")
      }
      instance
    case _ => instance
  }
}
