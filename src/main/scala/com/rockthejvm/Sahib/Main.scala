package com.rockthejvm.Sahib

object Main {


  class LivingBeing(name: String) {

    def getAnimalName: String = this.name
  }
  class Animal(name:String, nickName:String){

    def getAnimalName: String = this.name
  }
  class Person(name:String, nickName:String) {

    def getName(): String = this.name
    def getNickname():String = this.nickName

    def blockMethod(temp: Person)(sahib: (Person, Animal) => Unit) = {
      println("First this")
      val personObject = new Person("Anil", "Lic Agent")
      val cocoAnimal = new Animal("Coco", "coco")
      sahib(personObject, cocoAnimal)

      println("After Sahib block")
    }
    def ravinder(temp : Person): Unit = {
      println("temp.name")
    }

    override def toString: String = {
      this.name + " " + this.nickName
    }
    }

  def main(args: Array[String]): Unit = {
    val temp1: Person = new Person("Temp", "tem1")
    val temp: Person = new Person("Temp", "tem1")
    temp.ravinder(temp1)
    temp1.blockMethod(temp) {
      (sahibPerson, rockyAnimal) =>
      println("-------")
      println(sahibPerson.getName())   //
      println("-------")
      println(rockyAnimal.getAnimalName)
      println("-------")
    }
    // -------
    // Temp
    //-------
    // coco

    println("Hello world!")
    // block method --> sahib person ---> ravinder  ---rockyanimal --- horse
    // object type person name--> sahib ninck ---sabiy
    // print starting the block
    // print fullname
    // print block ending
    temp1.blockMethod(temp) { (ravinder, horse) =>
        {
          println(" BLOCK STARTING")
          val sahib = new Person("Sahib","sahiby")
          println(sahib.getName() + " " +sahib.getNickname())
          println("Block ending")

        }

    temp.blockMethod(temp) { (sumit, rabbit) =>
    {
      println("let Start")
      val sumit = new Person("SUMIT", "RABBIT")
      println(sumit.getName() + " " + sumit.getNickname())
      println("let end")
    }

    }
    }
  }



}