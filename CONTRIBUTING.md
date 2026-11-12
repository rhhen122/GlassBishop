# Contributing to Glass Bishop

Thanks for helping improve Glass Bishop. This guide covers the project layout, the conventions used by the chess engine, and how to test a change before opening a pull request.

## Set up the project

Glass Bishop uses Java 21 and Maven. You will need a Java 21 JDK and Maven installed. From the repository root, run:

```sh
mvn test
```

To build the executable JAR:

```sh
mvn package
```

The project has no separate formatter or linter configured. Follow the style of the surrounding code and keep changes focused.

## Find the right place for a change

Production code lives under `src/main/java/org/GlassBishop`:

| Package | Responsibility |
|---|---|
| `model` | Position and move types, including `Board`, `Move`, `Piece`, and `Square` |
| `engine` | Legal move generation, attack detection, position analysis, evaluation, and search |
| `io` | Reading and writing engine position JSON |
| `scoring` | White-centric Stupidity Score and ratings |
| `ui` | Swing game window and display components |
| `utility` | Text board and console output helpers |

The command-line entry point is `Main`. Tests live under the matching package in `src/test/java`; for example, engine tests belong in `src/test/java/org/GlassBishop/engine`.

## Engine conventions

- **Keep the model immutable.** `Board` copies its piece array and `Board.apply(move)` returns a new board. Prefer deriving the next position over mutating the current one.
- **Use the legal move generator.** `MoveGenerator.legalMoves(board, color)` filters out moves that leave that side's king in check. Do not duplicate its rules in search, UI, or input handling.
- **Do not generate captures of a king.** Checkmate is represented by check and no legal replies, not by removing a king.
- **Keep special-move state accurate.** When changing move application or position serialization, cover castling rights, en-passant targets, and promotion as well as ordinary moves.
- **Respect evaluation perspective.** `PositionEvaluator` returns a score from White's perspective; the search maximizes for White and minimizes for Black. The separate `StupidityScorer` is a White-focused reporting score and is not the move selector's evaluator.
- **Use coordinate notation for machine moves.** `Move.toUci()` is the engine-facing format (`e2e4`, `a7a8q`). `ChessNotation` formats human-readable SAN for the game log.
- **Keep external engine communication at the boundary.** Stockfish integration uses UCI in `StockfishEngine`; validate its returned move against Glass Bishop's legal moves before applying it.
- **Report invalid input clearly.** Follow the existing reader and CLI patterns so malformed positions, illegal moves, and process errors are not silently ignored.

## Making an engine change

For a rules change, locate the relevant logic in `MoveGenerator`, `AttackDetector`, `Board`, or `PositionAnalyzer`. Add tests for the rule itself and any relevant edge cases. Useful cases include both colors, captures and blocked paths, check and pins, promotions, castling, and en passant.

For a search or evaluation change, use `SmartMoveSelectorTest` and `PositionAnalyzerTest` as examples. Prefer positions that demonstrate the expected decision or score, including positions where Black is to move. Keep the distinction between move-selection evaluation and the displayed Stupidity Score intact unless the change explicitly targets both.

For input/output or CLI changes, test successful round trips as well as malformed input. Position JSON must preserve the side to move and special-move metadata; move-sequence conversion starts from the standard starting position and accepts legal UCI moves.

Add a regression test for each bug fix where practical. Tests should assert observable behavior rather than private implementation details.

## Verify and submit

Before submitting, run:

```sh
mvn test
```

For changes that affect packaging, the CLI, or runtime dependencies, also run:

```sh
mvn package
```

Review the diff to confirm it contains only the intended changes. In your pull request description, explain the behavior change, note any limitations or compatibility considerations, and summarize the tests you ran.
