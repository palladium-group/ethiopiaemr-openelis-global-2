import * as Yup from "yup";

/**
 * @param {boolean} [isNationalIdRequired=false] - When true, nationalId is required (from PATIENT_NATIONAL_ID_REQUIRED config).
 */
const CreatePatientValidationSchema = (isNationalIdRequired = false) =>
  Yup.object().shape({
    nationalId: isNationalIdRequired
      ? Yup.string().required("National ID Required")
      : Yup.string(),
    birthDateForDisplay: Yup.string()
      .required("Patient Birth date Required")
      .test("valid-date", "Invalid date format", function (value) {
        const dateFormat = /^\d{2}\/\d{2}\/\d{4}$/;
        if (!value || !value.match(dateFormat)) {
          return false;
        }
        const [day, month, year] = value.split("/");
        const date = new Date(`${year}-${month}-${day}`);
        const date2 = new Date(`${year}-${day}-${month}`);

        const validDate1 = date instanceof Date && !isNaN(date);
        const validDate2 = date2 instanceof Date && !isNaN(date2);

        return validDate1 || validDate2;
      }),
    patientContact: Yup.object().shape({
      person: Yup.object().shape({
        email: Yup.string().email("Contact Email Must Be Valid"),
      }),
    }),
    gender: Yup.string().required("Gender is Required"),
  });

export default CreatePatientValidationSchema;
