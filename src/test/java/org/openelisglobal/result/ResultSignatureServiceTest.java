package org.openelisglobal.result;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.service.ResultSignatureService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.springframework.beans.factory.annotation.Autowired;

public class ResultSignatureServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultSignatureService resultSignatureService;
    @Autowired
    private ResultService resultService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result.xml");
    }

    @Test
    public void getAll_shouldReturnAllResultSignatures() {
        List<ResultSignature> resultSignatures = resultSignatureService.getAll();
        assertEquals(2, resultSignatures.size());
        assertEquals("2", resultSignatures.get(0).getId());
        assertEquals("3", resultSignatures.get(1).getId());

    }

    @Test
    public void getData_shouldReturnSignatureData() {
        ResultSignature resultSignature = resultSignatureService.get("3");
        resultSignatureService.getData(resultSignature);
        assertEquals("External Doctor", resultSignature.getNonUserName());
    }

    @Test
    public void getResultSignatureByResult_shouldReturnResultSignature() {
        Result result = resultService.get("3");
        List<ResultSignature> resultSignatures = resultSignatureService.getResultSignaturesByResult(result);
        assertEquals(1, resultSignatures.size());
        assertEquals("2", resultSignatures.get(0).getId());
    }

    @Test
    public void getResultSignatureById_shouldReturnResultSignature() {
        ResultSignature resultSignature1 = new ResultSignature();
        resultSignature1.setId("3");
        ResultSignature resultSignature = resultSignatureService.getResultSignatureById(resultSignature1);
        assertEquals("External Doctor", resultSignature.getNonUserName());
    }

    @Test
    public void getResultSignaturesByResults_shouldReturnResultSignatures() {
        List<Result> results = resultService.getAll();
        List<ResultSignature> resultSignatures = resultSignatureService.getResultSignaturesByResults(results);
        assertEquals(2, resultSignatures.size());

    }

}
