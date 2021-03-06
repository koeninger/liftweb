/*
 * Copyright 2009-2010 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.liftweb {
package json {

import java.util.Date
import _root_.org.specs.Specification
import _root_.org.specs.runner.{Runner, JUnit}

class ExtractionExampleTest extends Runner(ExtractionExamples) with JUnit
object ExtractionExamples extends Specification {
  import JsonAST._
  import JsonParser._

  implicit val formats = DefaultFormats

  "Extraction example" in {
    val json = parse(testJson)
    json.extract[Person] mustEqual Person("joe", Address("Bulevard", "Helsinki"), List(Child("Mary", 5, Some(date("2004-09-04T18:06:22Z"))), Child("Mazy", 3, None)))
  }

  "Extraction with path expression example" in {
    val json = parse(testJson)
    (json \ "address").extract[Address] mustEqual Address("Bulevard", "Helsinki")
  }

  "Partial extraction example" in {
    val json = parse(testJson)
    json.extract[SimplePerson] mustEqual SimplePerson("joe", Address("Bulevard", "Helsinki"))
  }

  "Map with primitive values extraction example" in {
    val json = parse(testJson)
    json.extract[PersonWithMap] mustEqual 
      PersonWithMap("joe", Map("street" -> "Bulevard", "city" -> "Helsinki"))
  }

  "Map with object values extraction example" in {
    val json = parse(twoAddresses)
    json.extract[PersonWithAddresses] mustEqual 
      PersonWithAddresses("joe", Map("address1" -> Address("Bulevard", "Helsinki"),
                                     "address2" -> Address("Soho", "London")))
  }

  "Simple value extraction example" in {
    val json = parse(testJson)
    json.extract[Name] mustEqual Name("joe")
    (json \ "children")(0).extract[Name] mustEqual Name("Mary")
    (json \ "children")(1).extract[Name] mustEqual Name("Mazy")
  }

  "Primitive extraction example" in {
    val json = parse(primitives)
    json.extract[Primitives] mustEqual Primitives(124, 123L, 126.5, 127.5.floatValue, "128", 'symb, 125, 129.byteValue, true)
  }

  "Null extraction example" in {
    val json = parse("""{ "name": null, "age": 5, "birthdate": null }""")
    json.extract[Child] mustEqual Child(null, 5, None)
  }

  "Date extraction example" in {
    val json = parse("""{"name":"e1","timestamp":"2009-09-04T18:06:22Z"}""")
    json.extract[Event] mustEqual Event("e1", date("2009-09-04T18:06:22Z"))
  }

  "Option extraction example" in {
    val json = parse("""{ "name": null, "age": 5, "mother":{"name":"Marilyn"}}""")
    json.extract[OChild] mustEqual OChild(None, 5, Some(Parent("Marilyn")), None)
  }

  "Missing JSON array can be extracted as an empty List" in {
    parse(missingChildren).extract[Person] mustEqual Person("joe", Address("Bulevard", "Helsinki"), Nil)
  }

  "Multidimensional array extraction example" in {
    parse(multiDimensionalArrays).extract[MultiDim] mustEqual MultiDim(
      List(List(List(1, 2), List(3)), List(List(4), List(5, 6))), 
      List(List(Name("joe"), Name("mary")), List(Name("mazy"))))
  }

  /* Does not work yet.
  "List extraction example" in {
    val json = parse(testJson)
    (json \ "children").extract[List[Name]] mustEqual List("Mary", "Mazy")
  }
  */

  "Extraction and decomposition are symmetric" in {
    val person = parse(testJson).extract[Person]
    Extraction.decompose(person).extract[Person] mustEqual person
  }

  val testJson = 
"""
{ "name": "joe",
  "address": {
    "street": "Bulevard",
    "city": "Helsinki"
  },
  "children": [
    {
      "name": "Mary",
      "age": 5
      "birthdate": "2004-09-04T18:06:22Z"
    },
    {
      "name": "Mazy",
      "age": 3
    }
  ]
}
"""

  val missingChildren =
"""
{
  "name": "joe",
  "address": {
    "street": "Bulevard",
    "city": "Helsinki"
  }
}
"""

  val twoAddresses =
"""
{
  "name": "joe",
  "addresses": {
    "address1": {
      "street": "Bulevard",
      "city": "Helsinki"
    },
    "address2": {
      "street": "Soho",
      "city": "London"
    }
  }
}
"""

  val primitives = 
"""
{
  "l": 123,
  "i": 124,
  "sh": 125,
  "d": 126.5,
  "f": 127.5,
  "s": "128",
  "b": 129,
  "bool": true,
  "sym":"symb"
}
"""

  val multiDimensionalArrays =
"""
{
  "ints": [[[1, 2], [3]], [[4], [5, 6]]],
  "names": [[{"name": "joe"}, {"name": "mary"}], [[{"name": "mazy"}]]]
}
"""

  def date(s: String) = DefaultFormats.dateFormat.parse(s).get
}

case class Person(name: String, address: Address, children: List[Child])
case class Address(street: String, city: String)
case class Child(name: String, age: Int, birthdate: Option[java.util.Date])

case class SimplePerson(name: String, address: Address)

case class PersonWithMap(name: String, address: Map[String, String])
case class PersonWithAddresses(name: String, addresses: Map[String, Address])

case class Name(name: String)

case class Primitives(i: Int, l: Long, d: Double, f: Float, s: String, sym: Symbol, sh: Short, b: Byte, bool: Boolean)

case class OChild(name: Option[String], age: Int, mother: Option[Parent], father: Option[Parent])
case class Parent(name: String)

case class Event(name: String, timestamp: Date)

case class MultiDim(ints: List[List[List[Int]]], names: List[List[Name]])
}
}
