package com.alertavert.sentinel.persistence

import com.mongodb.casbah.Imports.ObjectId

trait HasId {
  var id: Option[ObjectId] = None

  def setId(id: ObjectId) {
    this.id = if (id != null) Some(id) else None
  }
}