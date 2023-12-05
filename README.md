# Advent of Code Runner

###### By RcCookie

---

A small framework for executing Advent of code tasks, which runs a program of your choice (not just Java) with automatic input download, output submission and more.
If you do use Java, you also get several utility functions which implement simple, but annoying things that you have to do regularly in AoC puzzles.
Some of the main features include:

 - Automatic input loading based on date
 - Automatic detection between first and second part of puzzle
 - Automatic submission of solution including feedback
 - For Java implementations:
   - Automatic class loading based on date and a customizable name pattern
   - Several utility methods which are commonly used when solving Advent of Code puzzles
 - For external programs: 
   - Run external program instead based on date and customizable name and input pattern

## For Java Implementations

### Installation

Use the maven artefact `de.rccookie:advent-of-code-runner:1.0.0` to include it
you project.
```xml
<dependency>
   <groupId>de.rccookie</groupId>
   <artifactId>advent-of-code-runner</artifactId>
   <version>1.0.0</version>
</dependency>
```
You also need to add the repository used to distribute it:
```xml
<repository>
   <id>rccookie.de</id>
   <url>https://mvn.repo.rccookie.de/releases</url>
</repository>
```

### Configuration

To be able to function properly, a `config.json` file should be present in the working directory
of the program, and file `token.txt` unless otherwise specified in the config file. Alternatively,
a config file can be passed to the program with the `--config` option. The config file should contain
the following fields:

 - `"classPattern"`: The fully qualified name pattern of your solution classes
   that you want to use. The name can include `{day}`, `{0_day}`, `{year}` and `{full_year}`,
   which will be replaced with the day of month, day of month padded with 0 if needed,
   the last two digits of the year, or the full year number, respectively. This pattern
   will be used to find and initiate an instance of your solution class for a specific
   puzzle. For example, the pattern `"de.rccookie.aoc._{year}.Solution{day}"` would match
   the class `de.rccookie.aoc._23.Solution24`, if the date was the 24th december 2023.
 - Optional: `"token"`: The file that contains the session token used to authenticate 
   on adventofcode.com. This token can be found by logging in on the website and then
   opening the browser devtools. In the category "storage" you can see the cookies of
   the website, which should include the `"session"` cookie. The default value of this
   is `"token.txt"`.
 - Optionally: `"showInputStats"`: Boolean, which can be used to control whether to
   print a short input summary at the beginning of execution. Enabled by default.

Additionally, some settings can be configured by changing the command line parameters.
A full list of possible options can be shown using `--help`. Alternatively, you can
define another main method and call one of the `Solution.run()` methods.

### Usage

To create an implementation, you just need to create a subclass of the abstract class
`de.rccookie.aoc.Solution`. An implementation only needs to implement the `task1()` method,
the class will automatically detect that `task2()` is not yet implemented and will run task 1.
Once `task2()` gets overridden (the base method does not need / shouldn't be called),
that task will automatically be run instead of task 1. In any case only one of
`task1()` and `task2()` will be executed on the same solution instance.

Implementations of this class must provide a parameter-less constructor, such that an
instance of the class can be created on demand.

## For Non-Java Implementations

### Installation

Simply download the latest release from the repository.
You can get the plain jar file or, for Windows, an exe wrapper for the jar file.
You need to have Java 17 or higher installed on your system.

### Configuration

To be able to function properly, a `config.json` file should be present in the working directory
of the program, and file `token.txt` unless otherwise specified in the config file. Alternatively,
a config file can be passed to the program with the `--config` option. The config file should contain
the following fields:

- `"command"`: The command pattern to execute to run your program. This pattern can
  (and should) have the following macros which will be replaced on execution:
    - `{day}`: The day of month of the puzzle
    - `{0_day}`: The day of month of the puzzle, padded with
      a zero if necessary
    - `{year}`: The year of puzzle mod 100 (the last two digits)
    - `{full_year}`: The year of the puzzle
    - `{task}`: The task number (1 or 2) describing the subtask of the puzzle to run
    - `{file}`: The filename of the input file (note that the input file is also piped in as stdin
    - `{abs_file}`: The absolute filename of the input file
    - `{input}`: The contents of the input file (to be passed as parameter to the program) (note that the input file is also piped in as stdin)
- Optional: `"token"`: The file that contains the session token used to authenticate
  on adventofcode.com. This token can be found by logging in on the website and then
  opening the browser devtools. In the category "storage" you can see the cookies of
  the website, which should include the `"session"` cookie. The default value of this
  is `"token.txt"`.
- Optionally: `"showInputStats"`: Boolean, which can be used to control whether to
  print a short input summary at the beginning of execution. Enabled by default.

Additionally, some settings can be configured by changing the command line parameters.
A full list of possible options can be shown using `--help`.

### Input and output

Your program will receive the puzzle input via the standard input stream.
You can also make use of the `{input}` macro to pass the input as an args
parameter (don't enquote `{input}`!). A simple example command could be:
```
python {year}/{day}/task{task}.py {input}
-- or --
python {year}/day{day}.py --input {input} --task {task}
```
If it was the 24th in the year 2023 and you have completed the first part of the puzzle,
the first command would cause the command `python 23/24/task2.py 'input...'` to be executed,
the second one would execute `python 23/day24.py --input 'input...' --task 2`.

To return a result which will then, if you press enter, be submitted on the website,
you simply have to print the result into the standard output stream. The last printed
line will be treated as result (anything else is irrelevant). And don't worry, **any
output or error messages from your program will be displayed regularly.**

### Usage

To execute the jar file, run `java -jar aoc-run.jar [options]`. If you use the exe
or create a simple shell script, you can then also simply run
`aoc-run[.exe] [options]`. If a config file is present and well-configured, you should
not need to pass any additional parameters - day, year and subtask is determined
automatically! You can also always run `aoc-run --help` for more info on command
line options.
