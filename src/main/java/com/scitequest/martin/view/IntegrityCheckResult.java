package com.scitequest.martin.view;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class IntegrityCheckResult {

    public enum IntegrityCheckError {
        INVALID_MEASUREMENT_DIRECTORY,
        MISSING_IMAGE,
        IMAGE_OPEN_FAILED,
        IO_EXCEPTION,
        NO_FILES_CHECKED,
        MEASUREMENT_EXCEPTION,
    }

    public static final class IntegrityCheckContext {
        public final Path folder;
        public final Path imagePath;

        public IntegrityCheckContext(Path folder, Path imagePath) {
            this.folder = folder;
            this.imagePath = imagePath;
        }
    }

    private final Optional<HashMap<Path, Boolean>> res;
    private final Optional<IntegrityCheckError> err;
    private final IntegrityCheckContext ctx;

    public IntegrityCheckResult(
            Optional<HashMap<Path, Boolean>> res,
            Optional<IntegrityCheckError> err,
            IntegrityCheckContext ctx) {
        this.res = res;
        this.err = err;
        this.ctx = ctx;
    }

    public static IntegrityCheckResult ofCompleted(
            HashMap<Path, Boolean> res, IntegrityCheckContext ctx) {
        if (res == null || ctx == null) {
            throw new IllegalArgumentException("an argument is null");
        }
        if (res.isEmpty()) {
            return new IntegrityCheckResult(
                    Optional.empty(), Optional.of(IntegrityCheckError.NO_FILES_CHECKED), ctx);
        }
        return new IntegrityCheckResult(Optional.of(res), Optional.empty(), ctx);
    }

    public static IntegrityCheckResult ofError(
            IntegrityCheckError err, IntegrityCheckContext ctx) {
        if (err == null || ctx == null) {
            throw new IllegalArgumentException("an argument is null");
        }
        return new IntegrityCheckResult(Optional.empty(), Optional.of(err), ctx);
    }

    public IntegrityCheckContext getContext() {
        return ctx;
    }

    public IntegrityCheckError unwrapErr() {
        return err.get();
    }

    public int unwrapSize() {
        return res.get().size();
    }

    public boolean isOk() {
        return res.isPresent();
    }

    private static boolean resultIsSuccess(HashMap<Path, Boolean> files) {
        return files.values().stream().allMatch(v -> v);
    }

    public boolean isSuccess() {
        return isOk() && resultIsSuccess(res.get());
    }

    public boolean isFailure() {
        return isOk() && !resultIsSuccess(res.get());
    }

    public Set<Path> unwrapValidFiles() {
        return res.get().entrySet().stream()
                .filter(e -> e.getValue() == true)
                .map(Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<Path> unwrapInvalidFiles() {
        return res.get().entrySet().stream()
                .filter(e -> e.getValue() == false)
                .map(Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    public String unwrapAndFormatAsTable() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<Path, Boolean> entry : res.get().entrySet()) {
            String filename = entry.getKey().getFileName().toString();
            String validityStatus = entry.getValue() ? "valid" : "invalid";
            sb.append(filename).append(" - ").append(validityStatus).append(System.lineSeparator());
        }

        return sb.toString();
    }
}
