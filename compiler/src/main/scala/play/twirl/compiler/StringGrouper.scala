/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.twirl.compiler

/**
 * Groups sub sections of Strings.
 * Basically implements String.grouped, except that it guarantees that it won't break surrogate pairs.
 */
object StringGrouper {

  /**
   * Group the given string by the given size.
   *
   * @param s The string to group.
   * @param n The size of the groups.
   * @return A list of strings, grouped by the specific size.
   */
  def apply(s: String, n: Int): List[String] = {
    if (s.length <= n + 1 /* because we'll split at n + 1 if character n - 1 is a high surrogate */ ) {
      List(s)
    } else {
      val parts = if (s.charAt(n - 1).isHighSurrogate) {
        s.splitAt(n + 1)
      } else {
        s.splitAt(n)
      }
      parts._1 :: apply(parts._2, n)
    }
  }

}