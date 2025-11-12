package com.myorg.usbparser.service;

import java.io.File;
import java.util.List;

public interface JsonlWriter<T> {
    void write(File outputFile, List<T> data) throws Exception;
}
