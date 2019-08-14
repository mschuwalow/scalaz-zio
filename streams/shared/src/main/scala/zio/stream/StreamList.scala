package zio.stream

import zio._
import ZStream.InputStream

final case class StreamList[+E, +A](head: A, tail: IO[Option[E], StreamList[E, A]]) { self =>

  def counit: A = head

  def coflatMap[B](f: StreamList[E, A] => B): StreamList[E, B] = {
    def loop(s: StreamList[E, A]): StreamList[E, B] = {
      StreamList(f(s), s.tail.map(loop))
    }

    loop(self)
  }

  def cojoin: StreamList[E, StreamList[E, A]] = {
    def loop(s: StreamList[E, A]): StreamList[E, StreamList[E, A]] = {
      StreamList(s, s.tail.map(loop))
    }
    loop(self)
  }

  def map[B](f: A => B): StreamList[E, B] =
    StreamList(f(head), tail.map(_.map(f)))

  def foldLeft[B](b: B)(f: (B, A) => B): IO[E, B] = {
    if (counit == 0) println(tail)
    tail.foldM({
      case Some(e) => IO.fail(e)
      case None    => ZIO.effectTotal(println("bar")) *> ZIO.succeed(b)
    }, _.foldLeft(f(b, head))(f))
  }

  def toStream: UIO[InputStream[E, A]] =
    RefM.make[IO[Option[E], StreamList[E, A]]](ZIO.succeed(self)).map { ref =>
      ref.modify(_.map(s => (s.head, s.tail)))
    }
}
