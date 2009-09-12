package net.liftweb.json

/*
 * Copyright 2009 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

import java.lang.reflect.{Constructor => JConstructor, Type}
import java.util.Date
import JsonAST._

object Meta {
  /** Intermediate metadata format for case classes.
   *  This ADT is constructed (and then memoized) from given case class using reflection.
   *
   *  Example mapping.
   *
   *  package xx 
   *  case class Person(name: String, address: Address, children: List[Child])
   *  case class Address(street: String, city: String)
   *  case class Child(name: String, age: BigInt)
   *
   *  will produce following Mapping:
   *
   *  Constructor(None, "xx.Person", List(
   *    Value("name"),
   *    Constructor(Some("address"), "xx.Address", List(Value("street"), Value("city"))),
   *    ListConstructor("children", "xx.Child", List(Value("name"), Value("age")))))
   */
  sealed abstract class Mapping
  case class Value(path: String, targetType: Class[_]) extends Mapping
  case class Constructor(path: Option[String], constructor: JConstructor[_], args: List[Mapping]) extends Mapping
  case class ListConstructor(path: String, constructor: JConstructor[_], args: List[Mapping]) extends Mapping
  case class ListOfPrimitives(path: String, elementType: Class[_]) extends Mapping
  case class Optional(mapping: Mapping) extends Mapping

  private val memo = new Memo[Class[_], Mapping]

  private[json] def mappingOf(clazz: Class[_]) = {
    import Reflection._

    def makeMapping(path: Option[String], clazz: Class[_], isList: Boolean): Mapping = isList match {
      case false => Constructor(path, clazz.getDeclaredConstructors()(0), constructorArgs(clazz))
      case true if primitive_?(clazz) => ListOfPrimitives(path.get, clazz)
      case true => ListConstructor(path.get, clazz.getDeclaredConstructors()(0), constructorArgs(clazz))
    }

    def constructorArgs(clazz: Class[_]) = clazz.getDeclaredFields.filter(!static_?(_)).map { f =>
      val annot = f.getAnnotation(classOf[path])
      val path = if (annot == null) f.getName else annot.value
      fieldMapping(path, f.getType, f.getGenericType)
    }.toList.reverse

    def fieldMapping(name: String, fieldType: Class[_], genericType: Type): Mapping = 
      if (primitive_?(fieldType)) Value(name, fieldType)
      else if (fieldType == classOf[List[_]]) makeMapping(Some(name), typeParameter(genericType), true)
      else if (classOf[Option[_]].isAssignableFrom(fieldType))
        if (container_?(genericType)) {
          val types = containerTypes(genericType)
          Optional(fieldMapping(name, types._1, types._2))
        } else Optional(fieldMapping(name, typeParameter(genericType), null))
      else makeMapping(Some(name), fieldType, false)
    memo.memoize(clazz, makeMapping(None, _, false))
  }

  private class Memo[A, R] {
    private var cache = Map[A, R]()

    def memoize(x: A, f: A => R): R = synchronized {
      if (cache contains x) cache(x) else {
        val ret = f(x)
        cache += (x -> ret)
        ret
      }
    }
  }

  object Reflection {
    import java.lang.reflect._

    val primitives = Set[Class[_]](classOf[String], classOf[Int], classOf[Long], classOf[Double], 
                                   classOf[Float], classOf[Byte], classOf[BigInt], classOf[Boolean], 
                                   classOf[Short], classOf[java.lang.Integer], classOf[java.lang.Long], 
                                   classOf[java.lang.Double], classOf[java.lang.Float], 
                                   classOf[java.lang.Byte], classOf[java.lang.Boolean], 
                                   classOf[java.lang.Short], classOf[Date])

    def typeParameter(t: Type): Class[_] = {
      val ptype = t.asInstanceOf[ParameterizedType]
      ptype.getActualTypeArguments()(0).asInstanceOf[Class[_]]
    }

    def containerTypes(t: Type): (Class[_], Type) = {
      val ptype = t.asInstanceOf[ParameterizedType]
      val c = ptype.getActualTypeArguments()(0).asInstanceOf[ParameterizedType]
      val ctype = c.getRawType.asInstanceOf[Class[_]]
      (ctype, c)
    }

    def primitive_?(clazz: Class[_]) = primitives contains clazz
    def static_?(f: Field) = Modifier.isStatic(f.getModifiers)
    def container_?(t: Type) = 
      t.asInstanceOf[ParameterizedType].getActualTypeArguments()(0).isInstanceOf[ParameterizedType]
  }
}
