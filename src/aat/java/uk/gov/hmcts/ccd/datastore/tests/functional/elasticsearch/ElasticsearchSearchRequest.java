package uk.gov.hmcts.ccd.datastore.tests.functional.elasticsearch;

import com.google.common.annotations.VisibleForTesting;

class ElasticsearchSearchRequest {
    public ElasticsearchSearchRequest() {
    }

    @VisibleForTesting
    static String exactMatch(String field, Object value) {
        return "{"
            + "  \"query\": {"
            + "    \"match\": {"
            + "    \"" + field + "\" : \"" + value + "\""
            + "    }"
            + "  }"
            + "}";
    }

    @VisibleForTesting
    static String exactMatchWithSourceFilter(String field, Object value, String... sourceFilters) {
        return "{"
            + "  \"_source\": [\"" + String.join("\",\"", sourceFilters) + "\"],"
            + "  \"query\": {"
            + "    \"match\": {"
            + "    \"" + field + "\" : \"" + value + "\""
            + "    }"
            + "  }"
            + "}";
    }

    @VisibleForTesting
    static String wildcardMatch(String field, Object value) {
        return "{"
            + "  \"query\": {"
            + "    \"wildcard\": {"
            + "    \"" + field + "\" : \"" + String.valueOf(value) + "\""
            + "    }"
            + "  }"
            + "}";
    }

}
