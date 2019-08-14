package zio.stream

import ZStream.InputStream

final case class StreamZipper[+E, +A](focus: A, next: InputStream[E, A]) { self =>

  def counit: A = focus

  def coflatMap[B](fa: StreamZipper[E, A] => B): StreamZipper[E, B] = {
    val loop: InputStream[E, B] = next.map { (elem: A) =>
      fa(StreamZipper(elem, next))
    }
    StreamZipper(fa(self), loop)
  }

  def cojoin: StreamZipper[E, StreamZipper[E, A]] = {
    val loop: InputStream[E, StreamZipper[E, A]] = next.map { elem: A =>
      StreamZipper(elem, next)
    }
    StreamZipper(self, loop)
  }

  // def cojoin: StreamZipper[E, StreamZipper[E, A]] =
  //   new StreamZipper[E, StreamZipper[E, A]] {

  //     def focus = self

  //     def left = self.lefts

  //     def right = self.rights
  //   }

  def map[B](f: A => B): StreamZipper[E, B] =
    StreamZipper(f(focus), next.map(f))

  // def lefts: IO[Option[Nothing], StreamZipper[E, A]] =
  //   for {
  //     next <- left
  //     ref  <- Ref.make[InputStream[E, A]](ZIO.succeed(self.focus))
  //   } yield {
  //     new StreamZipper[E, A] {
  //       def focus = next

  //       def left = self.left

  //       def right = ref.modify((_, self.right)).flatten
  //     }
  //   }

  // def rights: IO[Option[E], StreamZipper[E, A]] =
  //   for {
  //     next <- right
  //     ref  <- Ref.make[InputStream[Nothing, A]](ZIO.succeed(self.focus))
  //   } yield {
  //     new StreamZipper[E, A] {
  //       def focus = next

  //       def left = ref.modify((_, self.left)).flatten

  //       def right = self.right
  //     }
  //   }

  // def toBeginning = {
  //   def loop(current: StreamZipper[E, A]): IO[Nothing, StreamZipper[E, A]] =
  //     current.lefts.foldM(
  //       _ => ZIO.succeed(current),
  //       n => loop(n)
  //     )
  //   loop(self)
  // }

  // def cutLeft = new StreamZipper[E, A] {
  //   def focus = self.focus
  //   def left = ZIO.fail(None)
  //   def right = self.right
  // }
}

// package zio.stream

// import zio._

// trait StreamZipper[+E, +A] { self =>

//   def focus: A
//   def lefts: List[A]
//   def rights: List[A]
//   def next: IO[Option[E], A]

//   def counit: A = focus

//   def coflatMap[B](fa: StreamZipper[E, A] => B): StreamZipper[E, B] =
//     cojoin.map(fa)

//   def cojoin: StreamZipper[E, StreamZipper[E, A]] =
//     new StreamZipper[E, StreamZipper[E, A]] {

//       def focus = self
//       def lefts = self.leftZ
//       def rights = self.rightZ
//       def next = self.nextZ
//     }

//   def leftZ: List[StreamZipper[E, A]] = {
//     def loop(current: StreamZipper[E, A], acc: List[StreamZipper[E, A]]): List[StreamZipper[E, A]] =
//       current.moveLeft match {
//         case None => acc.reverse
//         case Some(x) => loop(x, current :: acc)
//       }
//     loop(self, Nil)
//   }

//   def map[B](f: A => B): StreamZipper[E, B] =
//     new StreamZipper[E, B] {
//       def focus = f(self.focus)
//       def lefts = self.lefts.map(f)
//       def rights = self.rights.map(f)
//       def next = self.next.map(f)
//     }

//   def moveLeft: Option[StreamZipper[E, A]] =
//     lefts match {
//       case Nil => None
//       case x :: xs =>
//         Some {
//           new StreamZipper[E, A] {
//             def focus = x

//             def lefts = xs

//             def rights = self.focus :: rights

//             def next = self.next
//           }
//         }
//     }

//   def moveRight: IO[Option[E], StreamZipper[E, A]] =
//     rights match {
//       case x :: xs => ZIO.succeed(new StreamZipper[E, A] {
//         def focus = x
//         def lefts = xs
//         def rights = self.focus :: self.rights
//         def next = self.next
//       })
//       case Nil => next.map { n =>
//         new StreamZipper[E, A] {
//           def focus = n
//           def lefts = self.focus :: self.lefts
//           def rights = self.rights
//           def next = self.next
//         }
//       }
//     }

//   def nextZ: IO[Option[E], StreamZipper[E, A]] =
//     next.map { n =>
//       new StreamZipper[E, A] {
//         def focus = n
//         def lefts = self.focus :: self.lefts
//         def rights = self.rights
//         def next = self.next
//       }
//     }

//   def toBeginning = {
//     def loop(current: StreamZipper[E, A]): StreamZipper[E, A] =
//       current.moveLeft.fold(current)(loop)
//     loop(self)
//   }

//   def prune = new StreamZipper[E, A] {
//     def focus = self.focus
//     def lefts = Nil
//     def rights = self.rights
//     def next = self.next
//   }

//   def rightZ: List[StreamZipper[E, A]] = {
//     def loop(current: StreamZipper[E, A], acc: List[StreamZipper[E, A]]): List[StreamZipper[E, A]] =
//       rights match {
//         case x :: xs =>
//           val next = new StreamZipper[E, A] {
//             def focus = x
//             def lefts = xs
//             def rights = self.focus :: self.rights
//             def next = self.next
//           }
//           loop(next, current :: acc)
//         case _ => Nil
//       }
//       loop(self, Nil)
//   }
// }
