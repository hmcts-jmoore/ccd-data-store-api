package uk.gov.hmcts.ccd.domain.service.getcase;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

@Service
@Qualifier("authorised")
public class AuthorisedGetCaseOperation implements GetCaseOperation {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };

    private final GetCaseOperation getCaseOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final AccessControlService accessControlService;
    private final UserRepository userRepository;
    private final CaseUserRepository caseUserRepository;


    public AuthorisedGetCaseOperation(@Qualifier("classified") final GetCaseOperation getCaseOperation,
                                      @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                      final AccessControlService accessControlService,
                                      @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                      @Qualifier(CachedCaseUserRepository.QUALIFIER)  CaseUserRepository caseUserRepository) {
        this.getCaseOperation = getCaseOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.accessControlService = accessControlService;
        this.userRepository = userRepository;
        this.caseUserRepository = caseUserRepository;
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {

        return this.execute(caseReference);
    }

    @Override
    public Optional<CaseDetails> execute(String caseReference) {
    	Optional<CaseDetails> target = getCaseDetails(caseReference);
    	target = mergeCaseDetails(target, getCaseToInclude(target), "parties");
    	target = lookupRefs(target);
    	return target;
    }

	private Optional<CaseDetails> getCaseDetails(String caseReference)
	{
        return getCaseOperation.execute(caseReference)
            .flatMap(caseDetails ->
                verifyReadAccess(getCaseType(caseDetails.getCaseTypeId()),
                    getUserRoles(caseDetails.getId()),
                    caseDetails));
	}

	private String getNodeString(JsonNode node, String key)
	{
		if (node == null)
		{
			return null;
		}
		Object value = node.get(key);
		if (value == null)
		{
			return null;
		}
		return value.toString().replace("\"", "");
	}

	private Optional<CaseDetails> lookupRefs(Optional<CaseDetails> target)
	{
		if (target.isPresent())
		{
			Map<String, JsonNode> data = target.get().getData();
			ArrayNode parties = (ArrayNode)data.get("parties");
			ArrayNode partyRefs = (ArrayNode)data.get("partyRefs");
			if (parties != null && partyRefs != null)
			{
				Map<String, String> partyNames = new HashMap<>();
				parties.forEach(p -> partyNames.put(getNodeString(p.get("value"), "id"), getNodeString(p.get("value"), "name")));
				for (JsonNode partyRef : partyRefs)
				{
					((ObjectNode)partyRef.get("value")).put("name", partyNames.get(getNodeString(partyRef.get("value"),"id")));
				}
			}
		}
		return target;
	}

	/*
	 * Get the details of the case to include, if any.
	 */
	private Optional<CaseDetails> getCaseToInclude(Optional<CaseDetails> target)
	{
		if (!target.isPresent())
		{
			return Optional.empty();
		}
		else
		{
			String caseToIncludeReference = getCaseToIncludeReference(target.get());
			if (caseToIncludeReference == null)
			{
				return Optional.empty();
			}
			return getCaseDetails(caseToIncludeReference);
		}
	}

	/*
	 * Extract the details of the case to include, if any.
	 */
	private String getCaseToIncludeReference(CaseDetails target)
	{
		System.out.println("************************************************************************");
		System.out.println("************************************************************************");
		System.out.println(target.getData());
		System.out.println("************************************************************************");
		System.out.println("************************************************************************");
		Object parentCase = target.getData().get("parentCase");
		return parentCase != null ? parentCase.toString().replace("\"", "") : null;
	}

	/*
	 * Merge the case data from the source into the target, overwriting any existing matching entries.
	 * Also merge the security classifications.
	 */
	private Optional<CaseDetails> mergeCaseDetails(Optional<CaseDetails> target, Optional<CaseDetails> source, String ... fields)
	{
		if (!source.isPresent())
		{
			return target;
		}
		if (!target.isPresent())
		{
			return source;
		}
		for (String field : fields)
		{
			target.get().getData().put(field, source.get().getData().get(field));
			target.get().getDataClassification().put(field, source.get().getDataClassification().get(field));
		}
		return target;
	}
	

    private CaseType getCaseType(String caseTypeId) {
        return caseDefinitionRepository.getCaseType(caseTypeId);
    }


    private Set<String> getUserRoles(String caseId) {
        return Sets.union(userRepository.getUserRoles(),
            caseUserRepository
                .findCaseRoles(Long.valueOf(caseId), userRepository.getUserId())
                .stream()
                .collect(Collectors.toSet()));
    }

    private Optional<CaseDetails> verifyReadAccess(CaseType caseType, Set<String> userRoles, CaseDetails caseDetails) {

        if (caseType == null || caseDetails == null || CollectionUtils.isEmpty(userRoles)) {
            return Optional.empty();
        }

        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType, userRoles, CAN_READ) ||
            !accessControlService.canAccessCaseStateWithCriteria(caseDetails.getState(), caseType, userRoles, CAN_READ)) {
            return Optional.empty();
        }

        caseDetails.setData(MAPPER.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                MAPPER.convertValue(caseDetails.getData(), JsonNode.class),
                caseType.getCaseFields(),
                userRoles,
                CAN_READ,
                false),
            STRING_JSON_MAP));
        caseDetails.setDataClassification(MAPPER.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                MAPPER.convertValue(caseDetails.getDataClassification(), JsonNode.class),
                caseType.getCaseFields(),
                userRoles,
                CAN_READ,
                true),
            STRING_JSON_MAP));

        return Optional.of(caseDetails);
    }

}
