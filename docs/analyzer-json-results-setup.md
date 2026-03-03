# Analyzer JSON Results: Setup and Testing

This guide explains how to set up **one sample analyzer** that receives **native JSON** result data from a device interface, and how to verify that OpenELIS received the data.

---

## 1. API Overview

Device interfaces send results to OpenELIS via a single REST endpoint.

| Item | Value |
|------|--------|
| **Method** | `POST` |
| **URL** | `https://<host>/api/OpenELIS-Global/rest/analyzer/results` |
| **Content-Type** | `application/json` |
| **Auth** | Session (logged-in user). If no session, OpenELIS uses system user `1` for audit. |

### Request body (JSON)

```json
{
  "analyzerId": "<OpenELIS analyzer ID>",
  "results": [
    {
      "accessionNumber": "2025-00001",
      "testCode": "WBC",
      "result": "7.2",
      "units": "10^3/uL",
      "completeDate": "2025-03-03T14:30:00",
      "control": false
    }
  ]
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `analyzerId` | Yes | OpenELIS analyzer ID (from the analyzer table). |
| `results` | Yes | Array of result rows (at least one). |
| `results[].accessionNumber` | Yes | Sample accession number; must already exist in OpenELIS. |
| `results[].testCode` | Yes | Analyzer test code; must be mapped to an OpenELIS test for this analyzer. |
| `results[].result` | Yes | Result value (string). |
| `results[].units` | No | Units (e.g. `"10^3/uL"`). |
| `results[].completeDate` | No | Completion date/time. ISO-8601 (`yyyy-MM-dd'T'HH:mm:ss`) or system default format. Omit for “now”. |
| `results[].control` | No | `true` for control samples; default `false`. |

### Response

- **200 OK** – Success. Body example: `{ "imported": 1 }` or `{ "imported": 2, "warnings": ["No mapping for testCode: XYZ"] }`.
- **400 Bad Request** – Validation error or no valid results (e.g. all test codes unmapped). Body: `{ "error": "..." }`.
- **404 Not Found** – Analyzer not found. Body: `{ "error": "Analyzer not found: <id>" }`.
- **500 Internal Server Error** – Save failed. Body: `{ "error": "..." }`.

---

## 2. Setup: One Sample Analyzer

Do these steps once per device (or device type) that will send JSON.

### Step 2.1 – Create an analyzer record

You need one **analyzer** in OpenELIS so that results can be tied to it and test mappings can be configured.

**Option A – REST API (if available)**

```bash
curl -s -X POST "https://<host>/api/OpenELIS-Global/rest/analyzer/analyzers" \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=<your-session-cookie>" \
  -d '{
    "name": "JSON Sample Analyzer",
    "description": "Sample device for JSON result ingestion",
    "analyzerTypeName": "GenericHL7",
    "active": true
  }'
```

Note the returned `id` (e.g. `"5"`). That is your `analyzerId` for the JSON payload.

**Option B – Database**

Insert into the `analyzer` table (and optionally link to `analyzer_type` if your deployment uses it). Use your normal ID sequence. Example (adjust IDs and names to match your site):

```sql
-- Example only; use your sequence and naming.
INSERT INTO analyzer (id, name, description, analyzer_type, is_active, ...)
VALUES (nextval('analyzer_seq'), 'JSON Sample Analyzer', 'Sample device for JSON', 'CHEMISTRY', 'Y', ...);
```

Record the analyzer `id` (e.g. `5`) for the next steps.

### Step 2.2 – Ensure a sample and test exist

- **Sample:** The JSON `accessionNumber` must match an existing sample (e.g. create a sample with accession `2025-00001` in the UI or via your normal process).
- **Test:** There must be an OpenELIS **test** that will receive the result (e.g. “White Blood Count” with id `"123"`). Note the test `id` for mapping.

### Step 2.3 – Create analyzer test mapping(s)

Map each **analyzer test code** (e.g. `WBC`) to the OpenELIS **test id** for that analyzer.

**Option A – UI**

1. Log in to OpenELIS.
2. Go to **Results → [Your analyzer name]** or **Admin → Analyzer Test Name** (path may vary by version).
3. Add a mapping: analyzer test name/code = `WBC`, OpenELIS test = the test you chose (e.g. “White Blood Count”).

**Option B – REST**

```bash
curl -s -X POST "https://<host>/api/OpenELIS-Global/rest/AnalyzerTestName" \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=<session>" \
  -d '{
    "analyzerId": "5",
    "analyzerTestName": "WBC",
    "testId": "123"
  }'
