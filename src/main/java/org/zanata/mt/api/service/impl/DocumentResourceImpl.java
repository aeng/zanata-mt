package org.zanata.mt.api.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zanata.mt.api.dto.APIResponse;
import org.zanata.mt.api.dto.DocumentContent;
import org.zanata.mt.api.dto.DocumentStatistics;
import org.zanata.mt.api.dto.LocaleId;
import org.zanata.mt.api.dto.TypeString;
import org.zanata.mt.api.service.DocumentResource;
import org.zanata.mt.dao.DocumentDAO;
import org.zanata.mt.dao.LocaleDAO;
import org.zanata.mt.model.BackendID;
import org.zanata.mt.model.Document;
import org.zanata.mt.model.Locale;
import org.zanata.mt.process.DocumentProcessKey;
import org.zanata.mt.process.DocumentProcessManager;
import org.zanata.mt.service.DateRange;
import org.zanata.mt.service.DocumentContentTranslatorService;
import org.zanata.mt.util.UrlUtil;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
@RequestScoped
public class DocumentResourceImpl implements DocumentResource {
    private static final Logger LOG =
            LoggerFactory.getLogger(DocumentResourceImpl.class);

    private DocumentContentTranslatorService documentContentTranslatorService;

    private LocaleDAO localeDAO;

    private DocumentDAO documentDAO;

    private DocumentProcessManager docProcessManager;

    @SuppressWarnings("unused")
    public DocumentResourceImpl() {
    }

    @Inject
    public DocumentResourceImpl(
            DocumentContentTranslatorService documentContentTranslatorService,
            LocaleDAO localeDAO, DocumentDAO documentDAO,
            DocumentProcessManager docProcessManager) {
        this.documentContentTranslatorService =
                documentContentTranslatorService;
        this.localeDAO = localeDAO;
        this.documentDAO = documentDAO;
        this.docProcessManager = docProcessManager;
    }

    @Override
    public Response getStatistics(@QueryParam("url") String url,
            @QueryParam("fromLocaleCode") LocaleId fromLocaleCode,
            @QueryParam("toLocaleCode") LocaleId toLocaleCode,
            @QueryParam("dateRange") String dateRangeParam) {
        if (StringUtils.isBlank(url)) {
            APIResponse response =
                    new APIResponse(Response.Status.BAD_REQUEST, "Empty url");
            return Response.status(response.getStatus()).entity(response)
                    .build();
        }

        Optional<DateRange> dateParam =
                StringUtils.isBlank(dateRangeParam) ? Optional.empty() :
                        Optional.of(DateRange.from(dateRangeParam));

        List<Document> documents = documentDAO
                .getByUrl(url, Optional.ofNullable(fromLocaleCode),
                        Optional.ofNullable(toLocaleCode), dateParam);

        DocumentStatistics statistics = new DocumentStatistics(url);
        for (Document document: documents) {
            statistics.addRequestCount(
                    document.getSrcLocale().getLocaleId().getId(),
                    document.getTargetLocale().getLocaleId().getId(),
                    document.getCount());
        }
        return Response.ok().entity(statistics).build();
    }

    @Override
    public Response translate(DocumentContent docContent,
            @QueryParam("toLocaleCode") LocaleId toLocaleCode) {
        // Default to MS engine for translation
        BackendID backendID = BackendID.MS;

        Optional<APIResponse> errorResp =
                validateTranslateRequest(docContent, toLocaleCode);
        if (errorResp.isPresent()) {
            return Response.status(errorResp.get().getStatus())
                    .entity(errorResp.get()).build();
        }

        // if source locale == target locale, return docContent
        LocaleId fromLocaleCode = new LocaleId(docContent.getLocaleCode());
        if (fromLocaleCode.equals(toLocaleCode)) {
            LOG.info("Returning request as FROM and TO localeCode are the same:" + fromLocaleCode);
            return Response.ok().entity(docContent).build();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Request translations:" + docContent + " toLocaleCode"
                    + toLocaleCode + " backendId:" + backendID.getId());
        }

        DocumentProcessKey key =
                new DocumentProcessKey(docContent.getUrl(), fromLocaleCode,
                        toLocaleCode);
        try {
            docProcessManager.lock(key);

            Locale fromLocale = getLocale(fromLocaleCode);
            Locale toLocale = getLocale(toLocaleCode);

            Document doc = documentDAO
                    .getOrCreateByUrl(docContent.getUrl(), fromLocale, toLocale);

            DocumentContent newDocContent = documentContentTranslatorService
                    .translateDocument(doc, docContent, backendID, MAX_LENGTH);
            doc.incrementCount();
            documentDAO.persist(doc);
            return Response.ok().entity(newDocContent).build();
        } finally {
            docProcessManager.unlock(key);
        }
    }

    private Optional<APIResponse> validateTranslateRequest(DocumentContent docContent,
            LocaleId toLocaleId) {
        if (toLocaleId == null) {
            return Optional.of(new APIResponse(Response.Status.BAD_REQUEST,
                    "Invalid query param: toLocaleCode"));
        }
        if (docContent == null || docContent.getContents() == null ||
                docContent.getContents().isEmpty()) {
            return Optional.of(new APIResponse(Response.Status.BAD_REQUEST,
                    "Empty content:" + docContent));
        }
        if (StringUtils.isBlank(docContent.getLocaleCode())) {
            return Optional.of(new APIResponse(Response.Status.BAD_REQUEST,
                    "Empty localeCode"));
        }
        if (StringUtils.isBlank(docContent.getUrl()) ||
                !UrlUtil.isValidURL(docContent.getUrl())) {
            return Optional.of(new APIResponse(Response.Status.BAD_REQUEST,
                    "Invalid url:" + docContent.getUrl()));
        }
        for (TypeString string : docContent.getContents()) {
            if (StringUtils.isBlank(string.getValue()) ||
                    StringUtils.isBlank(string.getType())) {
                return Optional
                        .of(new APIResponse(Response.Status.BAD_REQUEST,
                                "Empty content: " + string.toString()));
            }
            if (!documentContentTranslatorService
                    .isMediaTypeSupported(string.getType())) {
                return Optional
                        .of(new APIResponse(Response.Status.BAD_REQUEST,
                                "Invalid mediaType: " + string.getType()));
            }
        }
        return Optional.empty();
    }

    private Locale getLocale(LocaleId localeCode) throws BadRequestException {
        if (localeCode == null) {
            return null;
        }
        Locale locale = localeDAO.getByLocaleId(localeCode);
        if (locale == null) {
            throw new BadRequestException("Not supported locale:" + localeCode);
        }
        return locale;
    }
}