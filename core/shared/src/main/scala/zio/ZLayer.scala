/*
 * Copyright 2017-2020 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zio

import zio.internal.Platform

/**
 * A `ZLayer[A, E, B]` describes a layer of an application: every layer in an
 * application requires some services (the input) and produces some services
 * (the output).
 *
 * Layers can be thought of as recipes for producing bundles of services, given
 * their dependencies (other services).
 *
 * Construction of layers can be effectful and utilize resources that must be
 * acquired and safetly released when the services are done being utilized.
 *
 * Because of their excellent composition properties, layers are the idiomatic
 * way in ZIO to create services that depend on other services.
 */
final case class ZLayer[-RIn <: Has[_], +E, +ROut <: Has[_]](value: ZManaged[RIn, E, ROut]) { self =>

  /**
   * Feeds the output services of this layer into the input of the specified
   * layer, resulting in a new layer with the inputs of this layer, and the
   * outputs of the specified layer.
   */
  def >>>[E1 >: E, ROut2 <: Has[_]](that: ZLayer[ROut, E1, ROut2]): ZLayer[RIn, E1, ROut2] =
    ZLayer(self.value.flatMap(v => that.value.provide(v)))

  /**
   * Combines this layer with the specified layer, producing a new layer that
   * has the inputs of both layers, and the outputs of both layers.
   */
  def ++[E1 >: E, RIn2 <: Has[_], ROut2 <: Has[_]](
    that: ZLayer[RIn2, E1, ROut2]
  ): ZLayer[RIn with RIn2, E1, ROut with ROut2] =
    ZLayer(
      ZManaged.accessManaged[RIn with RIn2] { env =>
        (self.value.provide(env) zipWith that.value.provide(env))((l, r) => l.merge[ROut2](r))
      }
    )

  /**
   * Builds a layer that has no dependencies into a managed value.
   */
  def build[RIn2 <: RIn](implicit ev: Has.Any =:= RIn2): Managed[E, ROut] = value.provide(ev(Has.any))

  /**
   * Converts a layer that requires no services into a managed runtime, which
   * can be used to execute effects.
   */
  def toRuntime[RIn2 <: RIn](p: Platform)(implicit ev: Has.Any =:= RIn2): Managed[E, Runtime[ROut]] =
    build.map(Runtime(_, p))
}
object ZLayer {

  def fromEffect[E, A <: Has[_]](zio: IO[E, A]): ZLayer[Has.Any, E, A] = ZLayer(ZManaged.fromEffect(zio))

  def fromFunction[A: Tagged, E, B <: Has[_]](f: A => B): ZLayer[Has[A], E, B] =
    ZLayer(ZManaged.fromEffect(ZIO.access[Has[A]](m => f(m.get[A]))))

  def fromFunction[A0: Tagged, A1: Tagged, E, B <: Has[_]](f: (A0, A1) => B): ZLayer[Has[A0 with A1], E, B] =
    ZLayer(ZManaged.fromEffect {
      for {
        a0 <- ZIO.environment[Has[A0]].map(_.get[A0])
        a1 <- ZIO.environment[Has[A1]].map(_.get[A1])
      } yield f(a0, a1)
    })

  def fromFunction[A0: Tagged, A1: Tagged, A2: Tagged, E, B <: Has[_]](f: (A0, A1, A2) => B): ZLayer[Has[A0 with A1 with A2], E, B] =
    ZLayer(ZManaged.fromEffect {
      for {
        a0 <- ZIO.environment[Has[A0]].map(_.get[A0])
        a1 <- ZIO.environment[Has[A1]].map(_.get[A1])
        a2 <- ZIO.environment[Has[A2]].map(_.get[A2])
      } yield f(a0, a1, a2)
    })

  def fromFunction[A0: Tagged, A1: Tagged, A2: Tagged, A3: Tagged, E, B <: Has[_]](f: (A0, A1, A2, A3) => B): ZLayer[Has[A0 with A1 with A2 with A3], E, B] =
    ZLayer(ZManaged.fromEffect {
      for {
        a0 <- ZIO.environment[Has[A0]].map(_.get[A0])
        a1 <- ZIO.environment[Has[A1]].map(_.get[A1])
        a2 <- ZIO.environment[Has[A2]].map(_.get[A2])
        a3 <- ZIO.environment[Has[A3]].map(_.get[A3])
      } yield f(a0, a1, a2, a3)
    })

  def fromFunction[A0: Tagged, A1: Tagged, A2: Tagged, A3: Tagged, A4: Tagged, E, B <: Has[_]](f: (A0, A1, A2, A3, A4) => B): ZLayer[Has[A0 with A1 with A2 with A3 with A4], E, B] =
    ZLayer(ZManaged.fromEffect {
      for {
        a0 <- ZIO.environment[Has[A0]].map(_.get[A0])
        a1 <- ZIO.environment[Has[A1]].map(_.get[A1])
        a2 <- ZIO.environment[Has[A2]].map(_.get[A2])
        a3 <- ZIO.environment[Has[A3]].map(_.get[A3])
        a4 <- ZIO.environment[Has[A4]].map(_.get[A4])
      } yield f(a0, a1, a2, a3, a4)
    })

  def fromFunctionM[A: Tagged, R <: Has[_], E, B <: Has[_]](f: A => ZIO[R, E, B]): ZLayer[R with Has[A], E, B] =
    ZLayer(ZManaged.fromEffect(ZIO.accessM[R with Has[A]](m => f(m.get[A]))))

