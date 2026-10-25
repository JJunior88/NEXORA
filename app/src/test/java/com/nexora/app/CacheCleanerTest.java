package com.nexora.app;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class CacheCleanerTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    @Test public void removesNestedCacheButKeepsAppDataAndCacheRoot() throws Exception {
        Path cache = folder.newFolder("cache").toPath();
        Path data = folder.newFile("personal-data.txt").toPath();
        Files.write(data, new byte[]{9, 8});
        Path nested = Files.createDirectory(cache.resolve("nested"));
        Files.write(nested.resolve("temporary"), new byte[]{1, 2, 3});
        CacheCleaner.Result result = CacheCleaner.clean(cache);
        assertEquals(3, result.bytes); assertEquals(1, result.files); assertEquals(0, result.failures);
        assertTrue(Files.isDirectory(cache)); assertArrayEquals(new byte[]{9, 8}, Files.readAllBytes(data));
        assertEquals(0, CacheCleaner.clean(cache).bytes);
    }
    @Test public void symlinksCannotEscapeCache() throws Exception {
        Path cache = folder.newFolder("cache").toPath();
        Path outside = folder.newFolder("outside").toPath();
        Files.write(outside.resolve("keep"), new byte[]{4, 5});
        Files.createSymbolicLink(cache.resolve("link"), outside);
        CacheCleaner.Result result = CacheCleaner.clean(cache);
        assertEquals(0, result.bytes);
        assertTrue(Files.exists(outside.resolve("keep")));
    }
    @Test public void missingCacheIsReportedAsFailure() {
        assertEquals(1, CacheCleaner.clean(folder.getRoot().toPath().resolve("missing")).failures);
    }
}
