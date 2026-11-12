package org.GlassBishop.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.GlassBishop.model.Board;
import org.GlassBishop.model.CastlingRight;
import org.GlassBishop.model.Piece;
import org.GlassBishop.model.Square;

import java.io.IOException;
import java.nio.file.Path;

public final class PositionJsonWriter {
    private final ObjectMapper mapper = new ObjectMapper();

    public void write(Board board, Path path) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("sideToMove", board.sideToMove().name());
        ArrayNode pieces = root.putArray("pieces");
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Square square = new Square(file, rank);
                Piece piece = board.pieceAt(square);
                if (piece == null) {
                    continue;
                }
                ObjectNode pieceNode = pieces.addObject();
                pieceNode.put("color", piece.color().name());
                pieceNode.put("type", piece.type().name());
                pieceNode.put("square", square.toString());
            }
        }
        ArrayNode castlingRights = root.putArray("castlingRights");
        for (CastlingRight right : CastlingRight.values()) {
            if (board.castlingRights().contains(right)) {
                castlingRights.add(right.name());
            }
        }
        if (board.enPassantTarget() == null) {
            root.putNull("enPassantTarget");
        } else {
            root.put("enPassantTarget", board.enPassantTarget().toString());
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), root);
    }
}