  def fromFunctionM[A0: Tagged, A1: Tagged, R <: Has[_], E, B <: Has[_]](f: (A0, A1) => ZIO[R, E, B]): ZLayer[R with Has[A0 with A1], E, B] =
    ZLayer(ZManaged.fromEffect{
      for {
        a0 <- ZIO.environment[Has[A0]].map(_.get[A0])
        a1 <- ZIO.environment[Has[A1]].map(_.get[A1])
        b  <- f(a0, a1)
      } yield b
    })

  def fromFunctionM[A0: Tagged, A1: Tagged, A2: Tagged, R <: Has[_], E, B <: Has[_]](f: (A0, A1, A2) => ZIO[R, E, B]): ZLayer[R with Has[A0 with A1 with A2], E, B] =
    ZLayer(ZManaged.fromEffect{
      for {
        a0 <- ZIO.environment[Has[A0]].map(_.get[A0])
        a1 <- ZIO.environment[Has[A1]].map(_.get[A1])
        a2 <- ZIO.environment[Has[A2]].map(_.get[A2])
        b  <- f(a0, a1, a2)
      } yield b
    })

  def fromFunctionM[A0: Tagged, A1: Tagged, A2: Tagged, A3: Tagged, R <: Has[_], E, B <: Has[_]](f: (A0, A1, A2, A3) => ZIO[R, E, B]): ZLayer[R with Has[A0 with A1 with A2 with A3], E, B] =
    ZLayer(ZManaged.fromEffect{
      for {
        a0 <- ZIO.environment[Has[A0]].map(_.get[A0])
        a1 <- ZIO.environment[Has[A1]].map(_.get[A1])
        a2 <- ZIO.environment[Has[A2]].map(_.get[A2])
        a3 <- ZIO.environment[Has[A3]].map(_.get[A3])
        b  <- f(a0, a1, a2, a3)
      } yield b
    })

  def fromFunctionM[A0: Tagged, A1: Tagged, A2: Tagged, A3: Tagged, A4: Tagged, R <: Has[_], E, B <: Has[_]](f: (A0, A1, A2, A3, A4) => ZIO[R, E, B]): ZLayer[R with Has[A0 with A1 with A2 with A3 with A4], E, B] =
    ZLayer(ZManaged.fromEffect{
      for {
        a0 <- ZIO.environment[Has[A0]].map(_.get[A0])
        a1 <- ZIO.environment[Has[A1]].map(_.get[A1])
        a2 <- ZIO.environment[Has[A2]].map(_.get[A2])
        a3 <- ZIO.environment[Has[A3]].map(_.get[A3])
        a4 <- ZIO.environment[Has[A4]].map(_.get[A4])
        b  <- f(a0, a1, a2, a3, a4)
      } yield b
    })

  def fromFunctionManaged[A: Tagged, E, B <: Has[_]](f: A => Managed[E, B]): ZLayer[Has[A], E, B] =
    ZLayer(ZManaged.accessManaged[Has[A]](m => f(m.get[A])))

  def fromFunctionManaged[A0: Tagged, A1: Tagged, E, B <: Has[_]](f: (A0, A1) => Managed[E, B]): ZLayer[Has[A0 with A1], E, B] =
    ZLayer {
      for {
        a0 <- ZManaged.environment[Has[A0]].map(_.get[A0])
        a1 <- ZManaged.environment[Has[A1]].map(_.get[A1])
        b  <- f(a0, a1)
      } yield b
    }

  def fromFunctionManaged[A0: Tagged, A1: Tagged, A2: Tagged, E, B <: Has[_]](f: (A0, A1, A2) => Managed[E, B]): ZLayer[Has[A0 with A1 with A2], E, B] =
    ZLayer {
      for {
        a0 <- ZManaged.environment[Has[A0]].map(_.get[A0])
        a1 <- ZManaged.environment[Has[A1]].map(_.get[A1])
        a2 <- ZManaged.environment[Has[A2]].map(_.get[A2])
        b  <- f(a0, a1, a2)
      } yield b
    }

  def fromFunctionManaged[A0: Tagged, A1: Tagged, A2: Tagged, A3: Tagged, E, B <: Has[_]](f: (A0, A1, A2, A3) => Managed[E, B]): ZLayer[Has[A0 with A1 with A2 with A3], E, B] =
    ZLayer {
      for {
        a0 <- ZManaged.environment[Has[A0]].map(_.get[A0])
        a1 <- ZManaged.environment[Has[A1]].map(_.get[A1])
        a2 <- ZManaged.environment[Has[A2]].map(_.get[A2])
        a3 <- ZManaged.environment[Has[A3]].map(_.get[A3])
        b  <- f(a0, a1, a2, a3)
      } yield b
    }

  def fromFunctionManaged[A0: Tagged, A1: Tagged, A2: Tagged, A3: Tagged, A4: Tagged, E, B <: Has[_]](f: (A0, A1, A2, A3, A4) => Managed[E, B]): ZLayer[Has[A0 with A1 with A2 with A3 with A4], E, B] =
    ZLayer {
      for {
        a0 <- ZManaged.environment[Has[A0]].map(_.get[A0])
        a1 <- ZManaged.environment[Has[A1]].map(_.get[A1])
        a2 <- ZManaged.environment[Has[A2]].map(_.get[A2])
        a3 <- ZManaged.environment[Has[A3]].map(_.get[A3])
        a4 <- ZManaged.environment[Has[A4]].map(_.get[A4])
        b  <- f(a0, a1, a2, a3, a4)
      } yield b
    }

  def fromManaged[E, A <: Has[_]](m: Managed[E, A]): ZLayer[Has.Any, E, A] = ZLayer(m)

  def succeed[A: Tagged](a: A): ZLayer[Has.Any, Nothing, Has[A]] = ZLayer(ZManaged.succeed(Has(a)))
}
