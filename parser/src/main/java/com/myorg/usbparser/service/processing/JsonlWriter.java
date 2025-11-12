package com.myorg.usbparser.service.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.myorg.usbparser.model.Section;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public final class JsonlWriter {
    private static final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonlWriter() {}

    public static void writeJsonl(List<Section> sections, File outFile) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(outFile))) {
            for (Section s : sections) {
                String json = mapper.writeValueAsString(s);
                w.write(json);
                w.write('\n');
            }
        }
    }
}
