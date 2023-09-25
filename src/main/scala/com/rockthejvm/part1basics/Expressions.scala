package com.rockthejvm.part1basics

import java.security.KeyStore.TrustedCertificateEntry

object Expressions {

  // expressions are structure that can be evaluated to value
  val meaningOfLife  = 40 +2

  // mathematicl expression: +, -, *, /, &, <<, >>, >>>
  val mathematical = 2 + 3 * 4

  // comparison expressions: <, <=, >, >=, ==,!=
  val equlityTest = 1 == 2

  // boolean expression: !, ||, &&
  val nonEqualityTest = !equlityTest

  // intructions vs expressions
  // expression are evaluted, instruction are executed
  // we think in terms of expressions in scala

  // ifs are expressions
  val aCondition = true
  val anIExpression = if (aCondition) 45 else 99

  //code blocks
  val aCodeBlock = {
    // local values
    val localValue = 78
    // expressions...

    //last expression = value of the block
    /*"return"*/ localValue +54
  }

  // everything is an expression in scala

  /**
   * Exercise:
   * without running the code, what do you think these values will print out?
   */
  // 1
  val someValue = {
    2 < 3
  }

  //2
  val someOtherValue = {
    if (someValue) 239 else 986
    42
  }

  //3
  val yetOtherValue: Unit = println("Scala")

  def main(args: Array[String]): Unit = {
    println(meaningOfLife) // 42
    println(someValue) // true
    println(someOtherValue)  // 42
    println (yetOtherValue) // Scala, ()
  }
}
