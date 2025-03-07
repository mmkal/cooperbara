// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a cooper of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cooperbara.shell;

/**
 * Superclass of all exceptions that may be thrown during command execution.
 * It exists to unify them.  It also provides access to the command name
 * and arguments for the failing command.
 */
public class CommandException extends Exception {

  private final Command command;

  /** Returns the command that failed. */
  public Command getCommand() {
    return command;
  }

  public CommandException(Command command, final String message) {
    super(message);
    this.command = command;
  }

  public CommandException(Command command, final Throwable cause) {
    super(cause);
    this.command = command;
  }

  public CommandException(Command command, final String message,
      final Throwable cause) {
    super(message, cause);
    this.command = command;
  }

  private static final long serialVersionUID = 2L;
}
