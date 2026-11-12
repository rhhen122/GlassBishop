<div align="center">

<img src=".github/GlassBishopLogo.png" alt="Glass Bishop logo" width="200">

# Glass Bishop <img src=".github/Bishop.webp" alt="Bishop" width="30">

### The Glass Cannon of Chess Engines

A positional evaluation engine for chess.

[![Made with Java](https://forthebadge.com/badges/made-with-java.svg)](https://forthebadge.com)
[![Contains Tasty Spaghetti Code](https://forthebadge.com/badges/contains-tasty-spaghetti-code.svg)](https://forthebadge.com)

</div>

<hr/>

## What is Glass Bishop?

Glass Bishop rates how **stupid** a position is. It reads a user's chess position from JSON, constructs a board, analyzes White's material and threats, searches for checkmate in one when White is to move, and reports a 0–100 Stupidity Score and its Stupidity Rating. It also searches legal moves for the side to move and recommends a move using the same chess search as Swing play. A higher Stupidity Score means a more "stupid" position.

The reported Stupidity Score remains White-centric and unchanged. Move recommendations in both JSON analysis and Swing play use the same searcher and position evaluator, with the selected `-depth` applied in either mode. Neither mode searches an unlimited game tree.

## What happens when it runs?

For `java -jar ... position.json`, the program proceeds through these steps:

1. **Read and validate the input.** `PositionJsonReader` parses the JSON, translates piece names and algebraic squares into model objects, and checks required fields, unique occupied squares, king counts, pawn placement, and any supplied special-move state.
2. **Build the position.** `Board` stores an 8-by-8 arrangement plus side-to-move, castling rights, and the en-passant target. Applying a move creates a new board rather than modifying the original.
3. **Analyze the current position.** `PositionAnalyzer` totals White's material, totals the values of White's pieces attacked by Black, and checks for a legal White checkmate-in-one if White is to move.
4. **Search for a move.** `SmartMoveSelector` searches the legal continuations to the requested depth, using the same evaluator and search strategy as Swing play. The separate Stupidity Score is calculated for the current position and the resulting position, but it does not select the move.
5. **Print the result.** The CLI prints the ASCII-art logo, a text board of the current position, its Stupidity Score, the number of legal root moves searched, and the recommended move with its resulting Stupidity Score and rating.

## Stupidity Score

The existing 0–100 scale and rating bands are preserved. The score uses three components:

### 1. White material missing

Piece values are pawn = 1, knight = 3, bishop = 3, rook = 5, queen = 9. A king is not counted as material. A complete standard White army is worth 39 points: eight pawns (8), two knights (6), two bishops (6), two rooks (10), and a queen (9).

If White has less than 39 material points, the missing amount contributes proportionally up to 45 score points. Material above 39 is treated as no missing material; it cannot reduce the score below zero.

### 2. Attacked White pieces

Every White piece whose square is attacked by at least one Black piece contributes its value once to the attacked-material total. Multiple attackers do not count the same White piece multiple times. For this component only, a king under attack is assigned a value of 9; this does not add the king to White's material total. Attacked material is capped at 39 for scoring, and contributes proportionally up to 45 score points.

This is a threat indicator, not an exchange or safety analysis: the engine does not consider whether the attacked piece is defended, whether capturing it would be legal, or whether the attacker is itself vulnerable.

### 3. White checkmate in one

When White is to move, the engine looks at each legal White move. If any move leaves Black in check with no legal response, the position has a White mate-in-one and receives a 10-point bonus. If it is Black's turn, this feature is false and no bonus is applied.

### Combined formula and bounds

`missingMaterial = min(39, max(0, 39 - whiteMaterial))`

`exposedMaterial = min(39, attackedWhiteMaterial)`

`score = clamp(round(45 * missingMaterial / 39 + 45 * exposedMaterial / 39 + (whiteCanMateInOne ? 10 : 0)), 0, 100)`

The result is rounded to the nearest integer, then clamped to 0–100. The mate-in-one bonus is part of the requested Glass Bishop scoring behavior.

For example, the starting-position demo has 39 White material points, no attacked White pieces, and no mate in one, so it scores 0. `white-piece-attacked.json` has 9 White material points and 9 attacked points: its score is `round(45 * 30/39 + 45 * 9/39) = 45`. The mate-in-one demo has 9 White material points, no attacked White pieces, and a mate-in-one bonus, giving `round(45 * 30/39 + 10) = 45`.

| Stupidity Rating          | Stupidity Score |
|---------------------------|-----------------|
| RS - Reverse Stupid       | 0–10            |
| SR - Standard Rating      | 11–19           |
| SS - Slightly Stupid      | 20–29           |
| MS - Moderately Stupid    | 30–39           |
| PB - Probabile Blunder    | 40–49           |
| BS - Blunderous Stupidity | 50–59           |
| LaB - Likely a Blunder    | 60–69           |
| LI - Lethal Idiocy        | 70–79           |
| WB - Walnut Brain         | 80–85           |
| BFC - Banned From Chess   | 86–95           |
| ZE - Zero Elo             | 96–100          |

## Position JSON format

Create a UTF-8 JSON file containing the position to analyze. Coordinates use standard algebraic square names: files `a`–`h`, ranks `1`–`8`, with White's home rank numbered 1. Piece colors and types are case-insensitive; the examples use uppercase. Each piece object has three required string fields:

| Field | Allowed values / meaning |
|---|---|
| `color` | `WHITE` or `BLACK` |
| `type` | `PAWN`, `KNIGHT`, `BISHOP`, `ROOK`, `QUEEN`, or `KING` |
| `square` | One square from `a1` through `h8` |

The top-level `sideToMove` field is required and must be `WHITE` or `BLACK`. Exactly one king of each color must be present. Squares must be unique, and pawns cannot be placed on ranks 1 or 8. This validates the board's structure and special-move metadata; it does not prove that the position could have arisen from a legal game history.

`castlingRights` and `enPassantTarget` are optional. If omitted or empty/null, the engine assumes there are no corresponding rights. Include them only when they genuinely remain available in the position.

```json
{
  "sideToMove": "WHITE",
  "pieces": [
    { "color": "WHITE", "type": "KING", "square": "e1" },
    { "color": "WHITE", "type": "QUEEN", "square": "d1" },
    { "color": "WHITE", "type": "PAWN", "square": "e2" },
    { "color": "BLACK", "type": "KING", "square": "e8" },
    { "color": "BLACK", "type": "ROOK", "square": "a8" }
  ],
  "castlingRights": [],
  "enPassantTarget": null
}
```

Valid `castlingRights` entries are `WHITE_KINGSIDE`, `WHITE_QUEENSIDE`, `BLACK_KINGSIDE`, and `BLACK_QUEENSIDE`. Each supplied right must have the corresponding king and rook on their original squares (`e1` and `h1`/`a1` for White; `e8` and `h8`/`a8` for Black). The move generator also checks that the squares between them are empty and that the king does not castle out of, through, or into check.

`enPassantTarget` is either `null` or the empty square passed over by the immediately preceding two-square pawn move. For example, after White moves a pawn from `e2` to `e4`, the target is `e3` and Black is to move. The target's rank must match the side to move; the just-moved enemy pawn must be behind it, and its original square must now be empty.

### JSON examples

The repository includes three ready-to-run positions in `examples/`:

| File | Demonstrates |
|---|---|
| `starting-position.json` | The standard initial board and all four castling rights |
| `white-piece-attacked.json` | A White queen exposed to a Black rook |
| `white-mate-in-one.json` | A White queen and king position with a legal checkmate in one |

Use the starting position JSON as a template. For custom positions, edit the pieces and update the side to move and special-move fields to match the board.

## Chess rules implemented

### Attack detection

`AttackDetector` checks attacks against a target square by:

- Looking at the two diagonally forward source squares for pawns, in the attacker's direction (pawns attack diagonally even when the target square is empty).
- Checking the eight knight offsets and the adjacent king squares.
- Scanning each orthogonal ray for a rook or queen and each diagonal ray for a bishop or queen. A ray stops at its first occupied square, so pieces do not attack through blockers.

This detector is also used to determine check and to filter illegal moves. Attack maps are pseudo-legal threats: whether a piece could move to a square for attack purposes does not depend on whether moving it would expose its own king.

### Legal move generation

`MoveGenerator` first generates piece-specific candidate moves, then applies each candidate to a temporary board and rejects it if the moving side's king is still in check. This prevents leaving one's own king in check and handles pinned pieces through the same general test.

Implemented move rules include:

- Pawns move one square forward, may move two squares from their starting rank if both squares are clear, and capture one square diagonally.
- A pawn reaching the last rank can promote to a queen, rook, bishop, or knight; all four choices are checked as distinct candidate moves.
- Knights move in L-shaped offsets; kings move one square; bishops, rooks, and queens slide along their legal rays until blocked.
- Capturing the opposing king is never generated as a move. Checkmate instead means that king is in check and has no legal response.
- En passant removes the captured pawn from the board before checking whether the capturing side's king remains safe.
- Castling requires a matching right, the correct king and rook on their home squares, empty intervening squares, and unattacked transit/destination squares. Applying castling moves both king and rook and removes the king's castling rights.
- Moving a king or moving/capturing a rook on its original corner removes the corresponding castling rights. A two-square pawn move sets the next en-passant target; any other move clears it.

### Checkmate and mate-in-one

`PositionAnalyzer.isCheckmate` tests the standard condition: the specified side's king is in check and that side has no legal moves. `whiteCanMateInOne` does not merely test whether a move gives check: it applies each legal White move, then requires Black to be in check and to have zero legal replies. This also naturally handles checkmate by promotion or castling where applicable.

## Build and run

The project uses Maven and Java 21. From the project root, build and run the shaded executable JAR:

```sh
mvn package
java -jar target/GlassBishop-X.X.X.jar path/to/position.json
```

Run without arguments or pass `help` to display the built-in ASCII-art help menu:

```sh
java -jar target/GlassBishop-X.X.X.jar
java -jar target/GlassBishop-X.X.X.jar help
```

Start the Swing game and play White against the engine as Black:

```sh
java -jar target/GlassBishop-X.X.X.jar play -depth=3
```

Convert a sequence of legal UCI moves from the standard starting position into a JSON position:

```sh
java -jar target/GlassBishop-X.X.X.jar convert game-position.json e2e4 e7e5 g1f3
java -jar target/GlassBishop-X.X.X.jar game-position.json
```

Moves are passed as separate UCI arguments (`e2e4`, or for example `a7a8q` for promotion). The converter validates each move in sequence and writes the resulting board, side to move, castling rights, and en-passant target in the format used by JSON analysis. If no moves are supplied, it writes the standard starting position.

Play Glass Bishop against a local Stockfish executable in the GUI:

```sh
java -jar target/GlassBishop-X.X.X.jar stockfish /path/to/stockfish -depth=3
```

Glass Bishop plays White and Stockfish plays Black; both move automatically. The executable must support the UCI protocol. The configured depth is sent to each engine. Close the game window to stop the Stockfish process.

Search depth is optional and defaults to 4. Use any positive integer with `-depth` (the `--depth=3` spelling is also accepted). The same option controls move search in regular JSON evaluation mode. The value is not capped at 5. Higher depths search more plies and can take dramatically longer:

| Depth | Bot search |
|---|---|
| `1` | Searches 1 ply |
| `2` | Searches 2 plies |
| `3` | Searches 3 plies |
| `4` | Searches 4 plies |
| `5` | Searches 5 plies |
| `6` and above | Searches the specified number of plies |

The search uses iterative deepening, alpha-beta pruning, capture/promotion move ordering, quiescence search to extend tactical captures and check evasions, and a transposition table to reuse analyzed positions.

Watch the bot play against itself:

```sh
java -jar target/GlassBishop-X.X.X.jar selfplay -depth=2
```

Self-play opens the same board UI and automatically alternates moves for both colors, showing the evaluation bar, move log, and live search progress. Higher depths make self-play substantially slower.

Click a White piece and then one of its highlighted legal destinations. The board uses large, outlined Unicode chess glyphs (`♔ ♕ ♖ ♗ ♘ ♙` for White and `♚ ♛ ♜ ♝ ♞ ♟` for Black) to keep pieces distinct from both light and dark squares. A move-history panel records each move in standard algebraic notation, including captures, castling, promotions, check, and checkmate, with move numbers; it scrolls as the game progresses. A vertical evaluation bar shows the current position balance (White advantage grows from the bottom; Black advantage grows from the top), with a pawn-value estimate beside it. The engine responds after your move; pawn promotions let you choose a piece using its glyph. The board shows a checkmate or stalemate message when the game ends. This mode requires a desktop environment with a graphical display and a font that supports Unicode chess symbols.

Run a demo:

```sh
java -jar target/GlassBishop-X.X.X.jar examples/starting-position.json -depth=3
java -jar target/GlassBishop-X.X.X.jar examples/starting-position.json
java -jar target/GlassBishop-X.X.X.jar examples/white-piece-attacked.json
java -jar target/GlassBishop-X.X.X.jar examples/white-mate-in-one.json
```

The CLI accepts no arguments or the single argument `help` to print the help menu, `play` to open the human-versus-engine Swing game, `selfplay` to watch two Glass Bishop sides play, `stockfish <binary>` to have Glass Bishop play Stockfish, `convert <output.json> [uci-move ...]` to create a JSON position, or one JSON file path to analyze a position. Game modes accept the optional `-depth=<positive integer>` search option. JSON analysis uses the same search and chess position evaluator as the Glass Bishop game bot, so the same board, side to move, and depth produce the same recommended move. The existing White-centric Stupidity Score and its formula are still reported separately and are not used to select the move. During analysis it prints status updates as it loads the board, calculates the current score, generates legal moves, and searches candidate moves. For a JSON position, it prints an ASCII board (ranks 8 to 1 from top to bottom, files a to h from left to right). White pieces are uppercase (`P N B R Q K`), Black pieces are lowercase (`p n b r q k`), and `.` marks an empty square. It then prints the current position's White material, attacked White material, mate-in-one status, score, and rating. If legal moves exist, it also prints how many it evaluated and the recommended move in coordinate notation (for example `e2e4` or `a7a8q`), plus the resulting Stupidity Score and rating. If the side to move has no legal moves, it reports checkmate or stalemate instead. Invalid argument combinations, unreadable/invalid JSON, or invalid position metadata are reported rather than producing an evaluation.

In Swing play mode the status line reports engine progress: move generation, number of candidates, search depth, the candidate currently being searched, and completion. Updates run while the search is in the background so the window remains responsive.

JSON analysis and Swing play use the same chess search and position evaluation for move recommendations. The resulting position's Stupidity Score is also displayed in JSON analysis, but that score is not the move-selection evaluator. The current position's Stupidity Score formula remains unchanged.

Run the automated tests with:

```sh
mvn test
```

For the project layout, engine conventions, and contribution workflow, see the [contributor guide](CONTRIBUTING.md).

## Project structure

Each package has one responsibility:

| Package / class | Responsibility |
|---|---|
| `org.GlassBishop.Main` | Command-line entry point; loads a JSON path and prints the evaluation |
| `org.GlassBishop.io.PositionJsonReader` | Parses and validates a JSON position |
| `org.GlassBishop.io.PositionJsonWriter` | Writes a board to the JSON position format |
| `org.GlassBishop.model` | Chess domain types: immutable `Piece`, `Square`, `Move`, `Board`, colors, piece types, and castling rights |
| `org.GlassBishop.engine.AttackDetector` | Square attack and check detection |
| `org.GlassBishop.engine.MoveGenerator` | Candidate move generation and legal-move filtering |
| `org.GlassBishop.engine.PositionAnalyzer` | White material, attacked material, checkmate, and mate-in-one analysis |
| `org.GlassBishop.engine.MoveSelector` | Provides one-ply selection using the Stupidity Score |
| `org.GlassBishop.engine.SmartMoveSelector` | Searches legal continuations with iterative deepening, alpha-beta minimax, quiescence, move ordering, and a transposition table |
| `org.GlassBishop.engine.Difficulty` | Provides legacy presets for numeric search depths |
| `org.GlassBishop.engine.PositionEvaluator` | Estimates material, piece activity, threats, and terminal checkmate for search and the UI bar |
| `org.GlassBishop.engine.MoveRecommendation` | Carries the selected move, its resulting score, and number of evaluated legal moves |
| `org.GlassBishop.engine.ChessGame` | Manages game state, validates human moves, and applies engine moves for either color |
| `org.GlassBishop.engine.StockfishEngine` | Communicates with a Stockfish-compatible executable using UCI |
| `org.GlassBishop.engine.ChessNotation` | Formats recorded moves in standard algebraic notation |
| `org.GlassBishop.ui.ChessGameWindow` | Swing board interface for human-versus-engine and engine-versus-engine play |
| `org.GlassBishop.ui.ChessPieceButton` | Renders chess glyphs with contrasting outlines for visibility |
| `org.GlassBishop.ui.EvaluationBar` | Paints a bounded visual representation of White's positional advantage |
| `org.GlassBishop.scoring.StupidityScorer` | Applies the 0–100 scoring formula |
| `org.GlassBishop.scoring.StupidityRating` | Maps scores to the existing rating bands |
| `org.GlassBishop.scoring.ScoreResult` | Carries the score and reported component values |
| `org.GlassBishop.utility.PrintASCII` | Console artwork |
| `org.GlassBishop.utility.BoardPrinter` | Renders the position as a basic text board |
| `src/test/java` | JUnit tests for chess rules and JSON loading |

## Scope and limitations

- Evaluation reports White's material, attacked pieces, and mate in one; it does not evaluate Black's material, positional advantage, or initiative.
- JSON analysis and the Swing-game bot search to the requested positive integer depth using alpha-beta minimax, iterative deepening, quiescence search, capture ordering, and a transposition table. Both use the same lightweight evaluator for move selection, considering material, piece placement, attacks, and checkmate. The Stupidity Score is still calculated and displayed for analysis but remains separate from move selection. There is no configured depth cap, but search cost grows rapidly; large values may take impractically long or exhaust available stack/memory. The engine is not tournament-grade.
- In human-versus-engine Swing play, the human plays White and the bot plays Black. Self-play uses the same engine for both colors.
- The engine does not enforce repetition, the fifty-move rule, insufficient-material draws, move counters, or game history. The supplied board and special-move rights are treated as the complete position.
- The reader checks basic structural consistency, not every requirement for a historically reachable FIDE position.
- Because attacked material measures attacked squares rather than likely captures, it should be interpreted as a simple exposure signal, not as a full chess evaluation.

## Misc

[Logo Asset](https://www.vecteezy.com/png/45686418-black-queen-chess-piece-3d), [Badges](https://forthebadge.com), [Ascii Art](https://www.asciiart.eu/image-to-ascii)
