package com.nexora.app;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/** Deletes only this application's disposable cache; never follows symbolic links. */
final class CacheCleaner {
    static final class Result {
        long bytes;
        int files, failures;
        String summary() {
            return files + " arquivo(s) de cache removido(s), " + Metrics.bytes(bytes)
                    + ". " + failures + " item(ns) não removido(s).";
        }
    }
    static Result clean(Path cache) {
        Result result = new Result();
        if (Files.isSymbolicLink(cache) || !Files.isDirectory(cache)) {
            result.failures++;
            return result;
        }
        try {
            Files.walkFileTree(cache, new SimpleFileVisitor<Path>() {
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        long size = attrs.isRegularFile() ? attrs.size() : 0;
                        if (Files.deleteIfExists(file)) { result.files++; result.bytes += size; }
                    } catch (IOException | SecurityException e) { result.failures++; }
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFileFailed(Path file, IOException e) {
                    result.failures++;
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult postVisitDirectory(Path directory, IOException e) {
                    if (e != null) result.failures++;
                    if (!directory.equals(cache)) {
                        try { Files.deleteIfExists(directory); }
                        catch (IOException | SecurityException failure) { result.failures++; }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException | SecurityException e) { result.failures++; }
        return result;
    }
}
