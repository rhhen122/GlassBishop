package org.GlassBishop.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.GlassBishop.model.Board;
import org.GlassBishop.model.CastlingRight;
import org.GlassBishop.model.Color;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.PieceType;
import org.GlassBishop.model.Square;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Locale;

public final class PositionJsonReader {
    private final ObjectMapper mapper = new ObjectMapper();

    public Board read(Path path) throws IOException {
        JsonNode root = mapper.readTree(path.toFile());
        if (root == null || !root.isObject()) {
            throw new IOException("Position JSON must be an object");
        }
        JsonNode piecesNode = root.get("pieces");
        if (piecesNode == null || !piecesNode.isArray()) {
            throw new IOException("Position JSON must contain a 'pieces' array");
        }
        Color sideToMove = parseEnum(requiredText(root, "sideToMove"), Color.class, "sideToMove");
        Piece[][] squares = new Piece[8][8];
        int whiteKings = 0;
        int blackKings = 0;
        for (JsonNode node : piecesNode) {
            if (!node.isObject()) {
                throw new IOException("Each piece must be a JSON object");
            }
            Color color = parseEnum(requiredText(node, "color"), Color.class, "piece color");
            PieceType type = parseEnum(requiredText(node, "type"), PieceType.class, "piece type");
            Square square = parseSquare(requiredText(node, "square"));
            if (squares[square.file()][square.rank()] != null) {
                throw new IOException("Multiple pieces occupy square " + square);
            }
            if (type == PieceType.PAWN && (square.rank() == 0 || square.rank() == 7)) {
                throw new IOException("Pawns cannot be placed on the first or eighth rank: " + square);
            }
            squares[square.file()][square.rank()] = new Piece(color, type);
            if (type == PieceType.KING) {
                if (color == Color.WHITE) {
                    whiteKings++;
                } else {
                    blackKings++;
                }
            }
        }
        if (whiteKings != 1 || blackKings != 1) {
            throw new IOException("Position must contain exactly one white king and one black king");
        }

        EnumSet<CastlingRight> rights = EnumSet.noneOf(CastlingRight.class);
        JsonNode rightsNode = root.get("castlingRights");
        if (rightsNode != null && !rightsNode.isNull()) {
            if (!rightsNode.isArray()) {
                throw new IOException("'castlingRights' must be an array");
            }
            for (JsonNode right : rightsNode) {
                if (!right.isTextual()) {
                    throw new IOException("Each castling right must be a string");
                }
                rights.add(parseEnum(right.asText(), CastlingRight.class, "castling right"));
            }
        }
        validateCastlingRights(squares, rights);

        Square enPassantTarget = null;
        JsonNode enPassantNode = root.get("enPassantTarget");
        if (enPassantNode != null && !enPassantNode.isNull()) {
            if (!enPassantNode.isTextual()) {
                throw new IOException("'enPassantTarget' must be a square string or null");
            }
            enPassantTarget = parseSquare(enPassantNode.asText());
            int expectedRank = sideToMove == Color.WHITE ? 5 : 2;
            int capturedPawnRank = enPassantTarget.rank() + (sideToMove == Color.WHITE ? -1 : 1);
            int pawnOriginRank = enPassantTarget.rank() + (sideToMove == Color.WHITE ? 1 : -1);
            if (enPassantTarget.rank() != expectedRank
                    || squares[enPassantTarget.file()][enPassantTarget.rank()] != null
                    || squares[enPassantTarget.file()][capturedPawnRank] == null
                    || squares[enPassantTarget.file()][capturedPawnRank].type() != PieceType.PAWN
                    || squares[enPassantTarget.file()][capturedPawnRank].color() != sideToMove.opposite()
                    || squares[enPassantTarget.file()][pawnOriginRank] != null) {
                throw new IOException("Invalid en passant target: " + enPassantTarget);
            }
        }
        return new Board(squares, sideToMove, rights, enPassantTarget);
    }

    private static String requiredText(JsonNode node, String field) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IOException("Missing or invalid string field '" + field + "'");
        }
        return value.asText();
    }

    private static Square parseSquare(String value) throws IOException {
        try {
            return Square.parse(value);
        } catch (IllegalArgumentException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
    }

    private static void validateCastlingRights(Piece[][] squares, EnumSet<CastlingRight> rights)
            throws IOException {
        for (CastlingRight right : rights) {
            Color color = right.name().startsWith("WHITE") ? Color.WHITE : Color.BLACK;
            int rank = color == Color.WHITE ? 0 : 7;
            int rookFile = right.name().endsWith("KINGSIDE") ? 7 : 0;
            Piece king = squares[4][rank];
            Piece rook = squares[rookFile][rank];
            if (king == null || king.color() != color || king.type() != PieceType.KING
                    || rook == null || rook.color() != color || rook.type() != PieceType.ROOK) {
                throw new IOException("Castling right " + right + " requires its king and rook on their home squares");
            }
        }
    }

    private static <T extends Enum<T>> T parseEnum(String value, Class<T> type, String field) throws IOException {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid " + field + ": " + value, exception);
        }
    }
}
