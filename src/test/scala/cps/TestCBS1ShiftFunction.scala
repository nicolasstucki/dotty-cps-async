package cps

import org.junit.{Test,Ignore}
import org.junit.Assert._

import scala.util._

class TestCBS1ShiftFunction:

  def qqq: Int = 3

/*
  @Test def testAndThen1(): Unit = 
     implicit val printCode = cps.macroFlags.PrintCode
     implicit val debugLevel = cps.macroFlags.DebugLevel(20)
     val c = async[ComputationBound]{
        def add1(x:Int):Int = x+1
        (add1.andThen(x => x + await(T1.cbi(2))))(3)
     }
     assert(c.run() == Success("5"))
*/

