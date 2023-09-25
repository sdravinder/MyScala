package com.rockthejvm.part3fp

object MapFlatMapFilterFor {

  val aList = List(1, 2, 3)
  val firstElement = aList.head
  val rest = aList.tail

  // map
  val aIncrementedList = aList.map(_ + 1) // List(2,3,4)

  val onlyOddNumber = aList.map(_ % 2 != 0) // List(true, false, true)

  val toPair = (x: Int) => List(x, x + 1)
  val aFlatMappedList = aList.flatMap(toPair) // List(1,2,2,3,3,4)

  val number = List(1, 2, 3, 4)
  val char = List('a', 'b', 'c', 'd')
  val color = List("black", "white", "rad")

  val combinations = number.withFilter(_ % 2 == 0).flatMap(number => char.flatMap(char => color.map(color => s"$number$char-$color")))

  val combinationsFor = for {
    number <- number if number % 2 == 0
    char <- char
    color <- color
  } yield s"$number$char-$color"

  def main(args: Array[String]): Unit = {
    for{
      n <- number
    } println(n)

    number.foreach(println)
    println(combinations)
    println(combinationsFor)
  }

}
