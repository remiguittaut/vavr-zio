/*
 * Copyright (c) 2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.vavr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.Test;

public class EnvEffectsTest {

  @Test
  public void program() {
    var input = new LinkedList<String>();
    input.add("Toni");
    var output = new LinkedList<String>();

    echo().provide(HasConsole.test(input, output));

    assertEquals(List.of("what's your name?", "Hello Toni"), output);
  }

  public static void main(String[] args) {
    echo().provide(HasConsole.live());
  }

  private static ZIO<HasConsole, Throwable, Nothing> echo() {
    return HasConsole.println("what's your name?")
        .andThen(HasConsole::readln)
        .flatMap(name -> HasConsole.println("Hello " + name));
  }
}

interface HasConsole {

  <R extends HasConsole> HasConsole.Service<R> console();

  static ZIO<HasConsole, Throwable, String> readln() {
    return ZIO.accessM(env -> env.console().readln());
  }

  static ZIO<HasConsole, Throwable, Nothing> println(String text) {
    return ZIO.accessM(env -> env.console().println(text));
  }

  interface Service<R extends HasConsole> {
    ZIO<R, Throwable, String> readln();

    ZIO<R, Throwable, Nothing> println(String text);
  }

  static HasConsole test(Queue<String> input, List<String> output) {
    return new HasConsole() {

      @Override
      public <R extends HasConsole> Service<R> console() {
        return new HasConsole.Service<>() {

          @Override
          public ZIO<R, Throwable, String> readln() {
            return ZIO.from(input::poll);
          }

          @Override
          public ZIO<R, Throwable, Nothing> println(String text) {
            return ZIO.exec(() -> output.add(text));
          }
        };
      }
    };
  }

  static HasConsole live() {
    return new HasConsole() {

      @Override
      public <R extends HasConsole> Service<R> console() {
        return new HasConsole.Service<>() {

          @Override
          public ZIO<R, Throwable, String> readln() {
            return ZIO.from(() -> reader().readLine());
          }

          @Override
          public ZIO<R, Throwable, Nothing> println(String text) {
            return ZIO.exec(() -> writer().println(text));
          }

          private BufferedReader reader() {
            return new BufferedReader(new InputStreamReader(System.in));
          }

          private PrintWriter writer() {
            return new PrintWriter(System.out, true);
          }
        };
      }
    };
  }
}

