/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package pol.pdf.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import kubesure.io.pol.pdf.gen.PolicyPDFGeneratorHandler;

public class PolicyPDFGeneratorTest {
    @Test public void testCreatePDF() throws Exception{
        LambdaLogger logger = new LambdaLogger(){
            @Override
            public void log(byte[] message) {
                System.out.println(message.toString());
            }
        
            @Override
            public void log(String message) {
                System.out.println(message);
            }
        };
       
        URL url = getClass().getResource("1234567890.html");
        File file = new File(url.getPath());
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder sbuilder = new StringBuilder();
        String line; 
        while((line = reader.readLine()) != null) {
            sbuilder.append(line);
        } 
        reader.close();
        PolicyPDFGeneratorHandler pg = new PolicyPDFGeneratorHandler();
        byte [] pdfbytes = pg.generatePDF(sbuilder.toString(),logger);
        File pdf = new File("1234567890.pdf");
        FileUtils.writeByteArrayToFile(pdf, pdfbytes);
        assertTrue("pdf file not generated", pdf.exists());
    }

    @Test
    public void testFileSubString() {
        String key = "unprocessed/1234567890.html";
        String polnumber = key.substring(key.indexOf("/") + 1, key.indexOf("."));
        assertEquals("1234567890", polnumber);
    }
}
