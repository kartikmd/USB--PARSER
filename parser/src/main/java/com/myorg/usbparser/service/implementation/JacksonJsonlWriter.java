package com.myorg.usbparser.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.myorg.usbparser.service.JsonlWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Writes objects as JSON Lines (JSONL) using Jackson.
 */
@Slf4j
public class JacksonJsonlWriter<T> implements JsonlWriter<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

    private static final ObjectWriter OBJECT_WRITER = MAPPER.writer();

    @Override
    public void write(File outputFile, List<T> data) throws IOException {
        // Defensive checks
        if (outputFile == null) {
            throw new IllegalArgumentException("outputFile must not be null");
        }
        if (data == null || data.isEmpty()) {
            log.warn("No data provided, skipping write for file: {}", outputFile.getAbsolutePath());
            return;
        }

        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            log.warn("Could not create parent directories: {}", parent.getAbsolutePath());
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {

            for (T obj : data) {
                writer.write(OBJECT_WRITER.writeValueAsString(obj));
                writer.write('\n');
            }
            writer.flush();
            log.info("JSONL written: {} entries -> {}", data.size(), outputFile.getAbsolutePath());
        }
    }
}
