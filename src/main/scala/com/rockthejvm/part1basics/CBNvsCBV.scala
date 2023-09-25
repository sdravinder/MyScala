package com.rockthejvm.part1basics

import com.sun.tools.javac.Main

object CBNvsCBV {

  // CBV = call by value = arguments are evaluated before function invocation
  def aFunction(arg: Int): Int = arg +1
  val aComputation = aFunction(23 + 67)

  // CBN = call by name = arguments are passed LITERALLY, evaluated at every reference
  def aByNameFunction(arg: => Int): Int = arg + 1
  val anotherComputation = aByNameFunction(23 + 67)

  def printTwiceByValue(x: Long): Unit ={
    println("By value: " + x)
    println("By value: " + x)
  }

  /*
      CBN major features:
      - delayed evaluation of the argument
      - argument is evaluated every time it is used
  */
  def printTwiceByName(x: => Long): Unit = {
    println("By name:" + x)
    println("By name:" + x)
  }

  def inifinite(): Int = 1 + inifinite()
  def printFirst(x: Int, y: => Int) = println(x)

  def main(args: Array[String]): Unit = {
    println(aComputation)
    println(anotherComputation)
    printTwiceByValue(System.nanoTime())
    printTwiceByName(System.nanoTime())
    // printFirst(inifinite(), 42)   --> will by value stack overflow error
    printFirst(42, inifinite())  // in by name value is delete if it's is harmful
  }


}