package com.myorg.usbparser.service;

import com.myorg.usbparser.model.Section;
import com.myorg.usbparser.model.ValidationResult;

import java.util.List;

public interface Validator {
    ValidationResult validate(List<Section> tocSections, List<Section> parsedSections) throws Exception;
}
