/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package twirl.api

trait Template0[Result] {
  def render(): Result
}

trait Template1[A, Result] {
  def render(a: A): Result
}

trait Template2[A, B, Result] {
  def render(a: A, b: B): Result
}

trait Template3[A, B, C, Result] {
  def render(a: A, b: B, c: C): Result
}

trait Template4[A, B, C, D, Result] {
  def render(a: A, b: B, c: C, d: D): Result
}

trait Template5[A, B, C, D, E, Result] {
  def render(a: A, b: B, c: C, d: D, e: E): Result
}

trait Template6[A, B, C, D, E, F, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F): Result
}

trait Template7[A, B, C, D, E, F, G, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G): Result
}

trait Template8[A, B, C, D, E, F, G, H, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H): Result
}

trait Template9[A, B, C, D, E, F, G, H, I, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I): Result
}

trait Template10[A, B, C, D, E, F, G, H, I, J, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J): Result
}

trait Template11[A, B, C, D, E, F, G, H, I, J, K, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K): Result
}

trait Template12[A, B, C, D, E, F, G, H, I, J, K, L, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L): Result
}

trait Template13[A, B, C, D, E, F, G, H, I, J, K, L, M, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M): Result
}

trait Template14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N): Result
}

trait Template15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O): Result
}

trait Template16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P): Result
}

trait Template17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q): Result
}

trait Template18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R): Result
}

trait Template19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S): Result
}

trait Template20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T): Result
}

trait Template21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T, u: U): Result
}

trait Template22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, Result] {
  def render(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T, u: U, v: V): Result
}
