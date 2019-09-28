# build

gradle clean build -x test

# deploy 

1. copy pol-pdf-gen.zip to s3 bucket
2. upload zip file and configure handle "kubesure.io.pol.pdf.gen.PolicyPDFGeneratorHandler::handleRequest" 
3. save
4. Configure trigger 
