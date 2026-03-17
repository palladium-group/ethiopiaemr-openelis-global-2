# CBC Auto-Cancellation Feature Documentation

## Objective

The primary goal of the CBC (Complete Blood Count) Auto-Cancellation feature is
to ensure that samples achieve a **"Finished"** status in OpenELIS Global,
enabling seamless result synchronization back to **OpenMRS**.

## Business Rationale

In clinical workflows, a sample is only synced back to OpenMRS when the overall
sample status reaches `Finished`. For CBC panels, if only a subset of tests are
performed (e.g., through an analyzer), the remaining unperformed tests stay in a
`Not Started` or `Pending` state. This prevents the sample from being marked as
finished, effectively blocking the synchronization of the results that _were_
obtained.

By automatically canceling these unperformed/unvalidated CBC tests once a
matching analyzer has been validated, we:

1.  Complete the sample lifecycle automatically.
2.  Enable immediate data exchange with external systems (OpenMRS).
3.  Reduce manual intervention for lab technicians.

## Technical Implementation

### 1. Analyzer Matching Logic (Subset Matching)

The system identifies a matching analyzer by comparing the set of tests just
validated (`validatedTestIds`) with the sets of tests supported by each active
analyzer.

- **Criteria**: An analyzer is a match if **all** of its supported tests are
  contained within the `validatedTestIds`.
- **Logic**: `validatedTestIds.containsAll(analyzerSupportedTests)`

### 2. Automatic Cancellation

Once a match is found:

- The system scans all remaining analyses for that accession number.
- Any analysis belonging to the **CBC Panel** that was **not** part of the
  current validation batch is automatically moved to the `Canceled` status.
- This ensures that only the validated (and performed) tests remain "Finalized",
  while the rest are cleared.

### 3. Sample Status Transition

After cancellation, the system checks if all tests for the sample are now in a
terminal status (`Finalized`, `Canceled`, or `Non-Conforming`). If so, the
sample status is updated to `Finished`, triggering the sync to OpenMRS.

## Configuration

The feature is designed to be flexible and avoids hardcoded strings where
possible.

- **Panel Name**: The name of the panel targeted for auto-cancellation is
  defined in `application.properties`.
  - **Property**: `org.openelisglobal.cbc.panelname`
  - **Default**: `Complete Blood Count`
- **Injection**: The `ResultValidationServiceImpl` uses Spring `@Value`
  injection to load this name at runtime.

## Verification

- **Unit Tests**: `ResultValidationServiceImplTest` contains 7 test cases
  covering successful cancellation, subset matching, terminal status respect,
  and sample status transitions.
- **Logging**: The system logs information about identified analyzers and
  canceled tests for auditing purposes.

---

_For questions regarding this implementation, refer to
`ResultValidationServiceImpl.java` and its associated unit tests._
