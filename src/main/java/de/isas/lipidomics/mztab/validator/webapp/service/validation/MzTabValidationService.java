/*
 * Copyright 2017 Leibniz Institut für Analytische Wissenschaften - ISAS e.V..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.isas.lipidomics.mztab.validator.webapp.service.validation;

import de.isas.lipidomics.mztab.validator.webapp.domain.UserSessionFile;
import de.isas.lipidomics.mztab.validator.webapp.domain.ValidationLevel;
import de.isas.lipidomics.mztab.validator.webapp.domain.ValidationResult;
import de.isas.lipidomics.mztab.validator.webapp.service.StorageService;
import de.isas.lipidomics.mztab.validator.webapp.service.ValidationService;
import de.isas.mztab2.model.ValidationMessage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.pride.jmztab2.utils.errors.MZTabException;

/**
 *
 * @author Nils Hoffmann &lt;nils.hoffmann@isas.de&gt;
 */
@Service
public class MzTabValidationService implements ValidationService {

    private final StorageService storageService;

    @Autowired
    public MzTabValidationService(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public List<ValidationMessage> validate(MzTabVersion mzTabVersion,
        UserSessionFile userSessionFile, int maxErrors,
        ValidationLevel validationLevel, boolean checkCvMapping) {
        Path filepath = storageService.load(userSessionFile);

        try {
            List<ValidationMessage> validationResults = new ArrayList<>();
            validationResults.addAll(
                validate(mzTabVersion, filepath, validationLevel,
                    maxErrors, checkCvMapping));
            return validationResults;
        } catch (IOException ex) {
            if (ex.getCause() instanceof MZTabException) {
                MZTabException mex = (MZTabException) ex.getCause();
                return Arrays.asList(mex.getError().
                    toValidationMessage());
            }
            Logger.getLogger(MzTabValidationService.class.getName()).
                log(java.util.logging.Level.SEVERE, null, ex);
        }
        return Collections.emptyList();
    }

    @Override
    public Map<String, List<Map<String, String>>> parse(
        MzTabVersion mzTabVersion,
        UserSessionFile userSessionFile, int maxErrors,
        ValidationLevel validationLevel) {
        Path filepath = storageService.load(userSessionFile);

        try {
            return parse(mzTabVersion, filepath, validationLevel,
                maxErrors);
        } catch (IOException ex) {
            Logger.getLogger(MzTabValidationService.class.getName()).
                log(java.util.logging.Level.SEVERE, null, ex);
        }
        return Collections.emptyMap();
    }

    private Map<String, List<Map<String, String>>> parse(
        MzTabVersion mzTabVersion,
        Path filepath,
        ValidationLevel validationLevel, int maxErrors) throws IllegalStateException, IOException {
        switch (mzTabVersion) {
            case MZTAB_1_0:
                return new EbiValidator().parse(filepath, validationLevel.
                    name(),
                    maxErrors);
            case MZTAB_2_0:
                return new IsasValidator().parse(filepath, validationLevel.
                    name(),
                    maxErrors);
            default:
                throw new IllegalStateException(
                    "Unsupported mzTab version: " + mzTabVersion.toString());
        }
    }

    private List<ValidationMessage> validate(MzTabVersion mzTabVersion,
        Path filepath,
        ValidationLevel validationLevel, int maxErrors, boolean checkCvMapping) throws IllegalStateException, IOException {
        switch (mzTabVersion) {
            case MZTAB_1_0:
                return new EbiValidator().validate(filepath, validationLevel.
                    name(),
                    maxErrors, checkCvMapping);
            case MZTAB_2_0:
                return new IsasValidator().validate(filepath, validationLevel.
                    name(),
                    maxErrors, checkCvMapping);
            default:
                throw new IllegalStateException(
                    "Unsupported mzTab version: " + mzTabVersion.toString());
        }

    }

    @Override
    public List<ValidationResult> asValidationResults(
        List<ValidationMessage> validationMessage) {
        return validationMessage.stream().
            map((message) ->
            {
                ValidationLevel level = ValidationLevel.valueOf(message.
                    getMessageType().
                    getValue().
                    toUpperCase());
                return new ValidationResult(message.getLineNumber(), message.getCategory().name(), level,
                    message.getMessage(), message.getCode());
            }).
            collect(Collectors.toList());
    }
}
