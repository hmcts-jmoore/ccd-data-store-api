package uk.gov.hmcts.ccd.data.caseaccess;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.reform.amlib.AccessManagementService;
import uk.gov.hmcts.reform.amlib.DefaultRoleSetupImportService;
import uk.gov.hmcts.reform.amlib.models.ResourceDefinition;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static uk.gov.hmcts.reform.amlib.enums.AccessType.ROLE_BASED;
import static uk.gov.hmcts.reform.amlib.enums.RoleType.IDAM;
import static uk.gov.hmcts.reform.amlib.enums.SecurityClassification.PUBLIC;

@Transactional
public class AMCaseUserRepositoryComponentTest extends BaseTest {

    private static final String COUNT_CASE_USERS = "TBD - see CCDCaseUserRepositoryTest";

    private static final String JURISDICTION_ID = "JURISDICTION";
    private static final String CASE_TYPE_ID = "CASE_TYPE";
    private static final String CASE_REFERENCE = "1234123412341234";
    private static final Long CASE_ID = 1L;
    private static final Long CASE_ID_GRANTED = 2L;
    private static final Long CASE_ID_3 = 3L;
    private static final String USER_ID = "89000";
    private static final String USER_ID_GRANTED = "89001";
    private static final String CASE_ROLE = "[DEFENDANT]";
    private static final String CASE_ROLE_SOLICITOR = "[SOLICITOR]";
    private static final String CASE_ROLE_CREATOR = "[CREATOR]";

    @PersistenceContext
    private EntityManager em;

    private JdbcTemplate template;

    @Autowired
    private AMCaseUserRepository repository;

    @Autowired
    DefaultRoleSetupImportService defaultRoleSetupImportService;

    @Before
    public void setUp() {
        template = new JdbcTemplate(db);

        defaultRoleSetupImportService.addService(JURISDICTION_ID);
        defaultRoleSetupImportService.addRole(CASE_ROLE, IDAM, PUBLIC, ROLE_BASED);
        defaultRoleSetupImportService.addRole(CASE_ROLE_SOLICITOR, IDAM, PUBLIC, ROLE_BASED);
        defaultRoleSetupImportService.addRole(CASE_ROLE_CREATOR, IDAM, PUBLIC, ROLE_BASED);

        ResourceDefinition resourceDefinition =
            //TODO: What should be the resourceType and resourceName.
            //To be clarified with Mutlu/Shashank again.//resource name: CMC, FPL
            new ResourceDefinition(JURISDICTION_ID, "case", CASE_REFERENCE);
        defaultRoleSetupImportService.addResourceDefinition(resourceDefinition);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldGrantAccessAsCustomCaseRole() {
        repository.grantAccess(JURISDICTION_ID, CASE_REFERENCE, CASE_ID, USER_ID, CASE_ROLE);

        assertThat(countAccesses(CASE_ID, USER_ID, CASE_ROLE), equalTo(1));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_am.sql",
        "classpath:sql/insert_case_users_am.sql",
    })
    public void shouldRevokeAccessAsCustomCaseRole() {
        repository.revokeAccess(JURISDICTION_ID, CASE_TYPE_ID, CASE_ID_GRANTED, USER_ID_GRANTED, CASE_ROLE);

        assertThat(countAccesses(CASE_ID_GRANTED, USER_ID_GRANTED, CASE_ROLE), equalTo(0));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_am.sql",
        "classpath:sql/insert_case_users_am.sql",
    })
    public void shouldFindCasesUserIdHasAccessTo() {
        List<Long> caseIds = repository.findCasesUserIdHasAccessTo(USER_ID);

        assertThat(caseIds.size(), equalTo(1));
        assertThat(caseIds.get(0), equalTo(CASE_ID));

        caseIds = repository.findCasesUserIdHasAccessTo(USER_ID_GRANTED);

        assertThat(caseIds.size(), equalTo(3));
        assertThat(caseIds, containsInAnyOrder(CASE_ID_GRANTED, CASE_ID_GRANTED, CASE_ID_3));
    }

    private Integer countAccesses(Long caseId, String userId) {
        return countAccesses(caseId, userId, GlobalCaseRole.CREATOR.getRole());
    }

    private Integer countAccesses(Long caseId, String userId, String role) {
        em.flush();

        final Object[] parameters = new Object[]{
            caseId,
            userId,
            role
        };

        return template.queryForObject(COUNT_CASE_USERS, parameters, Integer.class);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_am.sql",
        "classpath:sql/insert_case_users_am.sql",
    })
    public void shouldFindCaseRolesUserPerformsForCase() {

        List<String> caseRoles = repository.findCaseRoles(CASE_TYPE_ID, CASE_ID, USER_ID);

        assertThat(caseRoles.size(), equalTo(1));
        assertThat(caseRoles.get(0), equalTo(CASE_ROLE_CREATOR));

        caseRoles = repository.findCaseRoles(CASE_TYPE_ID, CASE_ID_GRANTED, USER_ID_GRANTED);

        assertThat(caseRoles.size(), equalTo(2));
        assertThat(caseRoles, containsInAnyOrder(CASE_ROLE, CASE_ROLE_SOLICITOR));
    }

}
