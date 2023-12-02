# Advent of Code Runner

###### By RcCookie

---

This package contains a base class for classes that implement a solution for a day of Advent of Code,
which eases the development. Some of the main features include:

 - Automatic input loading based on date
 - Automatic class loading based on date and a customizable name pattern
 - Automatic detection between first and second part of puzzle
 - Automatic submission of solution including feedback
 - Several utility methods which are commonly used when solving Advent of Code puzzles

## Configuration

To be able to function properly, a `config.json` file should be present in the working directory
of the program. Alternatively, a config file can be passed to the program with the
`--config` option. The config file should contain the following fields:

 - `"token"`: The session token used to authenticate on adventofcode.com. This token
   can be found by logging in on the website and then opening the browser devtools.
   In the category "storage" you can see the cookies of the website, which should
   include the "session" cookie.
 - `"classPattern"`: The fully qualified name pattern of your solution classes
   that you want to use. The name can include `{day}`, `{0_day}`, `{year}` and `{full_year}`,
   which will be replaced with the day of month, day of month padded with 0 if needed,
   the last two digits of the year, or the full year number, respectively. This pattern
   will be used to find and initiate an instance of your solution class for a specific
   puzzle. For example, the pattern `"de.rccookie.aoc._{year}.Solution{day}"` would match
   the class `de.rccookie.aoc._23.Solution24`, if the date was the 24th december 2023.
 - Optionally: `"showInputStats"`: Boolean, which can be used to control whether to
   print a short input summary at the beginning of execution. Enabled by default.

Additionally, some settings can be configured by changing the command line parameters.
A full list of possible options can be shown using `--help`. Alternatively, you can
define another main method and call one of the `Solution.run()` methods.

## Usage

To create an implementation, you just need to create a subclass of the abstract class
`de.rccookie.aoc.Solution`. An implementation only needs to implement the `task1()` method,
the class will automatically detect that `task2()` is not yet implemented and will run task 1.
Once `task2()` gets overridden (the base method does not need / shouldn't be called),
that task will automatically be run instead of task 1. In any case only one of
`task1()` and `task2()` will be executed on the same solution instance.

Implementations of this class must provide a parameter-less constructor, such that an
instance of the class can be created on demand.
