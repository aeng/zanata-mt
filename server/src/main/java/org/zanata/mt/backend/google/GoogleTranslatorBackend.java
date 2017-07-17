/*
 * Copyright 2017, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.mt.backend.google;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.zanata.mt.annotation.DevMode;
import org.zanata.mt.api.dto.LocaleCode;
import org.zanata.mt.backend.BackendLocaleCode;
import org.zanata.mt.backend.google.internal.dto.GoogleLocaleCode;
import org.zanata.mt.exception.ZanataMTException;
import org.zanata.mt.model.AugmentedTranslation;
import org.zanata.mt.service.ConfigurationService;
import org.zanata.mt.service.TranslatorBackend;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
public class GoogleTranslatorBackend implements TranslatorBackend {

    // Max length per request for Google service
    private final static int MAX_LENGTH = 5000;

    private Translate translate;

    /**
     * Map from request locale to Google supported locale code
     *
     * https://cloud.google.com/translate/docs/languages
     */
    private final ImmutableMap<LocaleCode, GoogleLocaleCode> LOCALE_MAP =
            ImmutableMap.of(
                    LocaleCode.ZH_HANS, new GoogleLocaleCode("zh-CN"),
                    LocaleCode.ZH_HANT, new GoogleLocaleCode("zh-TW")
            );

    @Inject
    public GoogleTranslatorBackend(ConfigurationService configurationService, @DevMode boolean isDevMode) {
        if (!isDevMode && !configurationService.getGoogleADC().exists()) {
            throw new ZanataMTException("google application default credential is not defined");
        }
        translate = TranslateOptions.getDefaultInstance().getService();
    }

    @Override
    public AugmentedTranslation translate(String content,
            BackendLocaleCode srcLocale, BackendLocaleCode targetLocale,
            MediaType mediaType) throws ZanataMTException {
        return translate(Lists.newArrayList(content), srcLocale, targetLocale,
                mediaType).get(0);
    }

    @Override
    public List<AugmentedTranslation> translate(List<String> contents,
            BackendLocaleCode srcLocale, BackendLocaleCode targetLocale,
            MediaType mediaType) throws ZanataMTException {
        try {
            List<Translation> translations =
                    translate.translate(
                            contents,
                            Translate.TranslateOption
                                    .sourceLanguage(srcLocale.getLocaleCode()),
                            Translate.TranslateOption
                                    .targetLanguage(
                                            targetLocale.getLocaleCode()));
            return translations.stream()
                    .map(translation -> new AugmentedTranslation(
                            translation.getTranslatedText(),
                            translation.toString())).collect(
                            Collectors.toList());
        } catch (Exception e) {
            throw new ZanataMTException(
                    "Unable to get translations from Google API", e);
        }
    }

    @Override
    public BackendLocaleCode getMappedLocale(LocaleCode localeCode) {
        return LOCALE_MAP.get(localeCode);
    }

    @Override
    public int getCharLimitPerRequest() {
        return MAX_LENGTH;
    }
}
