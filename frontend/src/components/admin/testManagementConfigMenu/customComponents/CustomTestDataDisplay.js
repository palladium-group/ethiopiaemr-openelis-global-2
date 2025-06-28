import {
  Grid,
  Section,
  Column,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Heading,
  Row,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";

export const CustomTestDataDisplay = ({ testToDisplay }) => {
  if (!testToDisplay) return null;

  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <Section>
          <Heading>
            <FormattedMessage id={`banner.menu.patientEdit`} />
          </Heading>
        </Section>
        <hr />
        <Section>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage
                  id={`Test : ${testToDisplay.localization.english} (${testToDisplay.sampleType})`}
                />
              </Heading>
            </Section>
          </Section>
        </Section>
        <hr />
      </Column>
      <Column lg={8} md={8} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id={`Name`} />
            </Section>
          </Section>
        </Section>
        <Row>
          <Column lg={4}>
            <Section>
              <Section>
                <Section>
                  <FormattedMessage
                    id={`en : ${testToDisplay.localization.english}`}
                  />
                </Section>
              </Section>
            </Section>
          </Column>
          <Column lg={4}>
            <Section>
              <Section>
                <Section>
                  <FormattedMessage
                    id={`fr : ${testToDisplay.localization.french}`}
                  />
                </Section>
              </Section>
            </Section>
          </Column>
        </Row>
      </Column>
      <Column lg={8} md={8} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id={`Report Name`} />
            </Section>
          </Section>
        </Section>
        <Row>
          <Column lg={4}>
            <Section>
              <Section>
                <Section>
                  <FormattedMessage
                    id={`en : ${testToDisplay.reportLocalization.english}`}
                  />
                </Section>
              </Section>
            </Section>
          </Column>
          <Column lg={4}>
            <Section>
              <Section>
                <Section>
                  <FormattedMessage
                    id={`fr : ${testToDisplay.reportLocalization.french}`}
                  />
                </Section>
              </Section>
            </Section>
          </Column>
        </Row>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="Active" /> : {String(testToDisplay.active)}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="Orderable" /> :{" "}
              {String(testToDisplay.orderable)}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="Notify Patient of Results" /> :{" "}
              {String(testToDisplay.notifyResults)}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="In Lab Only" /> :{" "}
              {String(testToDisplay.inLabOnly)}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="Test unit" /> : {testToDisplay.testUnit}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="Sample types" /> :{" "}
              {testToDisplay.sampleType}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="Panel" /> : {testToDisplay.panel}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="Result type" /> : {testToDisplay.resultType}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="UOM" /> : {testToDisplay.uom}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="Significant digits" /> :{" "}
              {testToDisplay.significantDigits}
            </Section>
          </Section>
        </Section>
      </Column>
      <Column lg={4} md={4} sm={4}>
        <Section>
          <Section>
            <Section>
              <FormattedMessage id="LOINC" />: {testToDisplay.loinc ?? null}
            </Section>
          </Section>
        </Section>
      </Column>

      {testToDisplay &&
        testToDisplay?.hasDictionaryValues &&
        testToDisplay?.dictionaryValues && (
          <>
            <br />
            <hr />
            <Column lg={8} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <FormattedMessage id="Select values" /> :
                    <ul>
                      {testToDisplay.dictionaryValues.map((value, index) => (
                        <li key={index}>{value}</li>
                      ))}
                    </ul>
                  </Section>
                </Section>
              </Section>
            </Column>
            <Column lg={8} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <FormattedMessage id="Reference Value" /> :{" "}
                    {testToDisplay.referenceValue}
                  </Section>
                </Section>
              </Section>
            </Column>
          </>
        )}

      {testToDisplay &&
        testToDisplay?.hasLimitValues &&
        testToDisplay?.resultLimits?.length > 0 && (
          <Column lg={16} md={8} sm={4}>
            <>
              <br />
              <hr />
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="Result Limits" />
                    </Heading>
                  </Section>
                </Section>
                <TableContainer>
                  <Table size="sm">
                    <TableHead>
                      <TableRow>
                        <TableHeader>Sex</TableHeader>
                        <TableHeader>Age Range</TableHeader>
                        <TableHeader>Normal Range</TableHeader>
                        <TableHeader>Valid Range</TableHeader>
                        <TableHeader>Reporting Range</TableHeader>
                        <TableHeader>Critical Range</TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {testToDisplay?.resultLimits.map((limit, idx) => (
                        <TableRow key={idx}>
                          <TableCell>{limit.gender}</TableCell>
                          <TableCell>{limit.ageRange}</TableCell>
                          <TableCell>{limit.normalRange}</TableCell>
                          <TableCell>{limit.validRange}</TableCell>
                          <TableCell>{limit.reportingRange}</TableCell>
                          <TableCell>{limit.criticalRange}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Section>
            </>
          </Column>
        )}
    </Grid>
  );
};
