package kubesure.io.pol.pdf.gen;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import org.xhtmlrenderer.pdf.ITextRenderer;

public class PolicyPDFGeneratorHandler implements RequestHandler<S3Event, String> {

    @Override
    public String handleRequest(S3Event event, Context context) {
        LambdaLogger logger = context.getLogger();
        String result = null;
        for (S3EventNotificationRecord record : event.getRecords()) {
            try {
                result = processEvent(record, logger);
            } catch (Exception e) {
                logger.log(e.getMessage());
                e.printStackTrace();
                return "error";
            }
        }
        return result;
    }

    private String processEvent(S3EventNotificationRecord record, LambdaLogger logger) throws Exception {
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
        String bucketName = record.getS3().getBucket().getName();
        String objectName = record.getS3().getObject().getKey();
        logger.log(bucketName + "\n");
        logger.log(objectName + "\n");
        S3Object obj = s3.getObject(new GetObjectRequest(bucketName, objectName));
        S3ObjectInputStream s3stream = obj.getObjectContent();
        logger.log(obj.toString() + "\n");
        logger.log(s3stream.toString() + "\n");
        BufferedReader reader = new BufferedReader(new InputStreamReader(s3stream));
        StringBuilder sbuilder = new StringBuilder();
        String htmlString = null;
        while ((htmlString = reader.readLine()) != null) {
            sbuilder.append(htmlString);
        }
        s3stream.close();
        byte [] pdf = generatePDF(sbuilder.toString(), logger);

        InputStream in = new ByteArrayInputStream(pdf);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentType("application/pdf");
        meta.setContentLength(pdf.length);
        String polnumber = objectName.substring(29, objectName.indexOf("."));
        PutObjectResult result = s3.putObject(bucketName, "unprocessed/"+ polnumber+".pdf", in, meta);
        in.close();
        return result.getVersionId();
    }

    public byte [] generatePDF(String htmlString, LambdaLogger logger) throws Exception {
        logger.log("htmlString" + "\n");
        logger.log(htmlString);
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(htmlString);
        renderer.layout();
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream(htmlString.length())) {
            renderer.createPDF(bos);
            //logger.log(bos.toByteArray());
            return bos.toByteArray();
        }
    }
}
