/*
 * Copyright 2007-2009 WorldWide Conferencing, LLC
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
package net.liftweb.http


import net.liftweb.util.Helpers
import Helpers.{tryo, internetDateFormatter=>internetDateFormat, dateFormatter=>dateFormat, hourFormat, timeFormatter=>timeFormat}
import net.liftweb.common._
import java.util.Date

object ConversionRules extends Factory {
//TODO: REMIND: Mapper should handle null dates
  
  /**
   * A function to format a Date as a date and time ... can be replaced by a function that is user-specific
   */
  val formatDateTime = new FactoryMaker[Date=>String]( () => (d: Date) => internetDateFormat.format(d) ) {}
  
  /**
   * A function to format a Date as a date only
  */
  val formatDate = new FactoryMaker[Date=>String]( () => (d: Date) => dateFormat.format(d) ) {}
  
  /**
   * A function to format a Date as a time. By default uses Helpers.hourFormat which includes seconds but not time zone
  */
  val formatTime = new FactoryMaker[Date=>String]( () => (d: Date) => hourFormat.format(d) ) {}

  /**
   * A function that parses a String representing a date and time into a Date... can be replaced by something that's user-specific
   * By default uses Helpers.toDate which parses either as an "internet date" (date and time) or just a date if that fails.
   */
   val parseDateTime = new FactoryMaker[String=>Box[Date]]( () => (s: String) => tryo { internetDateFormat.parse(s) } ) {}
   
  /**
   * A function that parses a String representing a date into a Date.
   * Defaults to parseDateTime for backward comaptibility
   */
   val parseDate = new FactoryMaker[String=>Box[Date]]( () => (s: String) => tryo { dateFormat.parse(s) } ) {}
  
  /**
   * A function that parses a String representing a time into a Date.
   */
   val parseTime = new FactoryMaker[String=>Box[Date]]( () => (s: String) => tryo { hourFormat.parse(s) } or tryo {timeFormat.parse(s)}) {}
}
