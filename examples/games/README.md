# Example games

These files contain complete, legal games from the standard starting position. Each `.uci` file is a whitespace-separated sequence of UCI moves; a promotion includes its piece suffix, such as `a7a8q`.

To convert a game to a Glass Bishop position JSON file, pass its moves to the converter:

```sh
xargs java -jar target/GlassBishop-X.X.X.jar convert /tmp/game-position.json < examples/games/fools-mate.uci
java -jar target/GlassBishop-X.X.X.jar /tmp/game-position.json
```

The positions after the final checkmate are terminal, so normal analysis reports no legal move. Remove the final move from the sequence if you want to analyze the position immediately before mate.

| File | Result |
|---|---|
| `fools-mate.uci` | Black checkmates in two moves |
| `scholars-mate.uci` | White checkmates with the queen and bishop |
| `napoleons-mate.uci` | White delivers a quick queen-and-bishop checkmate |
| `morphy-opera-game.uci` | Paul Morphy's 1858 Opera Game, ending in checkmate |

The Morphy Opera Game score is transcribed from [Wikipedia's Opera Game article](https://en.wikipedia.org/wiki/Opera_game). The short mating examples are provided as illustrative legal move sequences.
