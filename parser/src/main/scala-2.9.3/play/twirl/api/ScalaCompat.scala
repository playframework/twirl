/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.api

/**
 * Cross scala version compatibility.
 */
object ScalaCompat {
  class WithRuntimeClass[T](manifest: ClassManifest[T]) {
    def runtimeClass: java.lang.Class[_] = manifest.erasure
  }
  implicit def addRuntimeClass[T](manifest: ClassManifest[T]): WithRuntimeClass[T] = new WithRuntimeClass(manifest)
}
