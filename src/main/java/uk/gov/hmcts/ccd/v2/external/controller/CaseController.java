package uk.gov.hmcts.ccd.v2.external.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;

import javax.transaction.Transactional;

import static org.springframework.http.ResponseEntity.status;

@RestController
@RequestMapping(path = "/")
public class CaseController {
    private final GetCaseOperation getCaseOperation;
    private final CreateEventOperation createEventOperation;
    private final CreateCaseOperation createCaseOperation;
    private final UIDService caseReferenceService;

    @Autowired
    public CaseController(
        @Qualifier(CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation,
        @Qualifier("authorised") final CreateEventOperation createEventOperation,
        @Qualifier("authorised") final CreateCaseOperation createCaseOperation,
        UIDService caseReferenceService
    ) {
        this.getCaseOperation = getCaseOperation;
        this.createEventOperation = createEventOperation;
        this.createCaseOperation = createCaseOperation;
        this.caseReferenceService = caseReferenceService;
    }

    @GetMapping(
        path = "/cases/{caseId}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.CASE
        }
    )
    @ApiOperation(
        value = "Retrieve a case by ID",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = CaseResource.class
        ),
        @ApiResponse(
            code = 400,
            message = V2.Error.CASE_ID_INVALID
        ),
        @ApiResponse(
            code = 404,
            message = V2.Error.CASE_NOT_FOUND
        )
    })
    public ResponseEntity<CaseResource> getCase(@PathVariable("caseId") String caseId) {
        if (!caseReferenceService.validateUID(caseId)) {
            throw new BadRequestException(V2.Error.CASE_ID_INVALID);
        }

        final CaseDetails caseDetails = this.getCaseOperation.execute(caseId)
                                                             .orElseThrow(() -> new CaseNotFoundException(caseId));

        return ResponseEntity.ok(new CaseResource(caseDetails));
    }

    @Transactional
    @PostMapping(
        path = "/cases/{caseId}/events",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.CREATE_EVENT
        }
    )
    @ApiOperation(
        value = "Submit event creation",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 201,
            message = "Created",
            response = CaseResource.class
        ),
        @ApiResponse(
            code = 400,
            message = V2.Error.CASE_ID_INVALID
        ),
        @ApiResponse(
            code = 404,
            message = V2.Error.EVENT_TRIGGER_NOT_FOUND
        ),
        @ApiResponse(
            code = 409,
            message = V2.Error.CASE_ALTERED
        )
    })
    public ResponseEntity<CaseResource> createEvent(@PathVariable("caseId") String caseId,
                                                    @RequestBody final CaseDataContent content) {
        if (!caseReferenceService.validateUID(caseId)) {
            throw new BadRequestException(V2.Error.CASE_ID_INVALID);
        }

        final CaseDetails caseDetails = createEventOperation.createCaseEvent(caseId,
            content);

        return status(HttpStatus.CREATED).body(new CaseResource(caseDetails, content));
    }

    @Transactional
    @PostMapping(
        path = "/case-types/{caseTypeId}/cases",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.CREATE_CASE
        }
    )
    @ApiOperation(
        value = "Submit case creation",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = CaseResource.class
        ),
        @ApiResponse(
            code = 404,
            message = V2.Error.EVENT_TRIGGER_NOT_FOUND
        )
    })
    public ResponseEntity<CaseResource> createCase(@PathVariable("caseTypeId") String caseTypeId,
                                                   @RequestBody final CaseDataContent content,
                                                   @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning) {
        final CaseDetails caseDetails = createCaseOperation.createCaseDetails(caseTypeId, content, ignoreWarning);

        return status(HttpStatus.CREATED).body(new CaseResource(caseDetails, content, ignoreWarning));
    }
}
