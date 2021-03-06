package cps
 
import scala.collection.ArrayOps
import scala.collection.IterableOps

trait AsyncShift[T]

trait AsyncShiftLowPriority1 {

 transparent inline given shiftedIterable[A,CA <: Iterable[A] ] as AsyncShift[CA] =
      new cps.runtime.IterableAsyncShift[A,CA]()

 //transparent inline given shiftedRange as AsyncShift[Range] =
 //     new cps.runtime.IterableAsyncShift[A,Range]()

}

object AsyncShift extends AsyncShiftLowPriority1 {

 transparent inline given shiftedArrayOps[A] as AsyncShift[scala.collection.ArrayOps[A]] = 
      new cps.runtime.ArrayOpsAsyncShift[A]()

 transparent inline given shiftedIterableOps[A,C[X] <: Iterable[X] & IterableOps[X,C,C[X]] ] as AsyncShift[C[A]] =
      new cps.runtime.IterableOpsAsyncShift[A,C]()

 transparent inline given shiftedList[A] as AsyncShift[scala.collection.immutable.List[A]] =
      new cps.runtime.ListAsyncShift[A]()

 transparent inline given shiftedCpsMonad[F[_], M <: CpsMonad[F]](using CpsMonad[F]) as AsyncShift[M] = new cps.runtime.CpsMonadSelfAsyncShift[F,M]

 transparent inline given shiftedOption[A] as AsyncShift[Option[A]] =
      new cps.runtime.OptionAsyncShift[A]()

 transparent inline given shiftedFunction1[A,B] as AsyncShift[Function1[A,B]] =
      cps.runtime.Function1AsyncShift[A,B]()

 transparent inline given shiftedTry[A] as AsyncShift[scala.util.Try[A]] =
      new cps.runtime.util.TryAsyncShift[A]()

 transparent inline given shiftedUsing as AsyncShift[scala.util.Using.type] =
       cps.runtime.util.UsingAsyncShift

 transparent inline given shiftedEither[A,B] as AsyncShift[Either[A,B]] =
      cps.runtime.util.EitherAsyncShift[A,B]()

 transparent inline given shiftedEitherLeftProjection[A,B] as AsyncShift[Either.LeftProjection[A,B]] =
      cps.runtime.util.EitherLeftProjectionAsyncShift[A,B]()

}

trait AsyncShifted[T,F[_]]

trait ObjectAsyncShift[T]
{
  //def apply[F[_]](obj:T, cpsMonad: CpsMonad[F]): AsyncShifted[T,F]
}


