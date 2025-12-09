package com.racephotos.service.storage;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class S3StringUtils {
    public static String sanitizeOptionalFolder(String folderName) {
        if (folderName == null || folderName.isBlank()) {
            return null;
        }
        String trimmed = folderName.trim();
        // Replace backslashes to avoid directory traversal semantics
        trimmed = trimmed.replace('\\', '/');
        // Collapse any duplicate slashes
        trimmed = trimmed.replaceAll("/{2,}", "/");
        // Strip leading/trailing slashes
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            return null;
        }
        // Validate characters: allow word chars, dash, underscore, dot, and forward slash for subfolders
        if (!trimmed.matches("[A-Za-z0-9._\\-/]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder name contains invalid characters");
        }
        // Prevent path traversal segments
        if (trimmed.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder name must not contain '..'");
        }
        // Replace any spaces to keep keys URL-safe
        if (trimmed.contains(" ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder name must not contain spaces");
        }
        return trimmed;
    }

    public static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Object name is blank");
        }
        String trimmed = name.trim();
        // Strip any path components to avoid directory traversal
        int lastSlash = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < trimmed.length() - 1) {
            trimmed = trimmed.substring(lastSlash + 1);
        }
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid object name");
        }
        return trimmed;
    }

    public static String sanitizePathSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path segment is blank");
        }
        String sanitized = segment.trim();
        while (sanitized.startsWith("/")) {
            sanitized = sanitized.substring(1);
        }
        while (sanitized.endsWith("/")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "-");
        if (sanitized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid path segment");
        }
        return sanitized;
    }
}
