package org.aponcet.mygift.dbmanager

class DbException(message: String, e: Exception) : Exception("$message. Reason: [${e.message}]", e)