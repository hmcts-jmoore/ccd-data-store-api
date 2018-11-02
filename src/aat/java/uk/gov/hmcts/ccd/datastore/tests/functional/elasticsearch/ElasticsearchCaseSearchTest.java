package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase.DATE_TIME;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseBuilder.FullCase.TEXT;
import static uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.State;

import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.TestData;
import uk.gov.hmcts.ccd.datastore.tests.helper.elastic.ElasticsearchTestDataLoaderExtension;

@ExtendWith(ElasticsearchTestDataLoaderExtension.class)
public class ElasticsearchCaseSearchTest extends ElasticsearchBaseTest {

    public static final String SEARCH_UPDATED_CASE_TEST_REFERENCE = TestData.uniqueReference();
    public static final String EXACT_MATCH_TEST_REFERENCE = TestData.uniqueReference();

    ElasticsearchCaseSearchTest(AATHelper aat) {
        super(aat);
    }

    @BeforeAll
    static void setUp() {
        assertElasticsearchEnabled();
    }

    @Nested
    @DisplayName("Search updated case")
    class SearchUpdatedCase {

        @Test
        @DisplayName("should return updated case on search")
        void shouldReturnUpdatedCaseOnSearch() {
            Long caseReference = (Long) testData.get(SEARCH_UPDATED_CASE_TEST_REFERENCE);
            String jsonSearchRequest = ElasticsearchSearchRequest.exactMatch(ES_FIELD_CASE_REFERENCE, caseReference);

            ValidatableResponse response = searchCase(asPrivateCaseworker(false), jsonSearchRequest);

            assertSingleCaseReturned(response);
            assertField(response, ES_FIELD_STATE, State.IN_PROGRESS);
            assertField(response, CASE_ID, caseReference);
        }
    }

    @Nested
    @DisplayName("Exact match")
    class ExactMatch {

        @Nested
        @DisplayName("text field")
        class TextField {

            @Test
            @DisplayName("should return case for exact match on a text field")
            void shouldReturnCaseForExactMatchOnTextField() {
                searchCaseForExactMatchAndVerifyResponse("TextField", TEXT);
            }
        }

        @Nested
        @DisplayName("date time field")
        class DateTimeField {

            @Test
            @DisplayName("should return case for exact match on a date time field")
            void shouldReturnCaseForExactMatchOnDateTimeField() {
                searchCaseForExactMatchAndVerifyResponse("DateTimeField", DATE_TIME);
            }
        }

        private void searchCaseForExactMatchAndVerifyResponse(String field, String value) {
            String jsonSearchRequest = ElasticsearchSearchRequest.exactMatch(CASE_DATA_FIELD_PREFIX + field, value);

            ValidatableResponse response = searchCase(asPrivateCaseworker(false), jsonSearchRequest);

            assertSingleCaseReturned(response);
            assertField(response, RESPONSE_CASE_DATA_FIELDS_PREFIX + field, value);
            assertField(response, CASE_ID, testData.get(EXACT_MATCH_TEST_REFERENCE));
        }

    }

    @Nested
    @DisplayName("Wildcard")
    class Wildcard {

        @Nested
        @DisplayName("text field")
        class TextField {

            @Test
            @DisplayName("should return case matching wildcard expression on a text field")
            void shouldReturnCaseForWildcardMatchOnTextField() {
                String wildcardExpr = TEXT.substring(0, 3).toLowerCase() + "*";
                searchCaseByWildcardAndVerifyResponse("TextField", wildcardExpr, TEXT);
            }
        }

        private void searchCaseByWildcardAndVerifyResponse(String field, String wildcardExpr, String expectedValue) {
            String jsonSearchRequest = ElasticsearchSearchRequest.wildcardMatch(CASE_DATA_FIELD_PREFIX + field, wildcardExpr);

            ValidatableResponse response = searchCase(asPrivateCaseworker(false), jsonSearchRequest);

            assertSingleCaseReturned(response);
            assertField(response, RESPONSE_CASE_DATA_FIELDS_PREFIX + field, expectedValue);
            assertField(response, CASE_ID, testData.get(EXACT_MATCH_TEST_REFERENCE));
        }

    }

}
