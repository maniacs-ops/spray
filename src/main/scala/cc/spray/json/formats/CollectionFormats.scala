package cc.spray.json
package formats

/*
 * Original implementation (C) 2009-2011 Debasish Ghosh
 * Adapted and extended in 2011 by Mathias Doenitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import reflect.Manifest

trait CollectionFormats {
  
  implicit def listFormat[T :JsonFormat] = new JsonFormat[List[T]] {
    def write(list: List[T]) = JsArray(list.map(_.toJson))
    def read(value: JsValue) = value match {
      case JsArray(elements) => elements.map(_.fromJson)
      case _ => throw new DeserializationException("List expected")
    }
  }
  
  implicit def arrayFormat[T :JsonFormat :Manifest] = new JsonFormat[Array[T]] {
    def write(array: Array[T]) = JsArray(array.map(_.toJson).toList)
    def read(value: JsValue) = value match {
      case JsArray(elements) => elements.map(_.fromJson[T]).toArray
      case _ => throw new DeserializationException("Array expected")
    }
  }
  
  implicit def mapFormat[K :JsonFormat, V :JsonFormat] = new JsonFormat[Map[K, V]] {
    def write(m: Map[K, V]) = JsObject {
      m.toList.map { t =>
        t._1.toJson match {
          case JsString(x) => JsField(x, t._2.toJson)
          case x => throw new SerializationException("Map key must be formatted as JsString, not '" + x + "'")
        }
      }
    }
    def read(value: JsValue) = value match {
      case JsObject(fields) => fields.map(field => (JsString(field.name).fromJson[K], field.value.fromJson[V])).toMap
      case _ => throw new DeserializationException("Map expected")
    }
  }

  implicit def immutableSetFormat[T :JsonFormat] = viaList[Set[T], T](list => Set(list :_*))
  
  import collection.mutable.Set
  implicit def mutableSetFormat[T :JsonFormat] = viaList[Set[T], T](list => Set.empty ++ list)

  def viaList[I <: Iterable[T], T :JsonFormat](f: List[T] => I): JsonFormat[I] = new JsonFormat[I] {
    def write(iterable: I) = JsArray(iterable.map(_.toJson).toList)
    def read(value: JsValue) = value match {
      case JsArray(elements) => f(elements.map(_.fromJson[T]))
      case _ => throw new DeserializationException("Collection expected")
    }
  }
  
}