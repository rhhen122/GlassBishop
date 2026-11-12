package org.GlassBishop.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StockfishEngine implements AutoCloseable {
    private static final String END_OF_OUTPUT = "\u0000";
    private static final Duration HANDSHAKE_TIMEOUT = Duration.ofSeconds(15);

    private final Process process;
    private final BufferedWriter input;
    private final BlockingQueue<String> output = new LinkedBlockingQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public StockfishEngine(Path binary) throws IOException {
        if (!Files.isRegularFile(binary) || !Files.isExecutable(binary)) {
            throw new IOException("Stockfish binary is not an executable file: " + binary);
        }
        process = new ProcessBuilder(binary.toAbsolutePath().toString())
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        input = new BufferedWriter(new OutputStreamWriter(
                process.getOutputStream(), StandardCharsets.UTF_8));
        Thread outputReader = new Thread(() -> readOutput(process), "stockfish-output");
        outputReader.setDaemon(true);
        outputReader.start();
        try {
            send("uci");
            await("uciok", HANDSHAKE_TIMEOUT);
            send("isready");
            await("readyok", HANDSHAKE_TIMEOUT);
        } catch (IOException exception) {
            close();
            throw exception;
        }
    }

    public String bestMove(List<String> moves, int depth) throws IOException {
        if (depth < 1) {
            throw new IllegalArgumentException("Search depth must be a positive integer.");
        }
        String position = moves.isEmpty() ? "position startpos"
                : "position startpos moves " + String.join(" ", moves);
        send(position);
        send("go depth " + depth);
        while (true) {
            String line = nextOutput(null);
            if (line.startsWith("bestmove ")) {
                String[] parts = line.split("\\s+");
                if (parts.length < 2 || parts[1].equals("(none)") || parts[1].equals("0000")) {
                    throw new IOException("Stockfish did not return a legal move.");
                }
                return parts[1];
            }
        }
    }

    private void send(String command) throws IOException {
        if (closed.get() || !process.isAlive()) {
            throw new IOException("Stockfish process is not running.");
        }
        synchronized (input) {
            input.write(command);
            input.newLine();
            input.flush();
        }
    }

    private void await(String expected, Duration timeout) throws IOException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                throw new IOException("Timed out waiting for Stockfish " + expected + ".");
            }
            String line = nextOutput(Duration.ofNanos(remaining));
            if (line.equals(expected)) {
                return;
            }
        }
    }

    private String nextOutput(Duration timeout) throws IOException {
        try {
            String line = timeout == null ? output.take()
                    : output.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
            if (line == null) {
                throw new IOException("Timed out waiting for Stockfish response.");
            }
            if (line.equals(END_OF_OUTPUT)) {
                throw new IOException("Stockfish process exited unexpectedly.");
            }
            return line;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for Stockfish.", exception);
        }
    }

    private void readOutput(Process stockfish) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stockfish.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.put(line);
            }
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } finally {
            output.offer(END_OF_OUTPUT);
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            synchronized (input) {
                input.write("quit");
                input.newLine();
                input.flush();
            }
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (IOException exception) {
            process.destroyForcibly();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }
}