```

**Option C – Database**

```sql
INSERT INTO analyzer_test_name_mapping (analyzer_id, analyzer_test_name, test_id, ...)
VALUES (5, 'WBC', '123', ...);
```

After adding mappings, the cache used by the JSON endpoint is refreshed (on next request or reload). So one mapping for `WBC` → test id `123` is enough to accept a result row with `"testCode": "WBC"`.

---

## 3. Test That Data Was Received

### 3.1 – Send a test JSON payload

Replace `<host>`, `analyzerId`, and (if needed) `accessionNumber` and `testCode` with your values. Ensure the sample exists and the test code is mapped.

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST "https://<host>/api/OpenELIS-Global/rest/analyzer/results" \
  -H "Content-Type: application/json" \
  -d '{
    "analyzerId": "5",
    "results": [
      {
        "accessionNumber": "2025-00001",
        "testCode": "WBC",
        "result": "7.2",
        "units": "10^3/uL",
        "control": false
      }
    ]
  }'
```

Expected: **HTTP 200** and body like `{ "imported": 1 }`.

### 3.2 – Verify in the React UI (modern frontend)

Analyzer results are shown on the **Analyzer Results** page, filtered by analyzer. The page is the same for all analyzers; the `type` query parameter selects which analyzer’s results to load.

**Flow:**

1. **Results** in the top menu → **From Analyzer** shows submenu items only for analyzers that have a **plugin** (e.g. Sysmex, Mindray). Each plugin registers a link like `/AnalyzerResults?type=SysmexXTAnalyzer`.
2. **JSON-ingestion-only analyzers** (no plugin) do **not** get a menu item. To view their results you must open the Analyzer Results page with the analyzer **name** as `type`.

**How to open the page for your analyzer:**

- **If your analyzer has a plugin:** use **Results → From Analyzer → [analyzer label]**.
- **If your analyzer has no plugin** (e.g. “JSON Sample Analyzer”): open the Analyzer Results page with `type` equal to the analyzer’s **exact name** (as in the `analyzer` table):

  **URL:**  
  `https://<host>/AnalyzerResults?type=<analyzer name>`

  Use the exact name; spaces must be encoded (e.g. `%20`). Examples:

  - Analyzer name **JSON Sample Analyzer**  
    → `https://<host>/AnalyzerResults?type=JSON%20Sample%20Analyzer`
  - Replace `<host>` with your base URL (e.g. `https://localhost` or your deployment host).

3. After the page loads, you should see the received result(s) (e.g. accession `2025-00001`) with the value you sent (e.g. `7.2`), ready for validation/release.

### 3.3 – Optional: Check the database

Query the analyzer results table (exact table/column names may vary by schema):

```sql
SELECT id, analyzer_id, accession_number, test_name, result, units, complete_date
FROM analyzer_results
WHERE analyzer_id = '5'
ORDER BY complete_date DESC
LIMIT 10;
```

You should see the row(s) you sent.

---

## 4. Checklist Summary

| Step | Action |
|------|--------|
| 1 | Create one **analyzer** (REST or DB); note `analyzerId`. |
| 2 | Ensure a **sample** exists with the accession number you will send. |
| 3 | Ensure an **OpenELIS test** exists; note its `testId`. |
| 4 | Create **analyzer test mapping**: analyzer test code (e.g. `WBC`) → `testId`. |
| 5 | **POST** JSON to `https://<host>/api/OpenELIS-Global/rest/analyzer/results`. |
| 6 | Confirm **200** and `"imported": 1` (or more). |
| 7 | Open **Results → [Analyzer]** in the UI and confirm the result appears. |

---

## 5. Troubleshooting

| Symptom | What to check |
|--------|----------------|
| **404 Analyzer not found** | `analyzerId` must be the exact OpenELIS analyzer id (string). Check analyzer list or DB. |
| **400 No valid results / No mapping for testCode** | Add a mapping for that `testCode` for this analyzer (Step 2.3). Reload cache if needed. |
| **200 but `imported: 0` and warnings** | Same as above: map the test codes used in `results[].testCode`. |
| Result not visible in UI | For analyzers without a plugin, open the page by URL: `/AnalyzerResults?type=<analyzer name>` (e.g. `?type=JSON%20Sample%20Analyzer`). The **Results → From Analyzer** submenu only lists plugin-based analyzers. |
| Sample not found when releasing | The `accessionNumber` in JSON must match an existing sample; create the sample first or use an existing accession. |
| **App fails to start: StaleStateException / JpaOptimisticLockingFailureException on `analyzer` update** | One or more `analyzer` rows have `last_updated` NULL (e.g. inserted manually). Run: `UPDATE analyzer SET last_updated = COALESCE(last_updated, NOW()) WHERE last_updated IS NULL;` then restart. |

---

## 6. Security and production

- The endpoint uses the current session user for audit when present; otherwise it uses system user `1`.
- In production, use HTTPS and restrict access (e.g. network, firewall, or future API key) as per your security policy.
- For high volume, consider batching multiple result rows in one `results` array per request.
